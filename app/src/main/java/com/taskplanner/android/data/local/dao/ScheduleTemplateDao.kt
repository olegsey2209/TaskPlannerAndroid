package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskplanner.android.data.local.entities.ScheduleTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTemplateDao {
    @Query("SELECT * FROM schedule_templates WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<ScheduleTemplateEntity>>

    @Query("SELECT * FROM schedule_templates WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(userId: String, id: String): ScheduleTemplateEntity?

    @Query("SELECT * FROM schedule_templates WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): ScheduleTemplateEntity?

    @Query("SELECT * FROM schedule_templates WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<ScheduleTemplateEntity>

    @Query("SELECT * FROM schedule_templates WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<ScheduleTemplateEntity>

    @Query("UPDATE schedule_templates SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: ScheduleTemplateEntity)

    @Update
    suspend fun update(template: ScheduleTemplateEntity)
}
