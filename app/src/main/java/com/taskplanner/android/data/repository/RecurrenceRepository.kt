package com.taskplanner.android.data.repository

import com.taskplanner.android.core.model.RecurrenceFrequency
import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.core.model.TaskOriginType
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.dao.RecurrenceRuleDao
import com.taskplanner.android.data.local.dao.TaskDao
import com.taskplanner.android.data.local.entities.RecurrenceRuleEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

class RecurrenceRepository(
    private val ruleDao: RecurrenceRuleDao,
    private val taskDao: TaskDao,
    private val syncTrigger: SyncTrigger
) {
    companion object {
        private const val LOOKAHEAD_DAYS: Long = 30L
        private const val DAY_MILLIS: Long = 24L * 60L * 60L * 1000L
    }

    fun observeRules(userId: String): Flow<List<RecurrenceRuleEntity>> = ruleDao.observeAll(userId)

    suspend fun getSourceTasksByIds(userId: String, ids: List<String>): List<TaskEntity> {
        if (ids.isEmpty()) return emptyList()
        
        return taskDao.getByIdsAny(userId, ids)
    }

    suspend fun addRecurrence(
        userId: String,
        sourceTaskId: String,
        frequency: RecurrenceFrequency,
        interval: Int,
        weekdays: List<Int>?,
        endDate: LocalDate?
    ) {
        val source = taskDao.getById(userId, sourceTaskId) ?: return
        val startDate = TimeUtils.localDateFromMillis(source.date)
        val now = System.currentTimeMillis()

        val weekdaysMask = weekdays?.fold(0) { acc, day -> acc or (1 shl day) } ?: 0
        val rule = RecurrenceRuleEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            frequency = frequency.raw,
            intervalValue = maxOf(1, interval),
            weekdaysMask = weekdaysMask,
            dayOfMonth = when (frequency) {
                RecurrenceFrequency.MONTHLY -> startDate.dayOfMonth
                RecurrenceFrequency.YEARLY -> startDate.dayOfMonth
                else -> 0
            },
            monthOfYear = if (frequency == RecurrenceFrequency.YEARLY) startDate.monthValue else 0,
            sourceTaskId = sourceTaskId,
            startDate = TimeUtils.startOfDayMillis(startDate),
            endDate = endDate?.let { TimeUtils.startOfDayMillis(it) },
            lastGeneratedAt = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        ruleDao.upsert(rule)

        generateTasksForRule(userId, rule)
        syncTrigger.trigger()
    }

    suspend fun createRecurrenceSeries(
        userId: String,
        title: String,
        description: String?,
        startDate: LocalDate,
        startTimeMillis: Long?,
        priority: Int,
        categoryId: String?,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int,
        frequency: RecurrenceFrequency,
        interval: Int,
        weekdays: List<Int>?,
        endDate: LocalDate?
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return
        if (frequency == RecurrenceFrequency.WEEKLY && (weekdays == null || weekdays.isEmpty())) return

        val now = System.currentTimeMillis()
        val dayMillis = TimeUtils.startOfDayMillis(startDate)
        val sourceTaskId = UUID.randomUUID().toString()

        val sourceTask = TaskEntity(
            id = sourceTaskId,
            userId = userId,
            title = normalizedTitle,
            description = description?.takeIf { it.isNotBlank() },
            notes = null,
            imageData = null,
            date = dayMillis,
            startTime = startTimeMillis,
            priority = priority,
            status = TaskStatus.PLANNED.raw,
            completedAt = null,
            hasReminder = hasReminder && startTimeMillis != null,
            reminderOffsetMinutes = reminderOffsetMinutes,
            categoryId = categoryId,
            parentTaskId = null,
            goalStepId = null,
            originType = TaskOriginType.MANUAL.raw,
            instanceDate = null,
            recurrenceRuleId = null,
            templateItemId = null,
            templateApplicationId = null,
            position = 0,
            searchText = (normalizedTitle + " " + (description ?: "")).lowercase(),
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        taskDao.upsert(sourceTask)

        addRecurrence(
            userId = userId,
            sourceTaskId = sourceTaskId,
            frequency = frequency,
            interval = interval,
            weekdays = weekdays,
            endDate = endDate
        )
    }

    suspend fun updateRecurrenceSeries(
        userId: String,
        ruleId: String,
        title: String,
        description: String?,
        startTimeMillis: Long?,
        priority: Int,
        categoryId: String?,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int,
        frequency: RecurrenceFrequency,
        interval: Int,
        weekdays: List<Int>?,
        endDate: LocalDate?
    ) {
        val rule = ruleDao.getById(userId, ruleId) ?: return
        val sourceTask = taskDao.getByIdAny(userId, rule.sourceTaskId) ?: return
        val now = System.currentTimeMillis()

        val weekdaysMask = weekdays?.fold(0) { acc, day -> acc or (1 shl day) } ?: rule.weekdaysMask.toInt()

        
        taskDao.upsert(sourceTask.copy(
            title = title.trim(),
            description = description?.takeIf { it.isNotBlank() },
            startTime = startTimeMillis,
            priority = priority,
            hasReminder = hasReminder && startTimeMillis != null,
            reminderOffsetMinutes = reminderOffsetMinutes,
            categoryId = categoryId,
            updatedAt = now,
            syncStatus = SyncStatus.UPDATED_LOCAL.raw
        ))

        
        val tomorrow = startOfDayMillis(java.time.LocalDate.now().plusDays(1))
        val futureTasks = taskDao.getForRangeAny(userId, tomorrow, Long.MAX_VALUE)
            .filter { it.recurrenceRuleId == ruleId && it.originType == TaskOriginType.RECURRENCE.raw && it.deletedAt == null }
        futureTasks.forEach { t ->
            val newStartTime = startTimeMillis?.let { stored ->
                val localTime = com.taskplanner.android.core.util.TimeUtils.localTimeFromMillis(stored)
                com.taskplanner.android.core.util.TimeUtils.millisFromLocalTime(localTime, com.taskplanner.android.core.util.TimeUtils.localDateFromMillis(t.date))
            }
            taskDao.upsert(t.copy(
                title = title.trim(),
                description = description?.takeIf { it.isNotBlank() },
                startTime = newStartTime,
                priority = priority,
                hasReminder = hasReminder && newStartTime != null,
                reminderOffsetMinutes = reminderOffsetMinutes,
                categoryId = categoryId,
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            ))
        }

        
        ruleDao.update(rule.copy(
            frequency = frequency.raw,
            intervalValue = maxOf(1, interval),
            weekdaysMask = weekdaysMask,
            endDate = endDate?.let { startOfDayMillis(it) },
            updatedAt = now,
            syncStatus = SyncStatus.UPDATED_LOCAL.raw
        ))

        syncTrigger.trigger()
    }

    private fun startOfDayMillis(date: java.time.LocalDate) = TimeUtils.startOfDayMillis(date)

    suspend fun stopRecurrence(userId: String, ruleId: String) {
        val rule = ruleDao.getById(userId, ruleId) ?: return
        val now = System.currentTimeMillis()
        val today = TimeUtils.startOfDayMillis(LocalDate.now())
        val cutoff = today + DAY_MILLIS

        taskDao.hardDeleteGeneratedForRuleFromDate(userId, ruleId, TaskOriginType.RECURRENCE.raw, cutoff)

        ruleDao.update(
            rule.copy(
                endDate = today,
                lastGeneratedAt = today,
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun resumeRecurrence(userId: String, ruleId: String) {
        val rule = ruleDao.getById(userId, ruleId) ?: return
        val now = System.currentTimeMillis()

        val today = TimeUtils.startOfDayMillis(LocalDate.now())
        val generationStart = maxOf(today, rule.startDate)
        val lastGenerated = generationStart - DAY_MILLIS

        val updated = rule.copy(
            endDate = null,
            lastGeneratedAt = lastGenerated,
            updatedAt = now,
            syncStatus = SyncStatus.UPDATED_LOCAL.raw
        )
        ruleDao.update(updated)
        generateTasksForRule(userId, updated)
        syncTrigger.trigger()
    }

    suspend fun deleteRecurrenceKeepingPast(userId: String, ruleId: String) {
        val rule = ruleDao.getById(userId, ruleId) ?: return
        val now = System.currentTimeMillis()
        val tomorrow = TimeUtils.startOfDayMillis(LocalDate.now().plusDays(1))

        
        taskDao.hardDeleteGeneratedForRuleFromDate(userId, ruleId, TaskOriginType.RECURRENCE.raw, tomorrow)

        
        taskDao.detachFromRule(userId, ruleId, now, SyncStatus.UPDATED_LOCAL.raw)

        ruleDao.update(rule.copy(deletedAt = now, updatedAt = now, syncStatus = SyncStatus.DELETED_LOCAL.raw))
        syncTrigger.trigger()
    }

    suspend fun detachRecurrence(userId: String, ruleId: String) {
        val rule = ruleDao.getById(userId, ruleId) ?: return
        val now = System.currentTimeMillis()

        val seriesStart = TimeUtils.localDateFromMillis(rule.startDate)
        val dayStart = TimeUtils.startOfDayMillis(seriesStart)
        val dayEnd = TimeUtils.startOfDayMillis(seriesStart.plusDays(1))
        val generatedOnStart = taskDao.getForRange(userId, dayStart, dayEnd)
            .firstOrNull { it.recurrenceRuleId == ruleId && it.originType == TaskOriginType.RECURRENCE.raw && it.deletedAt == null }
        if (generatedOnStart != null) {
            taskDao.softDelete(userId, generatedOnStart.id, deletedAt = now, updatedAt = now, syncStatus = SyncStatus.DELETED_LOCAL.raw)
        }

        ruleDao.update(rule.copy(deletedAt = now, updatedAt = now, syncStatus = SyncStatus.DELETED_LOCAL.raw))
        syncTrigger.trigger()
    }

    suspend fun softDeleteRule(userId: String, ruleId: String) {
        val rule = ruleDao.getById(userId, ruleId) ?: return
        val now = System.currentTimeMillis()
        ruleDao.update(
            rule.copy(
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.DELETED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun softDeleteEntireSeries(userId: String, ruleId: String) {
        val rule = ruleDao.getById(userId, ruleId) ?: return
        val now = System.currentTimeMillis()

        taskDao.hardDeleteAllGeneratedForRule(userId, ruleId, TaskOriginType.RECURRENCE.raw)

        val source = taskDao.getById(userId, rule.sourceTaskId)
        if (source != null) {
            taskDao.softDelete(userId, source.id, deletedAt = now, updatedAt = now, syncStatus = SyncStatus.DELETED_LOCAL.raw)
        }

        ruleDao.update(rule.copy(deletedAt = now, updatedAt = now, syncStatus = SyncStatus.DELETED_LOCAL.raw))
        syncTrigger.trigger()
    }

    suspend fun generateUpcomingTasksIfNeeded(userId: String) {
        val todayDate = LocalDate.now()
        val today = TimeUtils.startOfDayMillis(todayDate)
        val windowEnd = TimeUtils.startOfDayMillis(todayDate.plusDays(LOOKAHEAD_DAYS))

        val activeRules = ruleDao.getActiveForGeneration(userId, today = today, windowEnd = windowEnd)
        activeRules.forEach { rule ->
            val targetEnd = minOf(rule.endDate ?: windowEnd, windowEnd)
            val last = rule.lastGeneratedAt
            if (last != null && last >= targetEnd) return@forEach
            generateTasksForRule(userId, rule)
        }
    }

    private suspend fun generateTasksForRule(userId: String, rule: RecurrenceRuleEntity) {
        val sourceTask = taskDao.getById(userId, rule.sourceTaskId) ?: return
        val taskTitle = sourceTask.title

        val startDate = TimeUtils.localDateFromMillis(rule.startDate)
        val todayDate = LocalDate.now()
        val windowEndDate = todayDate.plusDays(LOOKAHEAD_DAYS)
        val generateUntilDate = minOf(rule.endDate?.let { TimeUtils.localDateFromMillis(it) } ?: windowEndDate, windowEndDate)

        val lastGeneratedDate = rule.lastGeneratedAt?.let { TimeUtils.localDateFromMillis(it) }
        val generationStartCandidate = if (lastGeneratedDate != null && !lastGeneratedDate.isBefore(startDate)) {
            lastGeneratedDate.plusDays(1)
        } else {
            startDate
        }

        var current = generationStartCandidate
        val effectiveHasReminder = sourceTask.hasReminder && sourceTask.startTime != null
        while (!current.isAfter(generateUntilDate)) {
            if (shouldGenerate(rule, startDate, current)) {
                val dayMillis = TimeUtils.startOfDayMillis(current)
                val endMillis = TimeUtils.startOfDayMillis(current.plusDays(1))
                val exists = taskDao.getForRangeAny(userId, dayMillis, endMillis).any { t ->
                    t.recurrenceRuleId == rule.id &&
                        t.originType == TaskOriginType.RECURRENCE.raw &&
                        t.date == dayMillis
                }
                if (!exists) {
                    val minPos = taskDao.getMinPositionForDay(userId, dayMillis, endMillis) ?: 0
                    val position = minPos - 1

                    val startTimeMillis = sourceTask.startTime?.let { stored ->
                        val localTime = TimeUtils.localTimeFromMillis(stored)
                        TimeUtils.millisFromLocalTime(localTime, current)
                    }

                    val now = System.currentTimeMillis()
                    val task = TaskEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = taskTitle,
                        description = sourceTask.description,
                        notes = sourceTask.notes,
                        imageData = sourceTask.imageData,
                        date = dayMillis,
                        startTime = startTimeMillis,
                        priority = sourceTask.priority,
                        status = TaskStatus.PLANNED.raw,
                        completedAt = null,
                        hasReminder = effectiveHasReminder && startTimeMillis != null,
                        reminderOffsetMinutes = sourceTask.reminderOffsetMinutes,
                        categoryId = sourceTask.categoryId,
                        parentTaskId = null,
                        goalStepId = null,
                        originType = TaskOriginType.RECURRENCE.raw,
                        instanceDate = dayMillis,
                        recurrenceRuleId = rule.id,
                        templateItemId = null,
                        templateApplicationId = null,
                        position = position,
                        searchText = (taskTitle + " " + (sourceTask.description ?: "")).lowercase(),
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                        syncStatus = SyncStatus.CREATED_LOCAL.raw
                    )
                    taskDao.upsert(task)
                }
            }
            current = current.plusDays(1)
        }

        val newLastGenerated = if (!generationStartCandidate.isAfter(generateUntilDate)) {
            TimeUtils.startOfDayMillis(generateUntilDate)
        } else {
            rule.lastGeneratedAt
        }

        val now = System.currentTimeMillis()
        ruleDao.update(rule.copy(lastGeneratedAt = newLastGenerated, updatedAt = now, syncStatus = SyncStatus.UPDATED_LOCAL.raw))
    }

    private fun shouldGenerate(rule: RecurrenceRuleEntity, seriesStart: LocalDate, date: LocalDate): Boolean {
        val interval = maxOf(1, rule.intervalValue)
        return when (rule.frequency) {
            RecurrenceFrequency.DAILY.raw -> {
                val days = ChronoUnit.DAYS.between(seriesStart, date).toInt()
                days >= 0 && (days % interval == 0)
            }
            RecurrenceFrequency.WEEKLY.raw -> {
                val startWeek = seriesStart.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val targetWeek = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val weeks = ChronoUnit.WEEKS.between(startWeek, targetWeek).toInt()
                if (weeks < 0 || weeks % interval != 0) return false
                val weekday = date.dayOfWeek.value
                (rule.weekdaysMask and (1 shl weekday)) != 0
            }
            RecurrenceFrequency.MONTHLY.raw -> {
                val months = ChronoUnit.MONTHS.between(seriesStart.withDayOfMonth(1), date.withDayOfMonth(1)).toInt()
                if (months < 0 || months % interval != 0) return false
                val dayOfMonth = if (rule.dayOfMonth == 0) seriesStart.dayOfMonth else rule.dayOfMonth
                date.dayOfMonth == dayOfMonth
            }
            RecurrenceFrequency.YEARLY.raw -> {
                val years = ChronoUnit.YEARS.between(seriesStart.withDayOfYear(1), date.withDayOfYear(1)).toInt()
                if (years < 0 || years % interval != 0) return false
                val expectedMonth = if (rule.monthOfYear == 0) seriesStart.monthValue else rule.monthOfYear
                val expectedDay = if (rule.dayOfMonth == 0) seriesStart.dayOfMonth else rule.dayOfMonth
                date.monthValue == expectedMonth && date.dayOfMonth == expectedDay
            }
            else -> false
        }
    }
}
