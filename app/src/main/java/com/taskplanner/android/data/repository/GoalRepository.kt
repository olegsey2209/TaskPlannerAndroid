package com.taskplanner.android.data.repository

import com.taskplanner.android.core.model.GoalStatus
import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.core.model.TaskOriginType
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.dao.GoalDao
import com.taskplanner.android.data.local.dao.GoalStepDao
import com.taskplanner.android.data.local.dao.TaskDao
import com.taskplanner.android.data.local.entities.GoalEntity
import com.taskplanner.android.data.local.entities.GoalStepEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class GoalRepository(
    private val goalDao: GoalDao,
    private val stepDao: GoalStepDao,
    private val taskDao: TaskDao,
    private val syncTrigger: SyncTrigger
) {
    fun observeGoals(userId: String): Flow<List<GoalEntity>> = goalDao.observeAll(userId)

    fun observeStepsForGoal(userId: String, goalId: String): Flow<List<GoalStepEntity>> =
        stepDao.observeForGoal(userId, goalId)

    fun observeLinkedTaskForStep(userId: String, stepId: String): Flow<TaskEntity?> =
        taskDao.observeFirstForGoalStep(userId, stepId)

    suspend fun createGoal(userId: String, title: String, description: String?) {
        val t = title.trim()
        if (t.isEmpty()) return
        val now = System.currentTimeMillis()
        val goal = GoalEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = t,
            description = description?.takeIf { it.isNotBlank() },
            status = GoalStatus.ACTIVE.raw,
            progressCached = 0.0,
            completedAt = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        goalDao.upsert(goal)
        syncTrigger.trigger()
    }

    suspend fun updateGoal(userId: String, goalId: String, title: String, description: String?) {
        val existing = goalDao.getById(userId, goalId) ?: return
        val now = System.currentTimeMillis()
        goalDao.update(
            existing.copy(
                title = title.trim().ifEmpty { existing.title },
                description = description?.takeIf { it.isNotBlank() },
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun createStep(userId: String, goalId: String, title: String, description: String?) {
        val goal = goalDao.getById(userId, goalId) ?: return
        val now = System.currentTimeMillis()
        val orderIndex = (stepDao.getMaxOrderIndex(userId, goalId) ?: -1) + 1
        val step = GoalStepEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            goalId = goalId,
            title = title.trim(),
            description = description?.takeIf { it.isNotBlank() },
            orderIndex = orderIndex,
            isCompleted = false,
            plannedDate = null,
            completedAt = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        stepDao.upsert(step)
        updateGoalProgress(goal)
        syncTrigger.trigger()
    }

    suspend fun updateStep(userId: String, stepId: String, title: String, description: String?) {
        val step = stepDao.getById(userId, stepId) ?: return
        val now = System.currentTimeMillis()
        stepDao.update(
            step.copy(
                title = title.trim().ifEmpty { step.title },
                description = description?.takeIf { it.isNotBlank() },
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        )
        val goal = goalDao.getById(userId, step.goalId) ?: return
        updateGoalProgress(goal)
        syncTrigger.trigger()
    }

    suspend fun softDeleteStep(userId: String, stepId: String) {
        val step = stepDao.getById(userId, stepId) ?: return
        val now = System.currentTimeMillis()
        taskDao.clearGoalStepIdForGoalStep(userId, stepId, updatedAt = now, syncStatus = SyncStatus.UPDATED_LOCAL.raw)
        stepDao.update(
            step.copy(
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.DELETED_LOCAL.raw
            )
        )
        val goal = goalDao.getById(userId, step.goalId) ?: return
        updateGoalProgress(goal)
        syncTrigger.trigger()
    }

    suspend fun softDeleteGoal(userId: String, goalId: String) {
        val goal = goalDao.getById(userId, goalId) ?: return
        val steps = stepDao.getForGoal(userId, goalId)
        val now = System.currentTimeMillis()
        steps.forEach { step ->
            taskDao.clearGoalStepIdForGoalStep(userId, step.id, updatedAt = now, syncStatus = SyncStatus.UPDATED_LOCAL.raw)
            stepDao.update(
                step.copy(
                    deletedAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.DELETED_LOCAL.raw
                )
            )
        }
        goalDao.update(
            goal.copy(
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.DELETED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun softDeleteTaskFromStep(userId: String, stepId: String) {
        val linked = taskDao.getForGoalStep(userId, stepId)
        if (linked.isEmpty()) return
        val now = System.currentTimeMillis()
        linked.forEach { task ->
            taskDao.softDelete(userId, task.id, deletedAt = now, updatedAt = now, syncStatus = SyncStatus.DELETED_LOCAL.raw)
        }
        syncTrigger.trigger()
    }

    suspend fun toggleStepCompletion(userId: String, stepId: String) {
        val step = stepDao.getById(userId, stepId) ?: return
        val now = System.currentTimeMillis()
        val completed = !step.isCompleted
        stepDao.update(
            step.copy(
                isCompleted = completed,
                completedAt = if (completed) now else null,
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        )

        val linked = taskDao.getForGoalStep(userId, stepId)
        linked.forEach { task ->
            val hasActiveSubtasks = task.parentTaskId == null && taskDao.getSubtasks(userId, task.id).any { it.deletedAt == null }
            if (hasActiveSubtasks) {
                return@forEach
            }

            val updatedTask = task.copy(
                status = if (completed) TaskStatus.COMPLETED.raw else TaskStatus.PLANNED.raw,
                completedAt = if (completed) now else null,
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
            taskDao.update(updatedTask)
        }

        val goal = goalDao.getById(userId, step.goalId) ?: return
        updateGoalProgress(goal)
        syncTrigger.trigger()
    }

    suspend fun createTaskFromStep(userId: String, stepId: String, date: LocalDate) {
        val step = stepDao.getById(userId, stepId) ?: return
        if (taskDao.getForGoalStep(userId, stepId).isNotEmpty()) return

        val now = System.currentTimeMillis()
        val dateMillis = TimeUtils.startOfDayMillis(date)
        val startOfDay = dateMillis
        val endOfDay = TimeUtils.startOfDayMillis(date.plusDays(1))
        val minPosition = taskDao.getMinPositionForDay(userId, startOfDay, endOfDay) ?: 0
        val position = minPosition - 1

        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = step.title,
            description = step.description,
            notes = null,
            imageData = null,
            date = dateMillis,
            startTime = null,
            priority = 1,
            status = if (step.isCompleted) TaskStatus.COMPLETED.raw else TaskStatus.PLANNED.raw,
            completedAt = if (step.isCompleted) now else null,
            hasReminder = false,
            reminderOffsetMinutes = 15,
            categoryId = null,
            parentTaskId = null,
            goalStepId = step.id,
            originType = TaskOriginType.GOAL_STEP.raw,
            instanceDate = dateMillis,
            recurrenceRuleId = null,
            templateItemId = null,
            templateApplicationId = null,
            position = position,
            searchText = (step.title + " " + (step.description ?: "")).lowercase(),
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        taskDao.upsert(task)
        syncTrigger.trigger()
    }

    suspend fun createTaskFromStepDetailed(
        userId: String,
        stepId: String,
        description: String?,
        date: LocalDate,
        startTime: LocalTime?,
        priority: TaskPriority,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int,
        categoryId: String?,
        imageData: ByteArray?
    ): String? {
        val step = stepDao.getById(userId, stepId) ?: return null
        if (taskDao.getForGoalStep(userId, stepId).isNotEmpty()) return null

        val now = System.currentTimeMillis()
        val startOfDayMillis = TimeUtils.startOfDayMillis(date)
        val endOfDayMillis = TimeUtils.startOfDayMillis(date.plusDays(1))
        val minPosition = taskDao.getMinPositionForDay(userId, startOfDayMillis, endOfDayMillis) ?: 0
        val position = minPosition - 1

        val taskId = UUID.randomUUID().toString()
        val normalizedDescription = description?.takeIf { it.isNotBlank() }
        val searchText = (step.title + " " + (normalizedDescription ?: "")).lowercase()

        val task = TaskEntity(
            id = taskId,
            userId = userId,
            title = step.title,
            description = normalizedDescription,
            notes = null,
            imageData = imageData,
            date = startOfDayMillis,
            startTime = startTime?.let { TimeUtils.millisFromLocalTime(it, date) },
            priority = priority.raw,
            status = if (step.isCompleted) TaskStatus.COMPLETED.raw else TaskStatus.PLANNED.raw,
            completedAt = if (step.isCompleted) now else null,
            hasReminder = hasReminder,
            reminderOffsetMinutes = reminderOffsetMinutes,
            categoryId = categoryId,
            parentTaskId = null,
            goalStepId = step.id,
            originType = TaskOriginType.GOAL_STEP.raw,
            instanceDate = startOfDayMillis,
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
        syncTrigger.trigger()
        return taskId
    }

    private suspend fun updateGoalProgress(goal: GoalEntity) {
        val total = stepDao.countTotal(goal.userId, goal.id)
        val completed = stepDao.countCompleted(goal.userId, goal.id)
        val progress = if (total == 0) 0.0 else completed.toDouble() / total.toDouble()

        val now = System.currentTimeMillis()
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
}
