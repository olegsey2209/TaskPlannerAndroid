package com.taskplanner.android.ui.main.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.data.local.entities.GoalEntity
import com.taskplanner.android.data.local.entities.GoalStepEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.data.repository.CategoryRepository
import com.taskplanner.android.data.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class GoalsViewModel(
    private val userId: String,
    private val goalRepository: GoalRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> =
        goalRepository.observeGoals(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories =
        categoryRepository.observeAll(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun stepsForGoal(goalId: String): Flow<List<GoalStepEntity>> {
        return goalRepository.observeStepsForGoal(userId, goalId)
    }

    fun linkedTaskForStep(stepId: String): Flow<TaskEntity?> {
        return goalRepository.observeLinkedTaskForStep(userId, stepId)
    }

    fun createGoal(title: String, description: String?) {
        viewModelScope.launch {
            goalRepository.createGoal(userId, title, description)
        }
    }

    fun updateGoal(goalId: String, title: String, description: String?) {
        viewModelScope.launch {
            goalRepository.updateGoal(userId, goalId, title, description)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.softDeleteGoal(userId, goalId)
        }
    }

    fun toggleStep(stepId: String) {
        viewModelScope.launch {
            goalRepository.toggleStepCompletion(userId, stepId)
        }
    }

    fun createStep(goalId: String, title: String, description: String?) {
        viewModelScope.launch {
            goalRepository.createStep(userId, goalId, title = title, description = description)
        }
    }

    fun updateStep(stepId: String, title: String, description: String?) {
        viewModelScope.launch {
            goalRepository.updateStep(userId, stepId, title, description)
        }
    }

    fun deleteStep(stepId: String) {
        viewModelScope.launch {
            goalRepository.softDeleteStep(userId, stepId)
        }
    }

    fun deleteTaskFromStep(stepId: String) {
        viewModelScope.launch {
            goalRepository.softDeleteTaskFromStep(userId, stepId)
        }
    }

    fun createTaskFromStepDetailed(
        stepId: String,
        description: String?,
        date: LocalDate,
        startTime: LocalTime?,
        priority: TaskPriority,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int,
        categoryId: String?,
        imageData: ByteArray?,
        onCreated: (taskId: String) -> Unit
    ) {
        viewModelScope.launch {
            val id = goalRepository.createTaskFromStepDetailed(
                userId = userId,
                stepId = stepId,
                description = description,
                date = date,
                startTime = startTime,
                priority = priority,
                hasReminder = hasReminder,
                reminderOffsetMinutes = reminderOffsetMinutes,
                categoryId = categoryId,
                imageData = imageData
            )
            if (id != null) onCreated(id)
        }
    }

    class Factory(
        private val userId: String,
        private val goalRepository: GoalRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalsViewModel(userId, goalRepository, categoryRepository) as T
        }
    }
}
