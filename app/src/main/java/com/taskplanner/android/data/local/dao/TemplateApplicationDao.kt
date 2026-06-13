package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskplanner.android.data.local.entities.TemplateApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateApplicationDao {
    @Query(
        """
        SELECT * FROM template_applications
        WHERE userId = :userId AND templateId = :templateId AND deletedAt IS NULL
        ORDER BY createdAt DESC
        """
    )
    fun observeForTemplate(userId: String, templateId: String): Flow<List<TemplateApplicationEntity>>

    @Query(
        """
        SELECT * FROM template_applications
        WHERE userId = :userId AND templateId = :templateId AND deletedAt IS NULL
        ORDER BY createdAt DESC
        """
    )
    suspend fun getForTemplate(userId: String, templateId: String): List<TemplateApplicationEntity>

    @Query("SELECT * FROM template_applications WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(userId: String, id: String): TemplateApplicationEntity?

    @Query("SELECT * FROM template_applications WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): TemplateApplicationEntity?

    @Query("SELECT * FROM template_applications WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<TemplateApplicationEntity>

    @Query("SELECT * FROM template_applications WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<TemplateApplicationEntity>

    @Query("UPDATE template_applications SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(application: TemplateApplicationEntity)

    @Update
    suspend fun update(application: TemplateApplicationEntity)

    @Query("DELETE FROM template_applications WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)
}
