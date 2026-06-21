package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskplanner.android.data.local.entities.ScheduleTemplateItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTemplateItemDao {
    @Query(
        """
        SELECT * FROM schedule_template_items
        WHERE userId = :userId AND templateId = :templateId AND deletedAt IS NULL
        ORDER BY weekday ASC, position ASC, title ASC
        """
    )
    fun observeForTemplate(userId: String, templateId: String): Flow<List<ScheduleTemplateItemEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM schedule_template_items
        WHERE userId = :userId AND templateId = :templateId AND deletedAt IS NULL
        """
    )
    fun observeCountForTemplate(userId: String, templateId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM schedule_template_items
        WHERE userId = :userId AND templateId = :templateId AND deletedAt IS NULL
        """
    )
    suspend fun getForTemplate(userId: String, templateId: String): List<ScheduleTemplateItemEntity>

    @Query("SELECT * FROM schedule_template_items WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(userId: String, id: String): ScheduleTemplateItemEntity?

    @Query("SELECT * FROM schedule_template_items WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): ScheduleTemplateItemEntity?

    @Query("SELECT * FROM schedule_template_items WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<ScheduleTemplateItemEntity>

    @Query("SELECT * FROM schedule_template_items WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<ScheduleTemplateItemEntity>

    @Query("UPDATE schedule_template_items SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ScheduleTemplateItemEntity)

    @Update
    suspend fun update(item: ScheduleTemplateItemEntity)

    @Query(
        """
        UPDATE schedule_template_items
        SET categoryId = :newCategoryId, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId
          AND categoryId = :oldCategoryId
          AND deletedAt IS NULL
        """
    )
    suspend fun replaceCategoryId(userId: String, oldCategoryId: String, newCategoryId: String, updatedAt: Long, syncStatus: Int)

    @Query(
        """
        UPDATE schedule_template_items
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE userId = :userId AND templateId = :templateId AND deletedAt IS NULL
        """
    )
    suspend fun softDeleteForTemplate(userId: String, templateId: String, deletedAt: Long, updatedAt: Long, syncStatus: Int)
}
