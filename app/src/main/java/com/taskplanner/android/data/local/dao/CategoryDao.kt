package com.taskplanner.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskplanner.android.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeAll(userId: String): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM categories
        WHERE userId = :userId
          AND (
            deletedAt IS NULL
            OR EXISTS (
                SELECT 1 FROM tasks
                WHERE tasks.userId = :userId
                  AND tasks.categoryId = categories.id
                  AND tasks.deletedAt IS NULL
            )
          )
        ORDER BY sortOrder ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeForTaskFilter(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getAll(userId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeAllForUser(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id AND userId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(userId: String, id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdAny(userId: String, id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE userId = :userId AND deletedAt IS NULL AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(userId: String, name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE userId = :userId AND deletedAt IS NOT NULL AND LOWER(name) = LOWER(:name) ORDER BY deletedAt DESC LIMIT 1")
    suspend fun getDeletedByName(userId: String, name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE userId = :userId AND syncStatus != :syncedRaw")
    suspend fun getUnsynced(userId: String, syncedRaw: Int): List<CategoryEntity>

    @Query("UPDATE categories SET syncStatus = :syncedRaw WHERE id = :id AND userId = :userId")
    suspend fun markSynced(userId: String, id: String, syncedRaw: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)
}
