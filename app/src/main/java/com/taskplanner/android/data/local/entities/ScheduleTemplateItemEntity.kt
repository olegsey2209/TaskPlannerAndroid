package com.taskplanner.android.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_template_items",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["templateId"]),
        Index(value = ["categoryId"])
    ]
)
data class ScheduleTemplateItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val templateId: String,
    val weekday: Int,
    val title: String,
    val description: String? = null,
    val startTime: Long? = null,
    val priority: Int,
    val hasReminder: Boolean,
    val reminderOffsetMinutes: Int,
    val categoryId: String? = null,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: Int
)

