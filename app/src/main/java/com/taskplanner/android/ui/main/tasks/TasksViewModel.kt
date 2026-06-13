package com.taskplanner.android.ui.main.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.data.repository.CategoryRepository
import com.taskplanner.android.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class TasksViewModel(
    private val userId: String,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    enum class Filter { ALL, COMPLETED, INCOMPLETE }
    enum class Sort { CUSTOM, PRIORITY_DESC, PRIORITY_ASC, CATEGORY }

    private val filter = MutableStateFlow(Filter.ALL)
    private val sort = MutableStateFlow(Sort.CUSTOM)
    val currentSort: StateFlow<Sort> = sort
    val currentFilter: StateFlow<Filter> = filter

    
    private val _categoryFilter = MutableStateFlow<Set<String?>>(emptySet())
    val categoryFilter: StateFlow<Set<String?>> = _categoryFilter

    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.observeAll(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val baseTasks: StateFlow<List<TaskEntity>> = _selectedDate
        .flatMapLatest { date -> taskRepository.observeTasksForDate(userId, date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> =
        combine(baseTasks, filter, sort, _categoryFilter) { tasks, filter, sort, catFilter ->
            val filtered = when (filter) {
                Filter.ALL -> tasks
                Filter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED.raw }
                Filter.INCOMPLETE -> tasks.filter { it.status != TaskStatus.COMPLETED.raw }
            }
            val catFiltered = if (catFilter.isEmpty()) filtered
                else filtered.filter { it.categoryId in catFilter }
            when (sort) {
                Sort.CUSTOM -> catFiltered
                Sort.PRIORITY_DESC -> catFiltered.sortedWith(compareByDescending<TaskEntity> { it.priority }.thenBy { it.position }.thenByDescending { it.createdAt })
                Sort.PRIORITY_ASC -> catFiltered.sortedWith(compareBy<TaskEntity> { it.priority }.thenBy { it.position }.thenByDescending { it.createdAt })
                Sort.CATEGORY -> catFiltered.sortedWith(compareBy<TaskEntity> { it.categoryId ?: "zzz" }.thenBy { it.position })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    
    val calendarTasks: StateFlow<List<TaskEntity>> = _selectedDate
        .flatMapLatest { date ->
            taskRepository.observeTasksForMonth(userId, java.time.YearMonth.from(date))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    
    val searchQuery = MutableStateFlow("")
    private val searchFrom = MutableStateFlow(LocalDate.now().minusDays(30))
    private val searchToInclusive = MutableStateFlow(LocalDate.now())

    val searchPeriodEnabled = MutableStateFlow(false)

    val searchResults: StateFlow<List<TaskEntity>> =
        combine(searchQuery.debounce(250), searchFrom, searchToInclusive, searchPeriodEnabled) { q, from, toInc, periodOn ->
            Triple(q.trim(), if (periodOn) Pair(from, toInc) else null, periodOn)
        }
            .flatMapLatest { (q, period, _) ->
                kotlinx.coroutines.flow.flow {
                    if (q.isBlank()) { emit(emptyList()); return@flow }
                    if (period != null) {
                        val toExclusive = period.second.plusDays(1)
                        emit(taskRepository.searchTasksByTitleInRange(userId, q, period.first, toExclusive))
                    } else {
                        emit(taskRepository.searchAllTasks(userId, q))
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setFilter(value: Filter) {
        filter.value = value
    }

    fun setSort(value: Sort) {
        sort.value = value
    }

    fun toggleCategoryFilter(categoryId: String?) {
        val current = _categoryFilter.value.toMutableSet()
        if (current.contains(categoryId)) current.remove(categoryId) else current.add(categoryId)
        _categoryFilter.value = current
    }

    fun clearCategoryFilter() { _categoryFilter.value = emptySet() }

    fun setSearchActive(active: Boolean) {
        if (!active) searchQuery.value = ""
    }

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun updateSearchPeriod(from: LocalDate, toInclusive: LocalDate) {
        searchFrom.value = from
        searchToInclusive.value = toInclusive
    }

    fun currentSearchPeriod(): Pair<LocalDate, LocalDate> = searchFrom.value to searchToInclusive.value

    fun createTask(
        date: LocalDate,
        title: String,
        description: String?,
        imageData: ByteArray?,
        priority: TaskPriority,
        categoryId: String?,
        startTime: LocalTime?,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int
    ) {
        viewModelScope.launch {
            taskRepository.createTask(
                userId = userId,
                title = title,
                description = description,
                imageData = imageData,
                date = date,
                priority = priority,
                categoryId = categoryId,
                startTime = startTime,
                hasReminder = hasReminder,
                reminderOffsetMinutes = reminderOffsetMinutes
            )
        }
    }

    fun updateTask(
        taskId: String,
        date: LocalDate,
        title: String,
        description: String?,
        imageData: ByteArray?,
        removeImage: Boolean = false,
        priority: TaskPriority,
        categoryId: String?,
        startTime: LocalTime?,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int
    ) {
        viewModelScope.launch {
            taskRepository.updateTask(
                userId = userId,
                taskId = taskId,
                date = date,
                title = title,
                description = description,
                imageData = imageData,
                removeImage = removeImage,
                priority = priority,
                categoryId = categoryId,
                startTime = startTime,
                hasReminder = hasReminder,
                reminderOffsetMinutes = reminderOffsetMinutes
            )
        }
    }

    fun toggleCompletion(taskId: String) {
        viewModelScope.launch {
            taskRepository.toggleCompletion(userId, taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.softDeleteTask(userId, taskId)
        }
    }

    fun persistCustomOrder(orderedIds: List<String>) {
        viewModelScope.launch {
            taskRepository.persistCustomOrder(userId, orderedIds)
        }
    }

    fun createSubtask(parentTaskId: String, title: String) {
        viewModelScope.launch {
            taskRepository.createSubtask(userId, parentTaskId, title)
        }
    }

    fun observeSubtasks(parentTaskId: String) = taskRepository.observeSubtasks(userId, parentTaskId)

    fun observeTasksForMonth(yearMonth: java.time.YearMonth) =
        taskRepository.observeTasksForMonth(userId, yearMonth)

    suspend fun countSubtasks(parentTaskId: String) = taskRepository.countSubtasks(userId, parentTaskId)

    fun formatTaskTime(task: TaskEntity): String? {
        val millis = task.startTime ?: return null
        val localTime = TimeUtils.localTimeFromMillis(millis)
        return "%02d:%02d".format(localTime.hour, localTime.minute)
    }

    class Factory(
        private val userId: String,
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TasksViewModel(userId, taskRepository, categoryRepository) as T
        }
    }
}
