package com.taskplanner.android.data.repository

import com.taskplanner.android.core.model.GoalStatus
import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.core.model.TaskOriginType
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.dao.GoalDao
import com.taskplanner.android.data.local.dao.GoalStepDao
import com.taskplanner.android.data.local.dao.RecurrenceRuleDao
import com.taskplanner.android.data.local.dao.TaskDao
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.notifications.NotificationHelper
import com.taskplanner.android.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao,
    private val goalDao: GoalDao,
    private val goalStepDao: GoalStepDao,
    private val recurrenceRuleDao: RecurrenceRuleDao,
    private val syncTrigger: SyncTrigger,
    private val context: android.content.Context? = null
) {
    fun observeTasksForDate(userId: String, date: LocalDate): Flow<List<TaskEntity>> {
        val start = TimeUtils.startOfDayMillis(date)
        val end = TimeUtils.startOfDayMillis(date.plusDays(1))
        return combine(
            taskDao.observeForDay(userId, startInclusive = start, endExclusive = end),
            recurrenceRuleDao.observeAll(userId)
        ) { tasks, rules ->
            val sourceIds = rules.map { it.sourceTaskId }.toSet()
            tasks.filterNot { it.id in sourceIds }
        }
    }

    fun observeSubtasks(userId: String, parentTaskId: String): Flow<List<TaskEntity>> {
        return taskDao.observeSubtasks(userId, parentTaskId)
    }

    fun observeTasksForMonth(userId: String, yearMonth: java.time.YearMonth): Flow<List<TaskEntity>> {
        val start = TimeUtils.startOfDayMillis(yearMonth.atDay(1))
        val end = TimeUtils.startOfDayMillis(yearMonth.plusMonths(1).atDay(1))
        return taskDao.observeForDay(userId, startInclusive = start, endExclusive = end)
    }

    suspend fun countSubtasks(userId: String, parentTaskId: String): Int {
        return taskDao.countSubtasks(userId, parentTaskId)
    }

    suspend fun createTask(
        userId: String,
        title: String,
        description: String?,
        imageData: ByteArray? = null,
        date: LocalDate,
        priority: TaskPriority = TaskPriority.MEDIUM,
        categoryId: String? = null,
        startTime: LocalTime? = null,
        hasReminder: Boolean = false,
        reminderOffsetMinutes: Int = 15,
        parentTaskId: String? = null,
        goalStepId: String? = null,
        originType: TaskOriginType = TaskOriginType.MANUAL
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return

        val now = System.currentTimeMillis()
        val startOfDay = TimeUtils.startOfDayMillis(date)
        val position = if (parentTaskId != null) {
            val max = taskDao.getMaxPositionForParent(userId, parentTaskId) ?: -1
            max + 1
        } else {
            val min = taskDao.getMinPositionForDay(userId, startOfDay, TimeUtils.startOfDayMillis(date.plusDays(1))) ?: 0
            min - 1
        }
        val searchText = (normalizedTitle + " " + (description ?: "")).lowercase()

        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = normalizedTitle,
            description = description?.takeIf { it.isNotBlank() },
            notes = null,
            imageData = imageData,
            date = startOfDay,
            startTime = startTime?.let { TimeUtils.millisFromLocalTime(it, date) },
            priority = priority.raw,
            status = TaskStatus.PLANNED.raw,
            completedAt = null,
            hasReminder = hasReminder,
            reminderOffsetMinutes = reminderOffsetMinutes,
            categoryId = categoryId,
            parentTaskId = parentTaskId,
            goalStepId = goalStepId,
            originType = originType.raw,
            instanceDate = null,
            recurrenceRuleId = null,
            templateItemId = null,
            templateApplicationId = null,
            position = position,
            searchText = searchText,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        taskDao.upsert(task)
        context?.let { ctx -> if (hasReminder) NotificationHelper.scheduleReminder(ctx, task) }
        syncTrigger.trigger()
    }

    suspend fun updateTask(
        userId: String,
        taskId: String,
        date: LocalDate,
        title: String,
        description: String?,
        imageData: ByteArray?,
        removeImage: Boolean = false,
        priority: TaskPriority,
        categoryId: String?,
        startTime: LocalTime?,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int
    ) {
        val task = taskDao.getById(userId, taskId) ?: return
        if (task.parentTaskId == null) {
            val existingDate = TimeUtils.localDateFromMillis(task.date)
            if (existingDate != date) {
                moveTaskToDate(userId, taskId, date)
            }
        }
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return

        val persistedDate = TimeUtils.localDateFromMillis(taskDao.getById(userId, taskId)?.date ?: task.date)
        val now = System.currentTimeMillis()
        val updated = task.copy(
            title = normalizedTitle,
            description = description?.takeIf { it.isNotBlank() },
            imageData = when { removeImage -> null; imageData != null -> imageData; else -> task.imageData },
            priority = priority.raw,
            categoryId = categoryId,
            startTime = startTime?.let { TimeUtils.millisFromLocalTime(it, persistedDate) },
            hasReminder = hasReminder,
            reminderOffsetMinutes = reminderOffsetMinutes,
            searchText = (normalizedTitle + " " + (description ?: "")).lowercase(),
            updatedAt = now,
            syncStatus = SyncStatus.UPDATED_LOCAL.raw
        )
        taskDao.update(updated)
        context?.let { ctx ->
            NotificationHelper.cancelReminder(ctx, taskId)
            if (updated.hasReminder) NotificationHelper.scheduleReminder(ctx, updated)
        }
        syncTrigger.trigger()
    }

    suspend fun createSubtask(userId: String, parentTaskId: String, title: String) {
        val parent = taskDao.getById(userId, parentTaskId) ?: return
        val date = TimeUtils.localDateFromMillis(parent.date)
        createTask(
            userId = userId,
            title = title,
            description = null,
            date = date,
            priority = TaskPriority.values().firstOrNull { it.raw == parent.priority } ?: TaskPriority.MEDIUM,
            categoryId = parent.categoryId,
            startTime = null,
            hasReminder = false,
            reminderOffsetMinutes = parent.reminderOffsetMinutes,
            parentTaskId = parentTaskId,
            goalStepId = null,
            originType = TaskOriginType.MANUAL
        )
        updateParentCompletionFromSubtasks(userId, parentTaskId)
    }

    suspend fun toggleCompletion(userId: String, taskId: String) {
        val task = taskDao.getById(userId, taskId) ?: return
        val hasActiveSubtasks = taskDao.getSubtasks(userId, taskId).any { it.deletedAt == null }
        if (task.parentTaskId == null && hasActiveSubtasks) {
            return
        }

        val now = System.currentTimeMillis()
        val willComplete = task.status != TaskStatus.COMPLETED.raw
        val updated = task.copy(
            status = if (willComplete) TaskStatus.COMPLETED.raw else TaskStatus.PLANNED.raw,
            completedAt = if (willComplete) now else null,
            updatedAt = now,
            syncStatus = SyncStatus.UPDATED_LOCAL.raw
        )
        taskDao.update(updated)

        if (task.goalStepId != null) {
            updateGoalStepCompletionFromLinkedTask(userId, goalStepId = task.goalStepId, isCompleted = willComplete, now = now)
        }

        if (task.parentTaskId != null) {
            updateParentCompletionFromSubtasks(userId, task.parentTaskId)
        }
        syncTrigger.trigger()
    }

    private suspend fun updateGoalStepCompletionFromLinkedTask(userId: String, goalStepId: String, isCompleted: Boolean, now: Long) {
        val step = goalStepDao.getById(userId, goalStepId) ?: return

        if (step.isCompleted != isCompleted) {
            goalStepDao.update(
                step.copy(
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) now else null,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
            )
        }

        val linkedTasks = taskDao.getForGoalStep(userId, goalStepId)
        linkedTasks.forEach { linked ->
            val hasActiveSubtasks = linked.parentTaskId == null && taskDao.getSubtasks(userId, linked.id).any { it.deletedAt == null }
            if (hasActiveSubtasks) return@forEach

            val shouldBeCompleted = isCompleted
            val isTaskCompleted = linked.status == TaskStatus.COMPLETED.raw
            if (shouldBeCompleted == isTaskCompleted) return@forEach

            taskDao.update(
                linked.copy(
                    status = if (shouldBeCompleted) TaskStatus.COMPLETED.raw else TaskStatus.PLANNED.raw,
                    completedAt = if (shouldBeCompleted) now else null,
                    updatedAt = now,
                    syncStatus = SyncStatus.UPDATED_LOCAL.raw
                )
            )
        }

        updateGoalProgress(userId, step.goalId, now)
    }

    private suspend fun updateGoalProgress(userId: String, goalId: String, now: Long) {
        val goal = goalDao.getById(userId, goalId) ?: return
        val total = goalStepDao.countTotal(userId, goalId)
        val completed = goalStepDao.countCompleted(userId, goalId)
        val progress = if (total == 0) 0.0 else completed.toDouble() / total.toDouble()

        val status = if (progress >= 1.0) GoalStatus.COMPLETED.raw else GoalStatus.ACTIVE.raw
        val completedAt = if (progress >= 1.0) (goal.completedAt ?: now) else null

        goalDao.update(
            goal.copy(
                progressCached = progress,
                status = status,
                completedAt = completedAt,
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        )
    }

    suspend fun updateParentCompletionFromSubtasks(userId: String, parentTaskId: String) {
        val parent = taskDao.getById(userId, parentTaskId) ?: return
        val subtasks = taskDao.getSubtasks(userId, parentTaskId).filter { it.deletedAt == null }
        if (subtasks.isEmpty()) return

        val allCompleted = subtasks.all { it.status == TaskStatus.COMPLETED.raw }
        val shouldBeCompleted = allCompleted
        val isCurrentlyCompleted = parent.status == TaskStatus.COMPLETED.raw
        if (shouldBeCompleted == isCurrentlyCompleted) return

        val now = System.currentTimeMillis()
        val updated = parent.copy(
            status = if (shouldBeCompleted) TaskStatus.COMPLETED.raw else TaskStatus.PLANNED.raw,
            completedAt = if (shouldBeCompleted) now else null,
            updatedAt = now,
            syncStatus = SyncStatus.UPDATED_LOCAL.raw
        )
        taskDao.update(updated)
    }

    suspend fun moveTaskToDate(userId: String, taskId: String, newDate: LocalDate) {
        val task = taskDao.getById(userId, taskId) ?: return
        val now = System.currentTimeMillis()
        val newDayMillis = TimeUtils.startOfDayMillis(newDate)
        taskDao.updateDate(userId, taskId, newDayMillis, now, SyncStatus.UPDATED_LOCAL.raw)

        if (task.parentTaskId == null) {
            val start = newDayMillis
            val end = TimeUtils.startOfDayMillis(newDate.plusDays(1))
            val minPosition = taskDao.getMinPositionForDay(userId, start, end) ?: 0
            taskDao.updatePosition(userId, taskId, minPosition - 1, now, SyncStatus.UPDATED_LOCAL.raw)

            val subtasks = taskDao.getSubtasks(userId, taskId).filter { it.deletedAt == null }
            subtasks.forEach { sub ->
                taskDao.updateDate(userId, sub.id, newDayMillis, now, SyncStatus.UPDATED_LOCAL.raw)
            }
            updateParentCompletionFromSubtasks(userId, taskId)
        }
        syncTrigger.trigger()
    }

    suspend fun softDeleteTask(userId: String, taskId: String) {
        val task = taskDao.getById(userId, taskId) ?: return
        val now = System.currentTimeMillis()
        taskDao.softDelete(userId, taskId, now, now, SyncStatus.DELETED_LOCAL.raw)
        if (task.parentTaskId == null) {
            val subtasks = taskDao.getSubtasks(userId, taskId).filter { it.deletedAt == null }
            subtasks.forEach { sub ->
                taskDao.softDelete(userId, sub.id, now, now, SyncStatus.DELETED_LOCAL.raw)
            }
        } else {
            updateParentCompletionFromSubtasks(userId, task.parentTaskId)
        }
        syncTrigger.trigger()
    }

    suspend fun persistCustomOrder(userId: String, orderedTaskIds: List<String>) {
        val now = System.currentTimeMillis()
        taskDao.updatePositionsBatch(userId, orderedTaskIds, now, SyncStatus.UPDATED_LOCAL.raw)
        syncTrigger.trigger()
    }

    suspend fun searchTasksByTitleInRange(userId: String, query: String, from: LocalDate, toExclusive: LocalDate, limit: Int = 200): List<TaskEntity> {
        val start = TimeUtils.startOfDayMillis(from)
        val end = TimeUtils.startOfDayMillis(toExclusive)
        return taskDao.searchByTitleInRange(userId, query.trim(), start, end, limit)
    }

    suspend fun searchAllTasks(userId: String, query: String, limit: Int = 200): List<TaskEntity> {
        return taskDao.searchAllByQuery(userId, query.trim(), limit)
    }
}
