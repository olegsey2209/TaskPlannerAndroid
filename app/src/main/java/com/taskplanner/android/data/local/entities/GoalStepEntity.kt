package com.taskplanner.android.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_steps",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["goalId"])
    ]
)
data class GoalStepEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalId: String,
    val title: String,
    val description: String? = null,
    val orderIndex: Int,
    val isCompleted: Boolean,
    val plannedDate: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: Int
)
