package com.taskplanner.android.data.repository

import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.core.model.TaskOriginType
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.core.util.AppLimits
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.dao.ScheduleTemplateDao
import com.taskplanner.android.data.local.dao.ScheduleTemplateItemDao
import com.taskplanner.android.data.local.dao.TaskDao
import com.taskplanner.android.data.local.dao.TemplateApplicationDao
import com.taskplanner.android.data.local.entities.ScheduleTemplateEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateItemEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.data.local.entities.TemplateApplicationEntity
import com.taskplanner.android.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class TemplateRepository(
    private val templateDao: ScheduleTemplateDao,
    private val itemDao: ScheduleTemplateItemDao,
    private val applicationDao: TemplateApplicationDao,
    private val taskDao: TaskDao,
    private val syncTrigger: SyncTrigger
) {
    data class TemplateItemInput(
        val weekday: Int,
        val title: String,
        val description: String?,
        val startTime: LocalTime?,
        val priority: Int,
        val hasReminder: Boolean,
        val reminderOffsetMinutes: Int,
        val categoryId: String?
    )

    fun observeTemplates(userId: String): Flow<List<ScheduleTemplateEntity>> = templateDao.observeAll(userId)

    fun observeItems(userId: String, templateId: String): Flow<List<ScheduleTemplateItemEntity>> =
        itemDao.observeForTemplate(userId, templateId)

    fun observeItemCount(userId: String, templateId: String): Flow<Int> =
        itemDao.observeCountForTemplate(userId, templateId)

    fun observeApplications(userId: String, templateId: String): Flow<List<TemplateApplicationEntity>> {
        return applicationDao.observeForTemplate(userId, templateId)
    }

    suspend fun upsertTemplateWithItems(
        userId: String,
        templateId: String?,
        title: String,
        description: String?,
        items: List<TemplateItemInput>
    ): String? {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return null
        if (items.isEmpty() || items.any { it.title.trim().isEmpty() }) return null

        val now = System.currentTimeMillis()
        val id = templateId ?: UUID.randomUUID().toString()

        val existing = templateDao.getById(userId, id)
        val template = if (existing == null) {
            ScheduleTemplateEntity(
                id = id,
                userId = userId,
                title = normalizedTitle,
                description = description?.takeIf { it.isNotBlank() },
                isArchived = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.CREATED_LOCAL.raw
            )
        } else {
            existing.copy(
                title = normalizedTitle,
                description = description?.takeIf { it.isNotBlank() },
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        }
        templateDao.upsert(template)

        itemDao.softDeleteForTemplate(userId, id, deletedAt = now, updatedAt = now, syncStatus = SyncStatus.DELETED_LOCAL.raw)

        items.forEachIndexed { index, input ->
            val itemTitle = input.title.trim()
            if (itemTitle.isEmpty()) return@forEachIndexed
            val startTimeMillis = input.startTime?.let { time ->
                TimeUtils.millisFromLocalTime(time, LocalDate.now())
            }
            val entity = ScheduleTemplateItemEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                templateId = id,
                weekday = input.weekday,
                title = itemTitle,
                description = input.description?.takeIf { it.isNotBlank() },
                startTime = startTimeMillis,
                priority = input.priority,
                hasReminder = input.hasReminder,
                reminderOffsetMinutes = input.reminderOffsetMinutes,
                categoryId = input.categoryId,
                position = index,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.CREATED_LOCAL.raw
            )
            itemDao.upsert(entity)
        }

        syncTrigger.trigger()
        return id
    }

    suspend fun createTemplate(userId: String, title: String, description: String?) {
        val t = title.trim()
        if (t.isEmpty()) return
        val now = System.currentTimeMillis()
        val template = ScheduleTemplateEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = t,
            description = description?.takeIf { it.isNotBlank() },
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        templateDao.upsert(template)
        syncTrigger.trigger()
    }

    suspend fun updateTemplate(userId: String, templateId: String, title: String, description: String?) {
        val existing = templateDao.getById(userId, templateId) ?: return
        val now = System.currentTimeMillis()
        templateDao.update(
            existing.copy(
                title = title.trim().ifEmpty { existing.title },
                description = description?.takeIf { it.isNotBlank() },
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun softDeleteTemplate(userId: String, templateId: String) {
        val existing = templateDao.getById(userId, templateId) ?: return
        val items = itemDao.getForTemplate(userId, templateId).filter { it.deletedAt == null }
        val now = System.currentTimeMillis()
        items.forEach { item ->
            itemDao.update(
                item.copy(
                    deletedAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.DELETED_LOCAL.raw
                )
            )
        }
        templateDao.update(
            existing.copy(
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.DELETED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun applyTemplate(userId: String, templateId: String, startDate: LocalDate, endDate: LocalDate): String? {
        templateDao.getById(userId, templateId) ?: return null
        val items = itemDao.getForTemplate(userId, templateId).filter { it.deletedAt == null }
        if (items.isEmpty()) return null

        val now = System.currentTimeMillis()
        val application = TemplateApplicationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            templateId = templateId,
            startDate = TimeUtils.startOfDayMillis(startDate),
            endDate = TimeUtils.startOfDayMillis(endDate),
            isActive = true,
            lastGeneratedAt = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        applicationDao.upsert(application)

        var current = startDate
        val datesNeedingTimeSort = mutableSetOf<LocalDate>()
        while (!current.isAfter(endDate)) {
            val weekday = current.dayOfWeek.toWeekdayNumber()
            val dayItems = items.filter { it.weekday == weekday }
            val dayMillis = TimeUtils.startOfDayMillis(current)
            val endMillis = TimeUtils.startOfDayMillis(current.plusDays(1))
            var dayTaskCount = taskDao.countTopLevelForDay(userId, dayMillis, endMillis)
            for (item in dayItems) {
                if (dayTaskCount >= AppLimits.MAX_TASKS_PER_DAY) break

                val maxPos = taskDao.getMaxPositionForDay(userId, dayMillis, endMillis) ?: -1
                val position = maxPos + 1

                val taskStartTimeMillis = item.startTime?.let { stored ->
                    val localTime = TimeUtils.localTimeFromMillis(stored)
                    TimeUtils.millisFromLocalTime(localTime, current)
                }

                val task = TaskEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = item.title,
                    description = item.description,
                    notes = null,
                    imageData = null,
                    date = dayMillis,
                    startTime = taskStartTimeMillis,
                    priority = item.priority,
                    status = TaskStatus.PLANNED.raw,
                    completedAt = null,
                    hasReminder = item.hasReminder && item.startTime != null,
                    reminderOffsetMinutes = item.reminderOffsetMinutes,
                    categoryId = item.categoryId,
                    parentTaskId = null,
                    goalStepId = null,
                    originType = TaskOriginType.TEMPLATE.raw,
                    instanceDate = dayMillis,
                    recurrenceRuleId = null,
                    templateItemId = item.id,
                    templateApplicationId = application.id,
                    position = position,
                    searchText = (item.title + " " + (item.description ?: "")).lowercase(),
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                    syncStatus = SyncStatus.CREATED_LOCAL.raw
                )
                taskDao.upsert(task)
                dayTaskCount += 1
                if (taskStartTimeMillis != null) {
                    datesNeedingTimeSort += current
                }
            }

            current = current.plusDays(1)
        }

        datesNeedingTimeSort.forEach { date ->
            sortTasksByTime(userId, date)
        }

        syncTrigger.trigger()
        return application.id
    }

    private suspend fun sortTasksByTime(userId: String, date: LocalDate) {
        val start = TimeUtils.startOfDayMillis(date)
        val end = TimeUtils.startOfDayMillis(date.plusDays(1))
        val now = System.currentTimeMillis()

        taskDao.getTopLevelForDay(userId, start, end)
            .sortedWith(
                compareBy<TaskEntity> { timeSortKey(it) ?: Int.MAX_VALUE }
                    .thenBy { it.position }
                    .thenByDescending { it.createdAt }
                    .thenBy { it.id }
            )
            .forEachIndexed { index, task ->
                if (task.position == index) return@forEachIndexed
                taskDao.upsert(
                    task.copy(
                        position = index,
                        updatedAt = now,
                        syncStatus = localUpdateStatus(task.syncStatus)
                    )
                )
            }
    }

    private fun timeSortKey(task: TaskEntity): Int? {
        val millis = task.startTime ?: return null
        val time = TimeUtils.localTimeFromMillis(millis)
        return time.hour * 60 + time.minute
    }

    private fun localUpdateStatus(current: Int): Int {
        return if (current == SyncStatus.SYNCED.raw) SyncStatus.UPDATED_LOCAL.raw else current
    }

    suspend fun deleteTemplateApplication(userId: String, applicationId: String) {
        val application = applicationDao.getByIdAny(userId, applicationId) ?: return
        val now = System.currentTimeMillis()
        taskDao.softDeleteForTemplateApplication(userId, applicationId, now)
        applicationDao.upsert(application.copy(
            deletedAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.DELETED_LOCAL.raw
        ))
        syncTrigger.trigger()
    }

    suspend fun countTasksForApplication(userId: String, applicationId: String): Int {
        return taskDao.countForTemplateApplication(userId, applicationId)
    }

    fun observeTasksForApplication(userId: String, applicationId: String): Flow<List<TaskEntity>> {
        return taskDao.observeForTemplateApplication(userId, applicationId)
    }
}

private fun DayOfWeek.toWeekdayNumber(): Int {
    return when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }
}
