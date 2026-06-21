package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.taskplanner.android.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND date >= :startInclusive AND date < :endExclusive
        ORDER BY position ASC, createdAt DESC, startTime ASC
        """
    )
    fun observeForDay(userId: String, startInclusive: Long, endExclusive: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId = :parentTaskId
        ORDER BY position ASC, createdAt ASC, id ASC
        """
    )
    suspend fun getSubtasks(userId: String, parentTaskId: String): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId = :parentTaskId
        ORDER BY position ASC, createdAt ASC, id ASC
        """
    )
    fun observeSubtasks(userId: String, parentTaskId: String): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId = :parentTaskId
        """
    )
    suspend fun countSubtasks(userId: String, parentTaskId: String): Int

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND goalStepId = :goalStepId
        ORDER BY createdAt DESC
        """
    )
    suspend fun getForGoalStep(userId: String, goalStepId: String): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND goalStepId = :goalStepId
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    fun observeFirstForGoalStep(userId: String, goalStepId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(userId: String, id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE userId = :userId AND id IN (:ids) AND deletedAt IS NULL")
    fun observeByIds(userId: String, ids: List<String>): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND id IN (:ids)
        """
    )
    suspend fun getByIds(userId: String, ids: List<String>): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND id IN (:ids)
        """
    )
    suspend fun getByIdsAny(userId: String, ids: List<String>): List<TaskEntity>

    @Query(
        """
        SELECT MIN(position) FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND date >= :startInclusive AND date < :endExclusive
        """
    )
    suspend fun getMinPositionForDay(userId: String, startInclusive: Long, endExclusive: Long): Int?

    @Query(
        """
        SELECT MAX(position) FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND date >= :startInclusive AND date < :endExclusive
        """
    )
    suspend fun getMaxPositionForDay(userId: String, startInclusive: Long, endExclusive: Long): Int?

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND date >= :startInclusive AND date < :endExclusive
          AND id NOT IN (
              SELECT sourceTaskId FROM recurrence_rules
              WHERE userId = :userId
                AND deletedAt IS NULL
                AND sourceTaskId IS NOT NULL
                AND sourceTaskId != ''
          )
        """
    )
    suspend fun countTopLevelForDay(userId: String, startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT MAX(position) FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId = :parentTaskId
        """
    )
    suspend fun getMaxPositionForParent(userId: String, parentTaskId: String): Int?

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND date >= :startInclusive AND date < :endExclusive
        ORDER BY date ASC
        """
    )
    suspend fun getForRange(userId: String, startInclusive: Long, endExclusive: Long): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND date >= :startInclusive AND date < :endExclusive
          AND id NOT IN (
              SELECT sourceTaskId FROM recurrence_rules
              WHERE userId = :userId
                AND deletedAt IS NULL
                AND sourceTaskId IS NOT NULL
                AND sourceTaskId != ''
          )
        ORDER BY date ASC
        """
    )
    suspend fun getVisibleForStatistics(
        userId: String,
        startInclusive: Long,
        endExclusive: Long
    ): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND date >= :startInclusive AND date < :endExclusive
        ORDER BY position ASC, createdAt DESC, id ASC
        """
    )
    suspend fun getTopLevelForDay(userId: String, startInclusive: Long, endExclusive: Long): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND date >= :startInclusive AND date < :endExclusive
        ORDER BY date ASC
        """
    )
    suspend fun getForRangeAny(userId: String, startInclusive: Long, endExclusive: Long): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND originType = :originType
          AND recurrenceRuleId IS NOT NULL
        """
    )
    suspend fun getActiveGeneratedRecurrenceTasks(userId: String, originType: Int): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND date >= :startInclusive AND date < :endExclusive
          AND (
            LOWER(title) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(COALESCE(description, '')) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchByTitleInRange(
        userId: String,
        query: String,
        startInclusive: Long,
        endExclusive: Long,
        limit: Int
    ): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND parentTaskId IS NULL
          AND (
            LOWER(title) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(COALESCE(description, '')) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchAllByQuery(userId: String, query: String, limit: Int): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<TaskEntity>

    @Query("UPDATE tasks SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE userId = :userId AND templateApplicationId = :applicationId")
    suspend fun hardDeleteForTemplateApplication(userId: String, applicationId: String)

    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 3 WHERE userId = :userId AND templateApplicationId = :applicationId AND deletedAt IS NULL")
    suspend fun softDeleteForTemplateApplication(userId: String, applicationId: String, deletedAt: Long)

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE userId = :userId
          AND templateApplicationId = :applicationId
          AND deletedAt IS NULL
        """
    )
    suspend fun countForTemplateApplication(userId: String, applicationId: String): Int

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :userId
          AND templateApplicationId = :applicationId
          AND deletedAt IS NULL
        ORDER BY date ASC, position ASC, createdAt DESC
        """
    )
    fun observeForTemplateApplication(userId: String, applicationId: String): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id AND userId = :userId")
    suspend fun softDelete(userId: String, id: String, deletedAt: Long, updatedAt: Long, syncStatus: Int)

    @Query(
        """
        UPDATE tasks
        SET categoryId = :newCategoryId, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId
          AND categoryId = :oldCategoryId
          AND deletedAt IS NULL
        """
    )
    suspend fun replaceCategoryId(userId: String, oldCategoryId: String, newCategoryId: String, updatedAt: Long, syncStatus: Int)

    @Query(
        """
        UPDATE tasks
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND recurrenceRuleId = :ruleId
        """
    )
    suspend fun softDeleteForRecurrenceRule(userId: String, ruleId: String, deletedAt: Long, updatedAt: Long, syncStatus: Int)

    @Query(
        """
        DELETE FROM tasks
        WHERE userId = :userId
          AND recurrenceRuleId = :ruleId
          AND originType = :originType
          AND deletedAt IS NULL
          AND date >= :cutoff
        """
    )
    suspend fun hardDeleteGeneratedForRuleFromDate(userId: String, ruleId: String, originType: Int, cutoff: Long)

    @Query(
        """
        UPDATE tasks SET deletedAt = :now, updatedAt = :now, syncStatus = 3
        WHERE userId = :userId
          AND recurrenceRuleId = :ruleId
          AND originType = :originType
          AND deletedAt IS NULL
          AND date >= :cutoff
        """
    )
    suspend fun softDeleteGeneratedForRuleFromDate(userId: String, ruleId: String, originType: Int, cutoff: Long, now: Long)

    @Query(
        """
        UPDATE tasks
        SET recurrenceRuleId = NULL, instanceDate = NULL, originType = :manualOriginType,
            updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId AND recurrenceRuleId = :ruleId
          AND deletedAt IS NULL AND date < :before
        """
    )
    suspend fun detachFromRuleBefore(
        userId: String,
        ruleId: String,
        before: Long,
        manualOriginType: Int,
        updatedAt: Long,
        syncStatus: Int
    )

    @Query(
        """
        UPDATE tasks
        SET deletedAt = NULL, recurrenceRuleId = NULL, instanceDate = NULL,
            originType = :manualOriginType, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId
          AND recurrenceRuleId = :ruleId
          AND originType = :recurrenceOriginType
          AND date >= :startInclusive AND date < :endExclusive
        """
    )
    suspend fun restoreGeneratedTaskForDay(
        userId: String,
        ruleId: String,
        recurrenceOriginType: Int,
        manualOriginType: Int,
        startInclusive: Long,
        endExclusive: Long,
        updatedAt: Long,
        syncStatus: Int
    )

    @Query(
        """
        DELETE FROM tasks
        WHERE userId = :userId
          AND recurrenceRuleId = :ruleId
          AND originType = :originType
          AND deletedAt IS NULL
        """
    )
    suspend fun hardDeleteAllGeneratedForRule(userId: String, ruleId: String, originType: Int)

    @Query(
        """
        UPDATE tasks
        SET recurrenceRuleId = NULL, instanceDate = NULL, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId AND recurrenceRuleId = :ruleId AND deletedAt IS NULL
        """
    )
    suspend fun detachFromRule(userId: String, ruleId: String, updatedAt: Long, syncStatus: Int)

    @Query(
        """
        UPDATE tasks
        SET goalStepId = NULL, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId AND goalStepId = :goalStepId
        """
    )
    suspend fun clearGoalStepIdForGoalStep(userId: String, goalStepId: String, updatedAt: Long, syncStatus: Int)

    @Query("UPDATE tasks SET date = :newDate, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id AND userId = :userId")
    suspend fun updateDate(userId: String, id: String, newDate: Long, updatedAt: Long, syncStatus: Int)

    @Query("UPDATE tasks SET position = :position, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id AND userId = :userId")
    suspend fun updatePosition(userId: String, id: String, position: Int, updatedAt: Long, syncStatus: Int)

    @Transaction
    suspend fun updatePositionsBatch(userId: String, orderedIds: List<String>, updatedAt: Long, syncStatus: Int) {
        orderedIds.forEachIndexed { index, id ->
            updatePosition(userId, id, index, updatedAt, syncStatus)
        }
    }
}
