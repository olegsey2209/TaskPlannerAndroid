package com.taskplanner.android.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_applications",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["templateId"])
    ]
)
data class TemplateApplicationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val templateId: String,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean,
    val lastGeneratedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: Int
)

