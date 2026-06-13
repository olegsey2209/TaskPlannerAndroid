package com.taskplanner.android.data.repository

import com.taskplanner.android.core.model.SyncStatus
import com.taskplanner.android.data.local.dao.UserProfileDao
import com.taskplanner.android.data.local.entities.UserProfileEntity
import com.taskplanner.android.sync.SyncTrigger

class UserRepository(
    private val userProfileDao: UserProfileDao,
    private val syncTrigger: SyncTrigger
) {
    suspend fun getOrCreateUser(uid: String, email: String?, username: String? = null): UserProfileEntity {
        val existing = userProfileDao.getById(uid)
        val now = System.currentTimeMillis()
        if (existing != null) {
            val updated = existing.copy(
                email = email ?: existing.email,
                username = username ?: existing.username,
                lastLoginAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.UPDATED_LOCAL.raw
            )
            userProfileDao.upsert(updated)
            syncTrigger.trigger()
            return userProfileDao.getById(uid)!!
        }

        val profile = UserProfileEntity(
            id = uid,
            email = email,
            username = username,
            createdAt = now,
            updatedAt = now,
            lastLoginAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.CREATED_LOCAL.raw
        )
        userProfileDao.upsert(profile)
        syncTrigger.trigger()
        return profile
    }

    suspend fun getById(id: String): UserProfileEntity? = userProfileDao.getById(id)
}
