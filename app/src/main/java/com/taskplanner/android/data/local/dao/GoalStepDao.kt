package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskplanner.android.data.local.entities.GoalStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalStepDao {
    @Query("SELECT * FROM goal_steps WHERE userId = :userId AND goalId = :goalId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun observeForGoal(userId: String, goalId: String): Flow<List<GoalStepEntity>>

    @Query("SELECT * FROM goal_steps WHERE id = :id AND userId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(userId: String, id: String): GoalStepEntity?

    @Query("SELECT * FROM goal_steps WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): GoalStepEntity?

    @Query("SELECT * FROM goal_steps WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<GoalStepEntity>

    @Query("SELECT * FROM goal_steps WHERE userId = :userId AND goalId = :goalId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    suspend fun getForGoal(userId: String, goalId: String): List<GoalStepEntity>

    @Query("SELECT MAX(orderIndex) FROM goal_steps WHERE userId = :userId AND goalId = :goalId AND deletedAt IS NULL")
    suspend fun getMaxOrderIndex(userId: String, goalId: String): Int?

    @Query("SELECT COUNT(*) FROM goal_steps WHERE userId = :userId AND goalId = :goalId AND deletedAt IS NULL")
    suspend fun countTotal(userId: String, goalId: String): Int

    @Query("SELECT COUNT(*) FROM goal_steps WHERE userId = :userId AND goalId = :goalId AND deletedAt IS NULL AND isCompleted = 1")
    suspend fun countCompleted(userId: String, goalId: String): Int

    @Query("SELECT * FROM goal_steps WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<GoalStepEntity>

    @Query("UPDATE goal_steps SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(step: GoalStepEntity)

    @Update
    suspend fun update(step: GoalStepEntity)
}
