package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskplanner.android.data.local.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId AND deletedAt IS NULL ORDER BY status ASC, createdAt DESC")
    fun observeAll(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND deletedAt IS NULL ORDER BY status ASC, createdAt DESC")
    suspend fun getAll(userId: String): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id AND userId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(userId: String, id: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<GoalEntity>

    @Query("UPDATE goals SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)
}
