package com.taskplanner.android.data.repository

import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.data.local.dao.CategoryDao
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val syncTrigger: SyncTrigger
) {
    fun observeAll(userId: String): Flow<List<CategoryEntity>> {
        return categoryDao.observeAll(userId)
    }

    suspend fun createCategory(userId: String, name: String, iconName: String, colorHex: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return

        val existing = categoryDao.getByName(userId, normalizedName)
        if (existing != null) return

        val all = categoryDao.getAll(userId)
        val maxSortOrder = all.maxOfOrNull { it.sortOrder } ?: -1
        val now = System.currentTimeMillis()
        val category = CategoryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = normalizedName,
            iconName = iconName,
            colorHex = colorHex,
            sortOrder = maxSortOrder + 1,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        categoryDao.upsert(category)
        syncTrigger.trigger()
    }

    suspend fun updateCategory(userId: String, id: String, name: String, iconName: String, colorHex: String) {
        val existing = categoryDao.getById(userId, id) ?: return
        val now = System.currentTimeMillis()
        categoryDao.upsert(
            existing.copy(
                name = name.trim().ifEmpty { existing.name },
                iconName = iconName,
                colorHex = colorHex,
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun softDeleteCategory(userId: String, id: String) {
        val existing = categoryDao.getById(userId, id) ?: return
        val now = System.currentTimeMillis()
        categoryDao.upsert(
            existing.copy(
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.DELETED_LOCAL.raw
            )
        )
        syncTrigger.trigger()
    }

    suspend fun seedDefaultCategoriesIfEmpty(userId: String) {
        val existing = categoryDao.getAll(userId).filter { it.deletedAt == null }
        if (existing.isNotEmpty()) return

        val defaults = listOf(
            Triple("Личное", "person", "#007AFF"),
            Triple("Работа", "work", "#34C759"),
            Triple("Дом", "home", "#FF9500"),
            Triple("Учеба", "school", "#5856D6"),
            Triple("Здоровье", "favorite", "#FF2D55"),
            Triple("Спорт", "fitness_center", "#64D2FF"),
            Triple("Творчество", "palette", "#FF64D2")
        )

        val now = System.currentTimeMillis()
        defaults.forEachIndexed { index, item ->
            val category = CategoryEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = item.first,
                iconName = item.second,
                colorHex = item.third,
                sortOrder = index,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.CREATED_LOCAL.raw
            )
            categoryDao.upsert(category)
        }
        syncTrigger.trigger()
    }
}
