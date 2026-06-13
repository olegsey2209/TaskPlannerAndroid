package com.taskplanner.android.ui.components

import androidx.compose.ui.graphics.Color
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.ui.theme.AppColors

object PriorityStyle {
    fun dotColor(priorityRaw: Int): Color = when (priorityRaw) {
        TaskPriority.LOW.raw -> AppColors.PriorityLow
        TaskPriority.HIGH.raw -> AppColors.PriorityHigh
        else -> AppColors.PriorityMedium
    }

    fun cardBg(priorityRaw: Int): Color = when (priorityRaw) {
        TaskPriority.LOW.raw -> AppColors.TaskCardLowBg
        TaskPriority.HIGH.raw -> AppColors.TaskCardHighBg
        else -> AppColors.TaskCardMediumBg
    }

    fun label(priorityRaw: Int): String = when (priorityRaw) {
        TaskPriority.LOW.raw -> "Низкий"
        TaskPriority.HIGH.raw -> "Высокий"
        else -> "Средний"
    }
}
