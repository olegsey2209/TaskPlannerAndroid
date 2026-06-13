package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskplanner.android.data.local.entities.RecurrenceRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceRuleDao {
    @Query("SELECT * FROM recurrence_rules WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<RecurrenceRuleEntity>>

    @Query("SELECT * FROM recurrence_rules WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(userId: String, id: String): RecurrenceRuleEntity?

    @Query("SELECT * FROM recurrence_rules WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): RecurrenceRuleEntity?

    @Query("SELECT * FROM recurrence_rules WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<RecurrenceRuleEntity>

    @Query(
        """
        SELECT * FROM recurrence_rules
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND startDate <= :windowEnd
          AND (endDate IS NULL OR endDate >= :today)
        ORDER BY createdAt DESC
        """
    )
    suspend fun getActiveForGeneration(userId: String, today: Long, windowEnd: Long): List<RecurrenceRuleEntity>

    @Query("SELECT * FROM recurrence_rules WHERE sourceTaskId = :sourceTaskId AND userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForSourceTask(userId: String, sourceTaskId: String): RecurrenceRuleEntity?

    @Query("SELECT * FROM recurrence_rules WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<RecurrenceRuleEntity>

    @Query("UPDATE recurrence_rules SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurrenceRuleEntity)

    @Update
    suspend fun update(rule: RecurrenceRuleEntity)
}
