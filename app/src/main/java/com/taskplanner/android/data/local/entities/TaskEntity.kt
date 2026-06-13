package com.taskplanner.android.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"]),
        Index(value = ["parentTaskId"]),
        Index(value = ["categoryId"]),
        Index(value = ["goalStepId"]),
        Index(value = ["recurrenceRuleId"]),
        Index(value = ["templateItemId"]),
        Index(value = ["templateApplicationId"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val notes: String? = null,
    val imageData: ByteArray? = null,
    val imageBase64: String? = null,
    val date: Long,
    val startTime: Long? = null,
    val priority: Int,
    val status: Int,
    val completedAt: Long? = null,
    val hasReminder: Boolean,
    val reminderOffsetMinutes: Int,
    val categoryId: String? = null,
    val parentTaskId: String? = null,
    val goalStepId: String? = null,
    val originType: Int,
    val instanceDate: Long? = null,
    val recurrenceRuleId: String? = null,
    val templateItemId: String? = null,
    val templateApplicationId: String? = null,
    val position: Int,
    val searchText: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: Int
)
