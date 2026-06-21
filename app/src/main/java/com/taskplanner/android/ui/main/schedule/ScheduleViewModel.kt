package com.taskplanner.android.ui.main.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.entities.RecurrenceRuleEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateItemEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.data.local.entities.TemplateApplicationEntity
import com.taskplanner.android.data.repository.RecurrenceRepository
import com.taskplanner.android.data.repository.TemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ScheduleSegment { TEMPLATES, RECURRENCE }

enum class RecurrenceSortOption(val title: String) {
    PRIORITY_DESC("Приоритет ↓"),
    PRIORITY_ASC("Приоритет ↑"),
    TITLE_ASC("По названию"),
    CREATED_DESC("Сначала новые")
}

data class RecurrenceSeriesUi(
    val rule: RecurrenceRuleEntity,
    val task: TaskEntity?
) {
    val isActive: Boolean
        get() {
            val end = rule.endDate
            return end == null || TimeUtils.localDateFromMillis(end).isAfter(LocalDate.now())
        }
}

class ScheduleViewModel(
    private val userId: String,
    private val templateRepository: TemplateRepository,
    private val recurrenceRepository: RecurrenceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _segment = MutableStateFlow(ScheduleSegment.TEMPLATES)
    val segment: StateFlow<ScheduleSegment> = _segment.asStateFlow()

    private val _recurrenceSort = MutableStateFlow(RecurrenceSortOption.PRIORITY_DESC)
    val recurrenceSort: StateFlow<RecurrenceSortOption> = _recurrenceSort.asStateFlow()

    val templates: StateFlow<List<ScheduleTemplateEntity>> =
        templateRepository.observeTemplates(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rules: StateFlow<List<RecurrenceRuleEntity>> =
        recurrenceRepository.observeRules(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val sourceTasksById = MutableStateFlow<Map<String, TaskEntity>>(emptyMap())

    val filteredTemplates: StateFlow<List<ScheduleTemplateEntity>> =
        combine(templates, searchQuery) { list, query ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@combine list
            list.filter { it.title.contains(trimmed, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recurrenceSeries: StateFlow<List<RecurrenceSeriesUi>> =
        combine(rules, sourceTasksById) { ruleList, tasksMap ->
            ruleList.map { rule ->
                RecurrenceSeriesUi(rule = rule, task = tasksMap[rule.sourceTaskId])
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredRecurrenceSeries: StateFlow<List<RecurrenceSeriesUi>> =
        combine(recurrenceSeries, searchQuery, recurrenceSort) { list, query, sort ->
            val trimmed = query.trim()
            val filtered = if (trimmed.isEmpty()) list else list.filter { (it.task?.title ?: "").contains(trimmed, ignoreCase = true) }
            when (sort) {
                RecurrenceSortOption.PRIORITY_DESC -> filtered.sortedWith(
                    compareByDescending<RecurrenceSeriesUi> { it.task?.priority ?: TaskPriority.MEDIUM.raw }
                        .thenByDescending { it.rule.createdAt }
                )
                RecurrenceSortOption.PRIORITY_ASC -> filtered.sortedWith(
                    compareBy<RecurrenceSeriesUi> { it.task?.priority ?: TaskPriority.MEDIUM.raw }
                        .thenByDescending { it.rule.createdAt }
                )
                RecurrenceSortOption.TITLE_ASC -> filtered.sortedBy { it.task?.title ?: "" }
                RecurrenceSortOption.CREATED_DESC -> filtered.sortedByDescending { it.rule.createdAt }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            recurrenceRepository.generateUpcomingTasksIfNeeded(userId)
        }
        viewModelScope.launch {
            rules.flatMapLatest { ruleList ->
                val ids = ruleList.map { it.sourceTaskId }.filter { it.isNotBlank() }.distinct()
                if (ids.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
                else recurrenceRepository.observeSourceTasks(userId, ids)
            }.collect { tasks ->
                sourceTasksById.value = tasks.associateBy { it.id }
            }
        }
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setSegment(value: ScheduleSegment) {
        _segment.value = value
    }

    fun applyRecurrenceSort(option: RecurrenceSortOption) {
        _recurrenceSort.value = option
    }

    fun createTemplate(title: String, description: String?) {
        viewModelScope.launch { templateRepository.createTemplate(userId, title, description) }
    }

    fun saveTemplateWithItems(
        templateId: String?,
        title: String,
        description: String?,
        items: List<TemplateRepository.TemplateItemInput>,
        onDone: (templateId: String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = templateRepository.upsertTemplateWithItems(
                userId = userId,
                templateId = templateId,
                title = title,
                description = description,
                items = items
            )
            onDone(id)
        }
    }

    fun observeTemplateItemCount(templateId: String): Flow<Int> {
        return templateRepository.observeItemCount(userId, templateId)
    }

    fun observeTemplateItems(templateId: String): Flow<List<ScheduleTemplateItemEntity>> {
        return templateRepository.observeItems(userId, templateId)
    }

    fun observeTemplateApplications(templateId: String): Flow<List<TemplateApplicationEntity>> {
        return templateRepository.observeApplications(userId, templateId)
    }

    fun observeTemplateApplicationTasks(applicationId: String): Flow<List<TaskEntity>> {
        return templateRepository.observeTasksForApplication(userId, applicationId)
    }

    fun updateTemplate(templateId: String, title: String, description: String?) {
        viewModelScope.launch { templateRepository.updateTemplate(userId, templateId, title, description) }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch { templateRepository.softDeleteTemplate(userId, templateId) }
    }

    fun applyTemplate(templateId: String, startDate: LocalDate, endDate: LocalDate, onApplied: (applicationId: String?) -> Unit = {}) {
        viewModelScope.launch {
            val id = templateRepository.applyTemplate(userId, templateId, startDate, endDate)
            onApplied(id)
        }
    }

    fun deleteTemplateApplication(applicationId: String) {
        viewModelScope.launch { templateRepository.deleteTemplateApplication(userId, applicationId) }
    }

    fun countTasksForApplication(applicationId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            onResult(templateRepository.countTasksForApplication(userId, applicationId))
        }
    }

    fun stopRecurrence(ruleId: String) {
        viewModelScope.launch { recurrenceRepository.stopRecurrence(userId, ruleId) }
    }

    fun resumeRecurrence(ruleId: String) {
        viewModelScope.launch { recurrenceRepository.resumeRecurrence(userId, ruleId) }
    }

    fun deleteRecurrence(ruleId: String) {
        viewModelScope.launch { recurrenceRepository.softDeleteRule(userId, ruleId) }
    }

    fun detachRecurrence(ruleId: String) {
        viewModelScope.launch { recurrenceRepository.deleteRecurrenceKeepingPast(userId, ruleId) }
    }

    fun createRecurrenceSeries(
        title: String,
        description: String?,
        startDate: LocalDate,
        startTimeMillis: Long?,
        priorityRaw: Int,
        categoryId: String?,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int,
        frequency: com.taskplanner.android.core.model.RecurrenceFrequency,
        interval: Int,
        weekdays: List<Int>?,
        endDate: LocalDate?
    ) {
        viewModelScope.launch {
            recurrenceRepository.createRecurrenceSeries(
                userId = userId,
                title = title,
                description = description,
                startDate = startDate,
                startTimeMillis = startTimeMillis,
                priority = priorityRaw,
                categoryId = categoryId,
                hasReminder = hasReminder,
                reminderOffsetMinutes = reminderOffsetMinutes,
                frequency = frequency,
                interval = interval,
                weekdays = weekdays,
                endDate = endDate
            )
        }
    }

    fun updateRecurrenceSeries(
        ruleId: String,
        title: String,
        description: String?,
        startTimeMillis: Long?,
        priorityRaw: Int,
        categoryId: String?,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int,
        frequency: com.taskplanner.android.core.model.RecurrenceFrequency,
        interval: Int,
        weekdays: List<Int>?,
        endDate: LocalDate?
    ) {
        viewModelScope.launch {
            recurrenceRepository.updateRecurrenceSeries(
                userId = userId,
                ruleId = ruleId,
                title = title,
                description = description,
                startTimeMillis = startTimeMillis,
                priority = priorityRaw,
                categoryId = categoryId,
                hasReminder = hasReminder,
                reminderOffsetMinutes = reminderOffsetMinutes,
                frequency = frequency,
                interval = interval,
                weekdays = weekdays,
                endDate = endDate
            )
        }
    }

    class Factory(
        private val userId: String,
        private val templateRepository: TemplateRepository,
        private val recurrenceRepository: RecurrenceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(userId, templateRepository, recurrenceRepository) as T
        }
    }
}
