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
        val incomingUsername = usableName(username, email)
        val existingUsername = usableName(existing?.username, email ?: existing?.email)
        val resolvedUsername = incomingUsername ?: existingUsername

        if (existing != null) {
            val needsRemoteNameRecovery = incomingUsername == null && existingUsername == null
            val hasMeaningfulChange =
                (email != null && email != existing.email) ||
                    (incomingUsername != null && resolvedUsername != existing.username)
            val updated = existing.copy(
                email = email ?: existing.email,
                username = resolvedUsername,
                lastLoginAt = now,
                updatedAt = when {
                    needsRemoteNameRecovery -> 0L
                    hasMeaningfulChange -> now
                    else -> existing.updatedAt
                },
                syncStatus = when {
                    needsRemoteNameRecovery -> SyncStatus.SYNCED.raw
                    hasMeaningfulChange -> SyncStatus.UPDATED_LOCAL.raw
                    else -> existing.syncStatus
                }
            )
            userProfileDao.upsert(updated)
            syncTrigger.trigger()
            return userProfileDao.getById(uid)!!
        }

        val profile = UserProfileEntity(
            id = uid,
            email = email,
            username = resolvedUsername,
            createdAt = now,
            updatedAt = if (resolvedUsername != null) now else 0L,
            lastLoginAt = now,
            deletedAt = null,
            syncStatus = if (resolvedUsername != null) {
                SyncStatus.CREATED_LOCAL.raw
            } else {
                SyncStatus.SYNCED.raw
            }
        )
        userProfileDao.upsert(profile)
        syncTrigger.trigger()
        return profile
    }

    suspend fun getById(id: String): UserProfileEntity? = userProfileDao.getById(id)

    private fun usableName(value: String?, email: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalized = trimmed.lowercase()
        val normalizedEmail = email?.trim()?.lowercase()
        val emailPrefix = normalizedEmail?.substringBefore('@')

        if ('@' in normalized || normalized == normalizedEmail || normalized == emailPrefix) {
            return null
        }
        return trimmed
    }
}
