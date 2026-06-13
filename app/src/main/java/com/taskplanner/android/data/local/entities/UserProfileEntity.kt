package com.taskplanner.android.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val email: String? = null,
    val username: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastLoginAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: Int
)

