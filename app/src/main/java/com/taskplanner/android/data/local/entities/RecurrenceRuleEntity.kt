package com.taskplanner.android.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurrence_rules",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["sourceTaskId"])
    ]
)
data class RecurrenceRuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val frequency: Int,
    val intervalValue: Int,
    val weekdaysMask: Int,
    val dayOfMonth: Int,
    val monthOfYear: Int,
    val sourceTaskId: String,
    val startDate: Long,
    val endDate: Long? = null,
    val lastGeneratedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: Int
)

