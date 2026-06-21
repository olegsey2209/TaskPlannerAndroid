package com.taskplanner.android.ui.main.screens

import com.taskplanner.android.ui.theme.AppColors
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import com.taskplanner.android.core.model.RecurrenceFrequency
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.data.local.entities.RecurrenceRuleEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateItemEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.data.local.entities.TemplateApplicationEntity
import com.taskplanner.android.ui.LocalAppGraph
import com.taskplanner.android.ui.main.schedule.RecurrenceSortOption
import kotlinx.coroutines.flow.collect
import com.taskplanner.android.ui.main.schedule.ScheduleSegment
import com.taskplanner.android.ui.main.schedule.ScheduleViewModel
import com.taskplanner.android.ui.main.schedule.RecurrenceSeriesUi
import androidx.compose.runtime.toMutableStateList
import com.taskplanner.android.data.repository.TemplateRepository
import androidx.compose.ui.platform.LocalContext
import android.app.TimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScheduleScreen(padding: PaddingValues, userId: String) {
    val graph = LocalAppGraph.current
    val vm: ScheduleViewModel = viewModel(
        key = "schedule-$userId",
        factory = ScheduleViewModel.Factory(userId, graph.templateRepository, graph.recurrenceRepository)
    )

    val segment by vm.segment.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val recurrenceSort by vm.recurrenceSort.collectAsState()

    val templates by vm.filteredTemplates.collectAsState()
    val recurrence by vm.filteredRecurrenceSeries.collectAsState()
    val categories by graph.categoryRepository.observeAll(userId).collectAsState(initial = emptyList())
    val allCategories by graph.categoryRepository.observeAllIncludingDeleted(userId).collectAsState(initial = emptyList())

    var showTemplateEditor by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<ScheduleTemplateEntity?>(null) }
    var showApplyTemplate by remember { mutableStateOf<ScheduleTemplateEntity?>(null) }
    var showTemplateApplications by remember { mutableStateOf<ScheduleTemplateEntity?>(null) }
    var showRecurrenceEditor by remember { mutableStateOf(false) }
    var editingRuleUi by remember { mutableStateOf<RecurrenceSeriesUi?>(null) }

    var deleteTemplateConfirm by remember { mutableStateOf<ScheduleTemplateEntity?>(null) }

    var pendingStop by remember { mutableStateOf<RecurrenceRuleEntity?>(null) }
    var pendingDetach by remember { mutableStateOf<RecurrenceRuleEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                segment = segment,
                onSegmentChange = vm::setSegment,
                query = query,
                onQueryChange = vm::setSearchQuery,
                showSort = segment == ScheduleSegment.RECURRENCE,
                sortOption = recurrenceSort,
                onSortSelected = vm::applyRecurrenceSort
            )

            if (segment == ScheduleSegment.TEMPLATES) {
                if (templates.isEmpty()) {
                    EmptyTemplatesState(onCreate = {
                        editingTemplate = null
                        showTemplateEditor = true
                    })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(templates, key = { it.id }) { template ->
                            val count by vm.observeTemplateItemCount(template.id).collectAsState(initial = 0)
                            TemplateCard(
                                template = template,
                                itemCount = count,
                                itemsFlow = { vm.observeTemplateItems(template.id) },
                                onApply = { showApplyTemplate = template },
                                onApplications = { showTemplateApplications = template },
                                onEdit = {
                                    editingTemplate = template
                                    showTemplateEditor = true
                                },
                                onDelete = { deleteTemplateConfirm = template }
                            )
                        }
                    }
                }
            } else {
                if (recurrence.isEmpty()) {
                    EmptyRecurrenceState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recurrence, key = { it.rule.id }) { item ->
                            RecurrenceCard(
                                task = item.task,
                                rule = item.rule,
                                isActive = item.isActive,
                                categories = allCategories,
                                onEdit = { editingRuleUi = item; showRecurrenceEditor = true },
                                onRequestStop = { pendingStop = item.rule },
                                onResume = { vm.resumeRecurrence(item.rule.id) },
                                onDetachRule = { pendingDetach = item.rule }
                            )
                        }
                    }
                }
            }
        }

        FloatingAddButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            onClick = {
                when (segment) {
                    ScheduleSegment.TEMPLATES -> {
                        editingTemplate = null
                        showTemplateEditor = true
                    }
                    ScheduleSegment.RECURRENCE -> {
                        showRecurrenceEditor = true
                    }
                }
            }
        )
    }

    if (showTemplateEditor) {
        val templateBeingEdited = editingTemplate
        if (templateBeingEdited == null) {
            TemplateEditorBottomSheet(
                template = null,
                initialItems = emptyList(),
                categories = categories,
                onDismiss = { showTemplateEditor = false },
                onSave = { title, description, items ->
                    vm.saveTemplateWithItems(
                        templateId = null,
                        title = title,
                        description = description,
                        items = items
                    )
                    showTemplateEditor = false
                }
            )
        } else {
            val loadedItems by produceState<List<ScheduleTemplateItemEntity>?>(
                initialValue = null,
                key1 = templateBeingEdited.id
            ) {
                vm.observeTemplateItems(templateBeingEdited.id).collect { value = it }
            }
            loadedItems?.let { items ->
                val editorKey = items.joinToString("|", prefix = templateBeingEdited.id) {
                    "${it.id}:${it.updatedAt}:${it.title}:${it.description.orEmpty()}"
                }
                androidx.compose.runtime.key(editorKey) {
                    TemplateEditorBottomSheet(
                        template = templateBeingEdited,
                        initialItems = items,
                        categories = categories,
                        onDismiss = { showTemplateEditor = false },
                        onSave = { title, description, editedItems ->
                            vm.saveTemplateWithItems(
                                templateId = templateBeingEdited.id,
                                title = title,
                                description = description,
                                items = editedItems
                            )
                            showTemplateEditor = false
                        }
                    )
                }
            }
        }
    }

    showApplyTemplate?.let { template ->
        val items = vm.observeTemplateItems(template.id).collectAsState(initial = emptyList()).value
        val existingApps = vm.observeTemplateApplications(template.id).collectAsState(initial = emptyList()).value
        ApplyTemplateBottomSheet(
            template = template,
            items = items,
            categories = categories,
            existingApplications = existingApps,
            onDismiss = { showApplyTemplate = null },
            onApply = { start, end, onApplied ->
                vm.applyTemplate(template.id, start, end) { applicationId ->
                    onApplied(applicationId)
                }
            },
            onUndo = { applicationId ->
                vm.deleteTemplateApplication(applicationId)
            }
        )
    }

    showTemplateApplications?.let { template ->
        TemplateApplicationsBottomSheet(
            onDismiss = { showTemplateApplications = null },
            applicationsFlow = { vm.observeTemplateApplications(template.id) },
            tasksFlow = { applicationId -> vm.observeTemplateApplicationTasks(applicationId) },
            categories = allCategories,
            loadTaskCount = { applicationId -> graph.templateRepository.countTasksForApplication(userId, applicationId) },
            onDeleteApplication = { applicationId -> vm.deleteTemplateApplication(applicationId) }
        )
    }

    if (showRecurrenceEditor) {
        RecurrenceEditorBottomSheet(
            categories = categories,
            existing = editingRuleUi,
            onDismiss = { showRecurrenceEditor = false; editingRuleUi = null },
            onSave = { payload ->
                val editing = editingRuleUi
                if (editing != null) {
                    vm.updateRecurrenceSeries(
                        ruleId = editing.rule.id,
                        title = payload.title,
                        description = payload.description,
                        startTimeMillis = payload.startTimeMillis,
                        priorityRaw = payload.priorityRaw,
                        categoryId = payload.categoryId,
                        hasReminder = payload.hasReminder,
                        reminderOffsetMinutes = payload.reminderOffsetMinutes,
                        frequency = payload.frequency,
                        interval = payload.interval,
                        weekdays = payload.weekdays,
                        endDate = payload.endDate
                    )
                } else {
                    vm.createRecurrenceSeries(
                        title = payload.title,
                        description = payload.description,
                        startDate = payload.startDate,
                        startTimeMillis = payload.startTimeMillis,
                        priorityRaw = payload.priorityRaw,
                        categoryId = payload.categoryId,
                        hasReminder = payload.hasReminder,
                        reminderOffsetMinutes = payload.reminderOffsetMinutes,
                        frequency = payload.frequency,
                        interval = payload.interval,
                        weekdays = payload.weekdays,
                        endDate = payload.endDate
                    )
                }
                showRecurrenceEditor = false
                editingRuleUi = null
            }
        )
    }

    deleteTemplateConfirm?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteTemplateConfirm = null },
            title = { Text("Удалить шаблон?") },
            text = { Text("Шаблон и все его элементы будут удалены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteTemplate(template.id)
                        deleteTemplateConfirm = null
                    }
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTemplateConfirm = null }) { Text("Отмена") } }
        )
    }

    pendingStop?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingStop = null },
            title = { Text("Остановить повтор?") },
            text = { Text("Правило перестанет генерировать новые задачи.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.stopRecurrence(rule.id)
                        pendingStop = null
                    }
                ) { Text("Остановить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingStop = null }) { Text("Отмена") } }
        )
    }

    pendingDetach?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDetach = null },
            title = { Text("Удалить повтор?") },
            text = { Text("Правило повтора будет удалено. Будущие задачи серии будут удалены, прошлые останутся.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.detachRecurrence(rule.id)
                        pendingDetach = null
                    }
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDetach = null }) { Text("Отмена") } }
        )
    }

}

@Composable
private fun Header(
    segment: ScheduleSegment,
    onSegmentChange: (ScheduleSegment) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    showSort: Boolean,
    sortOption: RecurrenceSortOption,
    onSortSelected: (RecurrenceSortOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = "Расписание",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = com.taskplanner.android.ui.theme.AppColors.Label,
                modifier = Modifier.align(Alignment.Center)
            )
            if (showSort) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.Blue.copy(alpha = 0.12f))
                            .clickable {
                                onSortSelected(
                                    if (sortOption == RecurrenceSortOption.PRIORITY_DESC)
                                        RecurrenceSortOption.PRIORITY_ASC
                                    else RecurrenceSortOption.PRIORITY_DESC
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (sortOption == RecurrenceSortOption.PRIORITY_ASC)
                                Icons.Filled.KeyboardArrowUp
                            else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Сортировка по приоритету",
                            tint = AppColors.Blue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.SystemGroupedBackground)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = com.taskplanner.android.ui.theme.AppColors.GrayText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Поиск", color = com.taskplanner.android.ui.theme.AppColors.GrayText, fontSize = 16.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = com.taskplanner.android.ui.theme.AppColors.Label, fontSize = 16.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(com.taskplanner.android.ui.theme.AppColors.Blue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        SegmentControl(selected = segment, onSelected = onSegmentChange)
    }
}

@Composable
private fun SegmentControl(selected: ScheduleSegment, onSelected: (ScheduleSegment) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(32.dp)
            .clip(shape)
            .background(AppColors.SystemGroupedBackground)
            .padding(2.dp)
    ) {
        SegmentButton(
            title = "Шаблоны недели",
            selected = selected == ScheduleSegment.TEMPLATES,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ScheduleSegment.TEMPLATES) }
        )
        SegmentButton(
            title = "Повторяющиеся задачи",
            selected = selected == ScheduleSegment.RECURRENCE,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(ScheduleSegment.RECURRENCE) }
        )
    }
}

@Composable
private fun SegmentButton(title: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(7.dp))
            .then(
                if (selected) Modifier
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(7.dp))
                    .background(Color.White)
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = com.taskplanner.android.ui.theme.AppColors.Label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun TemplateCard(
    template: ScheduleTemplateEntity,
    itemCount: Int,
    itemsFlow: () -> kotlinx.coroutines.flow.Flow<List<ScheduleTemplateItemEntity>>,
    onApply: () -> Unit,
    onApplications: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember(template.id) { mutableStateOf(false) }
    var menuExpanded by remember(template.id) { mutableStateOf(false) }

    
    val loadedItems by itemsFlow().collectAsState(initial = emptyList())
    val allItems = remember(loadedItems) {
        loadedItems
            .filter { it.deletedAt == null }
            .sortedWith(templateItemComparator())
    }

    val items = if (expanded) allItems else emptyList()

    val stripeColors = remember(allItems) {
        val priorities = allItems.map {
            when (it.priority) {
                TaskPriority.HIGH.raw -> AppColors.Red
                TaskPriority.LOW.raw -> AppColors.Green
                else -> AppColors.Orange
            }
        }.distinct()
        when {
            priorities.isEmpty() -> listOf(AppColors.Orange, AppColors.Orange)
            priorities.size == 1 -> listOf(priorities[0], priorities[0])
            else -> priorities
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = stripeColors
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
                ) {
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            template.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!template.description.isNullOrBlank()) {
                            Text(
                                template.description!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.GrayText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (itemCount > 0) {
                            Text(
                                text = "Задач в шаблоне: $itemCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.GrayText
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppColors.Blue)
                            .clickable(onClick = onApply),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Применить",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                
                AnimatedVisibility(
                    visible = expanded && items.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        items.forEach { item -> TemplateItemRow(item) }
                    }
                }

                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            if (expanded) "Скрыть" else "Показать",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Blue
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = AppColors.Blue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = "Меню", tint = AppColors.GrayText, modifier = Modifier.size(20.dp))
                        }
                        IosDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            IosMenuItem("Редактировать", Icons.Filled.Edit, AppColors.Blue) { menuExpanded = false; onEdit() }
                            IosMenuItem("Запуски шаблона", Icons.Filled.History, AppColors.Blue) { menuExpanded = false; onApplications() }
                            IosMenuDivider()
                            IosMenuItem("Удалить", Icons.Filled.Delete, AppColors.Red) { menuExpanded = false; onDelete() }
                        }
                    }
                }
            }
            } 
        } 
    } 
}

private fun templateItemComparator(): Comparator<ScheduleTemplateItemEntity> =
    compareBy<ScheduleTemplateItemEntity> { it.weekday }
        .thenBy { it.startTime ?: Long.MAX_VALUE }
        .thenBy { it.position }
        .thenBy { it.title }

@Composable
private fun TemplateItemRow(item: ScheduleTemplateItemEntity) {
    val weekday = weekdayShort(item.weekday)
    val timeText = item.startTime?.let { millis ->
        val t = TimeUtils.localTimeFromMillis(millis)
        "%02d:%02d".format(t.hour, t.minute)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(weekday, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(priorityColor(item.priority))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (timeText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecurrenceCard(
    task: TaskEntity?,
    rule: RecurrenceRuleEntity,
    isActive: Boolean,
    categories: List<CategoryEntity>,
    onEdit: () -> Unit,
    onRequestStop: () -> Unit,
    onResume: () -> Unit,
    onDetachRule: () -> Unit,
) {
    var menuExpanded by remember(rule.id) { mutableStateOf(false) }
    val title = task?.title ?: "Без названия"
    val category = task?.categoryId?.let { id -> categories.firstOrNull { it.id == id } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(16.dp)
        ) {
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(priorityColor(task?.priority ?: TaskPriority.MEDIUM.raw).copy(alpha = if (isActive) 1f else 0.55f))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (category != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = categoryIcon(category.iconName ?: "") ?: Icons.Filled.Label,
                            contentDescription = null,
                            tint = colorFromHex(category.colorHex ?: "#007AFF"),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (category != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colorFromHex(category.colorHex ?: "#007AFF").copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = category.name,
                            fontSize = 11.sp,
                            color = colorFromHex(category.colorHex ?: "#007AFF"),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    recurrenceSummary(rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.GrayText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Старт: ${TimeUtils.localDateFromMillis(rule.startDate).formatRussianShort()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.GrayText,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    rule.endDate?.let { "Окончание: ${TimeUtils.localDateFromMillis(it).formatRussianShort()}" } ?: "Без окончания",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.GrayText
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(active = isActive)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = "Меню", tint = AppColors.GrayText)
                    }
                    IosDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        IosMenuItem("Редактировать", Icons.Filled.Edit, AppColors.Blue) { menuExpanded = false; onEdit() }
                        IosMenuDivider()
                        if (isActive) {
                            IosMenuItem("Остановить повтор", Icons.Filled.Stop, AppColors.Label) { menuExpanded = false; onRequestStop() }
                        } else {
                            IosMenuItem("Возобновить повтор", Icons.Filled.PlayArrow, AppColors.Green) { menuExpanded = false; onResume() }
                        }
                        IosMenuDivider()
                        IosMenuItem("Удалить повтор", Icons.Filled.Delete, AppColors.Red) { menuExpanded = false; onDetachRule() }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(active: Boolean) {
    val bg = if (active) Color(0xFF0A84FF) else Color.Gray.copy(alpha = 0.55f)
    val text = if (active) "Активно" else "Не активно"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyTemplatesState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray.copy(alpha = 0.35f), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Нет шаблонов", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Создайте шаблон для быстрого планирования", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.75f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(18.dp))
        TextButton(onClick = onCreate) { Text("Создать шаблон", color = Color(0xFF0A84FF)) }
    }
}

@Composable
private fun EmptyRecurrenceState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Repeat, contentDescription = null, tint = Color.Gray.copy(alpha = 0.35f), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Нет повторяющихся задач", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Создайте правило для автоматического добавления задач", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.75f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun FloatingAddButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(com.taskplanner.android.ui.theme.FabGradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TemplateEditorBottomSheet(
    template: ScheduleTemplateEntity?,
    initialItems: List<ScheduleTemplateItemEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?, items: List<TemplateRepository.TemplateItemInput>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(template?.id, template?.updatedAt) { mutableStateOf(template?.title.orEmpty()) }
    var description by remember(template?.id, template?.updatedAt) { mutableStateOf(template?.description.orEmpty()) }
    val activeInitialItems = initialItems
        .filter { it.deletedAt == null }
        .sortedWith(templateItemComparator())
    val initialItemsKey = activeInitialItems.joinToString("|") { "${it.id}:${it.updatedAt}:${it.deletedAt ?: 0L}" }
    val itemStates = remember(template?.id, initialItemsKey) {
        mutableStateListOf<TemplateItemUi>().apply {
            if (template == null) {
                add(TemplateItemUi())
            } else {
                addAll(
                    activeInitialItems.map { entity ->
                        TemplateItemUi().apply {
                            id = entity.id
                            weekday = entity.weekday
                            this.title = entity.title
                            this.description = entity.description ?: ""
                            hasTime = entity.startTime != null
                            startTime = entity.startTime?.let { TimeUtils.localTimeFromMillis(it) }
                            priority = when (entity.priority) {
                                TaskPriority.LOW.raw -> TaskPriority.LOW
                                TaskPriority.HIGH.raw -> TaskPriority.HIGH
                                else -> TaskPriority.MEDIUM
                            }
                            hasReminder = entity.hasReminder
                            reminderOffsetMinutes = entity.reminderOffsetMinutes
                            categoryId = entity.categoryId
                        }
                    }
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        dragHandle = null,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp).statusBarsPadding()) {
            SheetHeader(
                title = if (template == null) "Новый шаблон" else "Редактировать шаблон",
                confirmText = "Сохранить",
                confirmEnabled = title.trim().isNotEmpty() && itemStates.isNotEmpty() && itemStates.all { it.title.trim().isNotEmpty() },
                onCancel = onDismiss,
                onConfirm = {
                    val inputs = itemStates.map { ui ->
                        TemplateRepository.TemplateItemInput(
                            weekday = ui.weekday,
                            title = ui.title.trim(),
                            description = ui.description.trim().ifBlank { null },
                            startTime = if (ui.hasTime) ui.startTime ?: LocalTime.of(9, 0) else null,
                            priority = ui.priority.raw,
                            hasReminder = ui.hasReminder && ui.hasTime,
                            reminderOffsetMinutes = ui.reminderOffsetMinutes,
                            categoryId = ui.categoryId
                        )
                    }
                    onSave(title.trim(), description.trim().ifBlank { null }, inputs)
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                SectionCard(title = "Основное") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название шаблона") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IosThinDivider()
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание шаблона") },
                        singleLine = false,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "ЗАДАЧИ В ШАБЛОНЕ",
                        fontSize = 13.sp,
                        color = AppColors.GrayText,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    itemStates.forEachIndexed { index, item ->
                        androidx.compose.runtime.key(item.id) {
                            TemplateItemEditorCard(
                                item = item,
                                categories = categories,
                                canRemove = itemStates.size > 1,
                                onRemove = { itemStates.removeAt(index) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(14.dp))
                            .clickable { itemStates.add(TemplateItemUi()) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.Blue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить задачу", color = AppColors.Blue, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private class TemplateItemUi {
    var id: String = UUID.randomUUID().toString()
    var weekday by mutableIntStateOf(1)
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var hasTime by mutableStateOf(false)
    var startTime by mutableStateOf<LocalTime?>(null)
    var priority by mutableStateOf(TaskPriority.MEDIUM)
    var hasReminder by mutableStateOf(false)
    var reminderOffsetMinutes by mutableIntStateOf(15)
    var categoryId by mutableStateOf<String?>(null)
}

@Composable
private fun TemplateItemEditorCard(
    item: TemplateItemUi,
    categories: List<CategoryEntity>,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var weekdayMenu by remember(item.id) { mutableStateOf(false) }
    var categoryMenu by remember(item.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("День недели", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
            Box {
                Row(
                    modifier = Modifier.clickable { weekdayMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(weekdayLong(item.weekday), color = AppColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Filled.ExpandMore, null, tint = AppColors.Blue, modifier = Modifier.size(18.dp))
                }
                IosDropdownMenu(expanded = weekdayMenu, onDismissRequest = { weekdayMenu = false }) {
                    (1..7).forEach { day ->
                        IosMenuItem(weekdayLong(day), Icons.Filled.CalendarToday,
                            if (item.weekday == day) AppColors.Blue else AppColors.Label
                        ) { item.weekday = day; weekdayMenu = false }
                    }
                }
            }
        }
        IosThinDivider()
        
        OutlinedTextField(
            value = item.title,
            onValueChange = { item.title = it },
            label = { Text("Название задачи") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )
        IosThinDivider()
        
        OutlinedTextField(
            value = item.description,
            onValueChange = { item.description = it },
            label = { Text("Описание") },
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )
        IosThinDivider()
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Категория", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
            Box {
                Row(modifier = Modifier.clickable { categoryMenu = true }, verticalAlignment = Alignment.CenterVertically) {
                    val catName = item.categoryId?.let { id -> categories.firstOrNull { it.id == id }?.name } ?: "Без категории"
                    Text(catName, color = AppColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Filled.ExpandMore, null, tint = AppColors.Blue, modifier = Modifier.size(16.dp))
                }
                IosDropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    IosMenuItem("Без категории", Icons.Filled.Label, AppColors.GrayText) { item.categoryId = null; categoryMenu = false }
                    if (categories.isNotEmpty()) IosMenuDivider()
                    categories.forEach { cat ->
                        IosMenuItem(cat.name, categoryIcon(cat.iconName ?: "") ?: Icons.Filled.Label, colorFromHex(cat.colorHex ?: "#007AFF")) { item.categoryId = cat.id; categoryMenu = false }
                    }
                }
            }
        }
        IosThinDivider()
        
        Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Приоритет", fontSize = 16.sp, color = AppColors.Label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TaskPriority.LOW to "Низкий", TaskPriority.MEDIUM to "Средний", TaskPriority.HIGH to "Высокий").forEach { (p, label) ->
                    val color = priorityColor(p.raw)
                    val selected = item.priority == p
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) color.copy(alpha = 0.22f) else Color.Transparent)
                            .border(BorderStroke(1.dp, if (selected) color else Color(0xFFE0E0E0)), RoundedCornerShape(10.dp))
                            .clickable { item.priority = p }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) color else AppColors.GrayText, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
        IosThinDivider()
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Указать время", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
            androidx.compose.material3.Switch(checked = item.hasTime, onCheckedChange = { checked: Boolean ->
                item.hasTime = checked
                if (!checked) { item.startTime = null; item.hasReminder = false }
                else if (item.startTime == null) item.startTime = LocalTime.of(9, 0)
            }, colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA), uncheckedBorderColor = Color.Transparent))
        }
        if (item.hasTime) {
            IosThinDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val current = item.startTime ?: LocalTime.of(9, 0)
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> item.startTime = LocalTime.of(hour, minute) },
                            current.hour,
                            current.minute,
                            true
                        ).show()
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Время начала", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                Text(
                    "%02d:%02d".format((item.startTime ?: LocalTime.of(9, 0)).hour, (item.startTime ?: LocalTime.of(9, 0)).minute),
                    color = AppColors.Blue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IosThinDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Напоминание", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                androidx.compose.material3.Switch(checked = item.hasReminder, onCheckedChange = { v: Boolean -> item.hasReminder = v }, enabled = item.startTime != null,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA), uncheckedBorderColor = Color.Transparent))
            }
            if (item.hasReminder) {
                ReminderOffsetChips(
                    selected = item.reminderOffsetMinutes,
                    onSelected = { item.reminderOffsetMinutes = it }
                )
            }
        }
        if (canRemove) {
            IosThinDivider()
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onRemove).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Delete, null, tint = AppColors.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Удалить задачу", color = AppColors.Red, fontSize = 16.sp)
            }
        }
    }
}

private data class RecurrenceEditorPayload(
    val title: String,
    val description: String?,
    val startDate: LocalDate,
    val startTimeMillis: Long?,
    val priorityRaw: Int,
    val categoryId: String?,
    val hasReminder: Boolean,
    val reminderOffsetMinutes: Int,
    val frequency: com.taskplanner.android.core.model.RecurrenceFrequency,
    val interval: Int,
    val weekdays: List<Int>?,
    val endDate: LocalDate?
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecurrenceEditorBottomSheet(
    categories: List<CategoryEntity>,
    existing: RecurrenceSeriesUi? = null,
    onDismiss: () -> Unit,
    onSave: (RecurrenceEditorPayload) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val existingTask = existing?.task
    val existingRule = existing?.rule
    val existingStartMillis = existingTask?.startTime

    var title by remember(existing?.rule?.id) { mutableStateOf(existingTask?.title ?: "") }
    var description by remember(existing?.rule?.id) { mutableStateOf(existingTask?.description ?: "") }
    var startDate by remember(existing?.rule?.id) {
        mutableStateOf(existingRule?.let { rule -> TimeUtils.localDateFromMillis(rule.startDate) } ?: LocalDate.now())
    }

    var hasTime by remember(existing?.rule?.id) { mutableStateOf(existingStartMillis != null) }
    var startTime by remember(existing?.rule?.id) {
        mutableStateOf(existingStartMillis?.let { ms -> TimeUtils.localTimeFromMillis(ms) } ?: LocalTime.of(9, 0))
    }

    var priority by remember(existing?.rule?.id) {
        mutableStateOf(existingTask?.let { task -> TaskPriority.entries.firstOrNull { p -> p.raw == task.priority } } ?: TaskPriority.MEDIUM)
    }
    var categoryId by remember(existing?.rule?.id) { mutableStateOf<String?>(existingTask?.categoryId) }

    var hasReminder by remember(existing?.rule?.id) { mutableStateOf(existingTask?.hasReminder ?: false) }
    var reminderOffsetMinutes by remember(existing?.rule?.id) { mutableIntStateOf(existingTask?.reminderOffsetMinutes ?: 15) }

    var frequency by remember(existing?.rule?.id) {
        mutableStateOf(existingRule?.let { rule -> com.taskplanner.android.core.model.RecurrenceFrequency.entries.firstOrNull { f -> f.raw == rule.frequency.toInt() } }
            ?: com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY)
    }
    var interval by remember(existing?.rule?.id) { mutableIntStateOf(existingRule?.intervalValue?.toInt() ?: 1) }
    val selectedWeekdays = remember(existing?.rule?.id) {
        val mask = existingRule?.weekdaysMask?.toInt() ?: 0
        val days = (1..7).filter { (mask and (1 shl it)) != 0 }.toMutableStateList()
        if (days.isEmpty()) mutableStateListOf<Int>() else days
    }

    var hasEndDate by remember(existing?.rule?.id) { mutableStateOf(existingRule?.endDate != null) }
    var endDate by remember(existing?.rule?.id) {
        mutableStateOf(existingRule?.endDate?.let { ms -> TimeUtils.localDateFromMillis(ms) } ?: LocalDate.now())
    }

    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    var categoryMenu by remember { mutableStateOf(false) }
    var frequencyMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Добавляем день недели только при СОЗДАНИИ нового правила
        if (existing == null) {
            val wd = startDate.dayOfWeek.value
            if (!selectedWeekdays.contains(wd)) selectedWeekdays.add(wd)
        }
    }

    val canSave = title.trim().isNotEmpty() && (frequency != com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY || selectedWeekdays.isNotEmpty())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        dragHandle = null,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).statusBarsPadding()) {
            SheetHeader(
                title = if (existing != null) "Редактировать повтор" else "Новый повтор",
                confirmText = "Сохранить",
                confirmEnabled = canSave,
                onCancel = onDismiss,
                onConfirm = {
                    val startTimeMillis = if (hasTime) TimeUtils.millisFromLocalTime(startTime, startDate) else null
                    onSave(
                        RecurrenceEditorPayload(
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            startDate = startDate,
                            startTimeMillis = startTimeMillis,
                            priorityRaw = priority.raw,
                            categoryId = categoryId,
                            hasReminder = hasReminder && hasTime,
                            reminderOffsetMinutes = reminderOffsetMinutes,
                            frequency = frequency,
                            interval = maxOf(1, interval),
                            weekdays = if (frequency == com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY) selectedWeekdays.sorted() else null,
                            endDate = if (hasEndDate) endDate else null
                        )
                    )
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                SectionCard(title = "Задача") {
                    IosField(value = title, onValueChange = { title = it }, placeholder = "Название")
                    IosThinDivider()
                    IosField(value = description, onValueChange = { description = it }, placeholder = "Описание (необязательно)", singleLine = false, minLines = 2)
                }

                
                SectionCard(title = "Дата и время") {
                    Row(modifier = Modifier.fillMaxWidth().clickable { pickingStart = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Дата начала", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        Text(startDate.formatRussianLong(), fontSize = 16.sp, color = AppColors.Blue, fontWeight = FontWeight.Medium)
                    }
                    IosThinDivider()
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Указать время", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(checked = hasTime, onCheckedChange = { v: Boolean -> hasTime = v; if (!v) hasReminder = false },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA), uncheckedBorderColor = Color.Transparent))
                    }
                    if (hasTime) {
                        IosThinDivider()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                            TimePickerDialog(context, { _, h, m -> startTime = LocalTime.of(h, m) }, startTime.hour, startTime.minute, true).show()
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Text("Время начала", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                            Text("%02d:%02d".format(startTime.hour, startTime.minute), fontSize = 16.sp, color = AppColors.Blue, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                
                SectionCard(title = "Категория") {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Категория", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        Box {
                            Row(modifier = Modifier.clickable { categoryMenu = true }, verticalAlignment = Alignment.CenterVertically) {
                                val name = categoryId?.let { id -> categories.firstOrNull { it.id == id }?.name } ?: "Без категории"
                                Text(name, color = AppColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Icon(Icons.Filled.ExpandMore, null, tint = AppColors.Blue, modifier = Modifier.size(16.dp))
                            }
                            IosDropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                IosMenuItem("Без категории", Icons.Filled.Label, AppColors.GrayText) { categoryId = null; categoryMenu = false }
                                if (categories.isNotEmpty()) IosMenuDivider()
                                categories.forEach { cat -> IosMenuItem(cat.name, categoryIcon(cat.iconName ?: "") ?: Icons.Filled.Label, colorFromHex(cat.colorHex ?: "#007AFF")) { categoryId = cat.id; categoryMenu = false } }
                            }
                        }
                    }
                }

                
                SectionCard(title = "Приоритет") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TaskPriority.LOW to "Низкий", TaskPriority.MEDIUM to "Средний", TaskPriority.HIGH to "Высокий").forEach { (p, label) ->
                            val color = priorityColor(p.raw)
                            val selected = priority == p
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (selected) color.copy(alpha = 0.22f) else Color.Transparent)
                                .border(BorderStroke(1.dp, if (selected) color else Color(0xFFE0E0E0)), RoundedCornerShape(10.dp))
                                .clickable { priority = p }.padding(vertical = 8.dp), contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (selected) color else AppColors.GrayText, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }

                
                SectionCard(title = "Напоминание") {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Включить напоминание", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(checked = hasReminder, onCheckedChange = { v: Boolean -> hasReminder = v },
                            enabled = hasTime,
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA), uncheckedBorderColor = Color.Transparent))
                    }
                    if (hasReminder && hasTime) {
                        IosThinDivider()
                        ReminderOffsetChips(
                            selected = reminderOffsetMinutes,
                            onSelected = { reminderOffsetMinutes = it }
                        )
                    }
                }

                
                SectionCard(title = "Повтор") {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Частота", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        Box {
                            Row(modifier = Modifier.clickable { frequencyMenu = true }, verticalAlignment = Alignment.CenterVertically) {
                                Text(frequencyTitle(frequency), color = AppColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Icon(Icons.Filled.ExpandMore, null, tint = AppColors.Blue, modifier = Modifier.size(16.dp))
                            }
                            IosDropdownMenu(expanded = frequencyMenu, onDismissRequest = { frequencyMenu = false }) {
                                listOf(com.taskplanner.android.core.model.RecurrenceFrequency.DAILY, com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY, com.taskplanner.android.core.model.RecurrenceFrequency.MONTHLY, com.taskplanner.android.core.model.RecurrenceFrequency.YEARLY).forEach { f ->
                                    IosMenuItem(frequencyTitle(f), Icons.Filled.Repeat, if (frequency == f) AppColors.Blue else AppColors.Label) {
                                        frequency = f; frequencyMenu = false
                                        if (f == com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY && selectedWeekdays.isEmpty() && existing == null) selectedWeekdays.add(startDate.dayOfWeek.value)
                                    }
                                }
                            }
                        }
                    }
                    IosThinDivider()
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(frequencyTitle(frequency), fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E0E0)).clickable { if (interval > 1) interval-- }, contentAlignment = Alignment.Center) {
                                Text("−", fontSize = 18.sp, color = AppColors.Label, fontWeight = FontWeight.Light)
                            }
                            Text("$interval", fontSize = 16.sp, color = AppColors.Label,
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E0E0)).clickable { interval++ }, contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 18.sp, color = AppColors.Label, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                    if (frequency == com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY) {
                        IosThinDivider()
                        Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Дни недели", fontSize = 14.sp, color = AppColors.GrayText)
                            WeekdayPicker(selected = selectedWeekdays)
                        }
                    }
                    IosThinDivider()
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Есть окончание", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(checked = hasEndDate, onCheckedChange = { v: Boolean -> hasEndDate = v; if (endDate.isBefore(startDate)) endDate = startDate },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA), uncheckedBorderColor = Color.Transparent))
                    }
                    if (hasEndDate) {
                        IosThinDivider()
                        Row(modifier = Modifier.fillMaxWidth().clickable { pickingEnd = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Окончание", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                            Text(endDate.formatRussianLong(), fontSize = 16.sp, color = AppColors.Blue, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("Повторяющиеся задачи создаются постепенно на ближайшие 30 дней.", fontSize = 12.sp, color = AppColors.GrayText)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (pickingStart) {
        CalendarDialog(
            initial = startDate,
            onDismiss = { pickingStart = false },
            onSelected = {
                startDate = it
                if (endDate.isBefore(startDate)) endDate = startDate
                if (frequency == com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY && selectedWeekdays.isEmpty() && existing == null) {
                    selectedWeekdays.add(startDate.dayOfWeek.value)
                }
                pickingStart = false
            }
        )
    }
    if (pickingEnd) {
        CalendarDialog(
            initial = endDate,
            onDismiss = { pickingEnd = false },
            onSelected = {
                endDate = it
                if (endDate.isBefore(startDate)) endDate = startDate
                pickingEnd = false
            }
        )
    }
}

@Composable
private fun WeekdayPicker(selected: MutableList<Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Дни недели", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (1..7).forEach { day ->
                val isSelected = selected.contains(day)
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable {
                            if (isSelected) selected.remove(day) else selected.add(day)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(weekdayShort(day), color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun frequencyTitle(f: com.taskplanner.android.core.model.RecurrenceFrequency): String {
    return when (f) {
        com.taskplanner.android.core.model.RecurrenceFrequency.DAILY -> "Каждый день"
        com.taskplanner.android.core.model.RecurrenceFrequency.WEEKLY -> "Каждую неделю"
        com.taskplanner.android.core.model.RecurrenceFrequency.MONTHLY -> "Каждый месяц"
        com.taskplanner.android.core.model.RecurrenceFrequency.YEARLY -> "Каждый год"
    }
}

@Composable
private fun PrioritySelector(selected: TaskPriority, onSelected: (TaskPriority) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PriorityChip("Н", TaskPriority.LOW, selected, onSelected)
        PriorityChip("С", TaskPriority.MEDIUM, selected, onSelected)
        PriorityChip("В", TaskPriority.HIGH, selected, onSelected)
    }
}

@Composable
private fun PriorityChip(label: String, value: TaskPriority, selected: TaskPriority, onSelected: (TaskPriority) -> Unit) {
    val isSelected = selected == value
    val bg = if (isSelected) priorityColor(value.raw) else Color.Transparent
    val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg.copy(alpha = if (isSelected) 0.95f else 0.15f))
            .clickable { onSelected(value) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun reminderOffsetLabel(minutes: Int): String {
    return when (minutes) {
        0 -> "В момент"
        5 -> "5 мин"
        10 -> "10 мин"
        15 -> "15 мин"
        30 -> "30 мин"
        60 -> "60 мин"
        else -> "$minutes мин"
    }
}

@Composable
private fun ReminderOffsetChips(selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Когда напомнить", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
            Text(reminderOffsetLabel(selected), color = AppColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Filled.ExpandMore, null, tint = AppColors.Blue, modifier = Modifier.size(16.dp))
        }
        IosDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0, 5, 10, 15, 30, 60).forEach { minutes ->
                IosMenuItem(
                    reminderOffsetLabel(minutes),
                    Icons.Filled.AccessTime,
                    if (selected == minutes) AppColors.Blue else AppColors.Label
                ) {
                    onSelected(minutes)
                    expanded = false
                }
            }
        }
    }
}

private fun weekdayLong(day: Int): String {
    return when (day) {
        1 -> "Понедельник"
        2 -> "Вторник"
        3 -> "Среда"
        4 -> "Четверг"
        5 -> "Пятница"
        6 -> "Суббота"
        7 -> "Воскресенье"
        else -> "День $day"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ApplyTemplateBottomSheet(
    template: ScheduleTemplateEntity,
    items: List<ScheduleTemplateItemEntity>,
    categories: List<CategoryEntity>,
    existingApplications: List<com.taskplanner.android.data.local.entities.TemplateApplicationEntity>,
    onDismiss: () -> Unit,
    onApply: (start: LocalDate, end: LocalDate, onApplied: (applicationId: String?) -> Unit) -> Unit,
    onUndo: (applicationId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startDate by remember(template.id) { mutableStateOf(LocalDate.now()) }
    var endDate by remember(template.id) { mutableStateOf(LocalDate.now().plusDays(7)) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var appliedApplicationId by remember(template.id) { mutableStateOf<String?>(null) }
    var showOverlapWarning by remember { mutableStateOf(false) }
    var pendingApplyStart by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var pendingApplyEnd by remember { mutableStateOf<java.time.LocalDate?>(null) }

    LaunchedEffect(appliedApplicationId) {
        if (appliedApplicationId != null) {
            kotlinx.coroutines.delay(800)
            onDismiss()
        }
    }

    fun hasOverlap(start: LocalDate, end: LocalDate): Boolean {
        val startMs = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(start)
        val endMs = com.taskplanner.android.core.util.TimeUtils.startOfDayMillis(end)
        return existingApplications.any { app ->
            app.deletedAt == null &&
            app.startDate <= endMs &&
            app.endDate >= startMs
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            SheetHeader(
                title = "Запуск шаблона",
                cancelText = if (appliedApplicationId == null) "Отмена" else "Готово",
                confirmText = "Запустить",
                confirmEnabled = appliedApplicationId == null && !endDate.isBefore(startDate) && items.isNotEmpty(),
                onCancel = onDismiss,
                onConfirm = {
                    if (hasOverlap(startDate, endDate)) {
                        pendingApplyStart = startDate
                        pendingApplyEnd = endDate
                        showOverlapWarning = true
                    } else {
                        onApply(startDate, endDate) { id ->
                            appliedApplicationId = id
                        }
                    }
                }
            )

            if (appliedApplicationId != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AppColors.Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Шаблон применён!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Создано задач: ${items.filter { it.deletedAt == null }.sumOf { countOccurrences(startDate, endDate, it.weekday) }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pickingStart = true }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Старт", modifier = Modifier.weight(1f))
                            Text(startDate.formatRussianLong(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pickingEnd = true }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Окончание", modifier = Modifier.weight(1f))
                            Text(endDate.formatRussianLong(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (items.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Будут созданы задачи", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            val sorted = items
                                .filter { it.deletedAt == null }
                                .sortedWith(compareBy<ScheduleTemplateItemEntity> { it.weekday }.thenBy { it.position }.thenBy { it.title })

                            sorted.forEach { item ->
                                val count = countOccurrences(startDate, endDate, item.weekday)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    val color = when (TaskPriority.values().firstOrNull { it.raw == item.priority } ?: TaskPriority.MEDIUM) {
                                        TaskPriority.HIGH -> AppColors.Red
                                        TaskPriority.MEDIUM -> AppColors.Orange
                                        TaskPriority.LOW -> AppColors.Green
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(weekdayShort(item.weekday), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("$count раз", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Divider(modifier = Modifier.padding(top = 4.dp))
                            Text(
                                "Всего задач будет создано: ${sorted.sumOf { countOccurrences(startDate, endDate, it.weekday) }}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Кнопка отмены применения убрана
                }
            }
        }
    }

    if (pickingStart) {
        CalendarDialog(
            initial = startDate,
            onDismiss = { pickingStart = false },
            onSelected = {
                startDate = it
                if (endDate.isBefore(startDate)) endDate = startDate
                pickingStart = false
            }
        )
    }
    if (pickingEnd) {
        CalendarDialog(
            initial = endDate,
            onDismiss = { pickingEnd = false },
            onSelected = {
                endDate = it
                if (endDate.isBefore(startDate)) startDate = endDate
                pickingEnd = false
            }
        )
    }

    if (showOverlapWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showOverlapWarning = false },
            title = { Text("Шаблон уже применён") },
            text = { Text("На этот период уже были созданы задачи по этому шаблону. Создать задачи ещё раз?") },
            confirmButton = {
                TextButton(onClick = {
                    showOverlapWarning = false
                    val s = pendingApplyStart ?: return@TextButton
                    val e = pendingApplyEnd ?: return@TextButton
                    onApply(s, e) { id -> appliedApplicationId = id }
                }) { Text("Создать снова") }
            },
            dismissButton = {
                TextButton(onClick = { showOverlapWarning = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TemplateApplicationsBottomSheet(
    onDismiss: () -> Unit,
    applicationsFlow: () -> kotlinx.coroutines.flow.Flow<List<TemplateApplicationEntity>>,
    tasksFlow: (applicationId: String) -> kotlinx.coroutines.flow.Flow<List<TaskEntity>>,
    categories: List<CategoryEntity>,
    loadTaskCount: suspend (applicationId: String) -> Int,
    onDeleteApplication: (applicationId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val applications by applicationsFlow().collectAsState(initial = emptyList())
    var pendingDelete by remember { mutableStateOf<TemplateApplicationEntity?>(null) }
    var selectedApplication by remember { mutableStateOf<TemplateApplicationEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        dragHandle = null
    ) {
        if (selectedApplication != null) {
            TemplateApplicationDetailContent(
                application = selectedApplication!!,
                tasksFlow = { tasksFlow(selectedApplication!!.id) },
                categories = categories,
                onBack = { selectedApplication = null }
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                SheetHeader(
                    title = "Запуски шаблона",
                    cancelText = "Готово",
                    confirmText = "",
                    confirmEnabled = false,
                    onCancel = onDismiss,
                    onConfirm = {}
                )

                if (applications.isEmpty()) {
                    Text(
                        "Этот шаблон ещё ни разу не применяли.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    return@ModalBottomSheet
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(applications, key = { it.id }) { application ->
                        val start = TimeUtils.localDateFromMillis(application.startDate).formatRussianLong()
                        val end = TimeUtils.localDateFromMillis(application.endDate).formatRussianLong()
                        val count = produceState<Int?>(initialValue = null, application.id) {
                            value = loadTaskCount(application.id)
                        }.value

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedApplication = application }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("$start – $end", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    val tasksText = count?.let { "Задач: $it" } ?: "Задач: …"
                                    Text(tasksText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(onClick = { pendingDelete = application }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                }
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = AppColors.GrayText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { application ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить применение шаблона?") },
            text = { Text("Будут удалены только задачи, созданные этим запуском.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteApplication(application.id)
                        pendingDelete = null
                    }
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            }
        )
    }

}

private data class AppliedTemplateTaskGroup(
    val id: String,
    val weekday: Int,
    val title: String,
    val startTime: Long?,
    val priority: TaskPriority,
    val category: CategoryEntity?,
    val createdCount: Int
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TemplateApplicationDetailContent(
    application: TemplateApplicationEntity,
    tasksFlow: () -> kotlinx.coroutines.flow.Flow<List<TaskEntity>>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit
) {
    val tasks by tasksFlow().collectAsState(initial = emptyList())
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val taskGroups = remember(tasks, categoryById) {
        buildAppliedTemplateTaskGroups(tasks, categoryById)
    }
    val groupedByWeekday = remember(taskGroups) {
        taskGroups.groupBy { it.weekday }.toSortedMap()
    }
    val start = TimeUtils.localDateFromMillis(application.startDate).formatRussianLong()
    val end = TimeUtils.localDateFromMillis(application.endDate).formatRussianLong()

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        SheetHeader(
            title = "Применение",
            cancelText = "Назад",
            confirmText = "",
            confirmEnabled = false,
            onCancel = onBack,
            onConfirm = {}
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$start – $end", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Задач: ${tasks.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (taskGroups.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "В этом применении шаблона задач не осталось.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                groupedByWeekday.forEach { (weekday, groups) ->
                    item(key = "weekday-$weekday") {
                        Text(
                            weekdayTitle(weekday),
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = AppColors.GrayText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(groups, key = { it.id }) { group ->
                        AppliedTemplateTaskRow(group)
                    }
                }
            }
        }
    }
}

private fun buildAppliedTemplateTaskGroups(
    tasks: List<TaskEntity>,
    categoryById: Map<String, CategoryEntity>
): List<AppliedTemplateTaskGroup> {
    return tasks
        .groupBy { task ->
            val weekday = taskWeekday(task)
            val timeKey = task.startTime?.let { TimeUtils.localTimeFromMillis(it).let { time -> time.hour * 60 + time.minute } } ?: -1
            listOf(
                weekday.toString(),
                task.title,
                timeKey.toString(),
                task.priority.toString(),
                task.categoryId.orEmpty()
            ).joinToString("|")
        }
        .mapNotNull { (key, groupedTasks) ->
            val first = groupedTasks.firstOrNull() ?: return@mapNotNull null
            AppliedTemplateTaskGroup(
                id = key,
                weekday = taskWeekday(first),
                title = first.title,
                startTime = first.startTime,
                priority = TaskPriority.values().firstOrNull { it.raw == first.priority } ?: TaskPriority.MEDIUM,
                category = first.categoryId?.let { categoryById[it] },
                createdCount = groupedTasks.size
            )
        }
        .sortedWith(
            compareBy<AppliedTemplateTaskGroup> { it.weekday }
                .thenBy { group -> group.startTime?.let { TimeUtils.localTimeFromMillis(it).let { time -> time.hour * 60 + time.minute } } ?: Int.MAX_VALUE }
                .thenBy { it.title }
        )
}

private fun taskWeekday(task: TaskEntity): Int {
    val date = TimeUtils.localDateFromMillis(task.instanceDate ?: task.date)
    return date.dayOfWeek.value
}

@Composable
private fun AppliedTemplateTaskRow(group: AppliedTemplateTaskGroup) {
    val priorityColor = when (group.priority) {
        TaskPriority.HIGH -> AppColors.Red
        TaskPriority.MEDIUM -> AppColors.Orange
        TaskPriority.LOW -> AppColors.Green
    }
    val category = group.category

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(group.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (group.startTime != null) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = AppColors.GrayText, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(TimeUtils.localTimeFromMillis(group.startTime).toString(), style = MaterialTheme.typography.bodySmall, color = AppColors.GrayText)
                    } else {
                        Text("Без времени", style = MaterialTheme.typography.bodySmall, color = AppColors.GrayText)
                    }

                    Text("  •  Создано: ${group.createdCount}", style = MaterialTheme.typography.bodySmall, color = AppColors.GrayText)

                    if (category != null) {
                        Text("  •  ", style = MaterialTheme.typography.bodySmall, color = AppColors.GrayText)
                        Icon(
                            categoryIcon(category.iconName) ?: Icons.Filled.Label,
                            contentDescription = null,
                            tint = colorFromHex(category.colorHex),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(category.name, style = MaterialTheme.typography.bodySmall, color = AppColors.GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun weekdayTitle(weekday: Int): String {
    return when (weekday) {
        1 -> "Понедельник"
        2 -> "Вторник"
        3 -> "Среда"
        4 -> "Четверг"
        5 -> "Пятница"
        6 -> "Суббота"
        7 -> "Воскресенье"
        else -> "День $weekday"
    }
}

@Composable
private fun SheetHeader(
    title: String,
    cancelText: String = "Отмена",
    confirmText: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) { Text(cancelText) }
        Spacer(modifier = Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.weight(1f))
        if (confirmText.isNotBlank()) {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) }
        } else {
            Spacer(modifier = Modifier.width(64.dp))
        }
    }
}

private fun countOccurrences(start: LocalDate, end: LocalDate, weekday: Int): Int {
    var count = 0
    var cursor = start
    while (!cursor.isAfter(end)) {
        if (cursor.dayOfWeek.value == weekday) count += 1
        cursor = cursor.plusDays(1)
    }
    return count
}

private fun colorFromHex(hex: String): Color {
    val cleaned = hex.trim().removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return Color(0xFF0A84FF)
    return when (cleaned.length) {
        6 -> Color((0xFF000000 or value).toInt())
        8 -> Color(value.toInt())
        else -> Color(0xFF0A84FF)
    }
}

@Composable
private fun CalendarDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSelected: (LocalDate) -> Unit
) {
    val locale = Locale("ru")
    var month by remember(initial) { mutableStateOf(YearMonth.of(initial.year, initial.month)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${month.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
            }
        },
        text = { MonthGrid(month = month, selected = initial, onSelect = onSelected) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val firstDay = month.atDay(1)
    val firstDow = (firstDay.dayOfWeek.value + 6) % 7 
    val daysInMonth = month.lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("П", "В", "С", "Ч", "П", "С", "В").forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        val totalCells = firstDow + daysInMonth
        val rows = (totalCells + 6) / 7
        var day = 1
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    if (cellIndex < firstDow || day > daysInMonth) {
                        Box(modifier = Modifier.size(32.dp))
                    } else {
                        val date = month.atDay(day)
                        val isSelected = date == selected
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable { onSelect(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), style = MaterialTheme.typography.bodySmall)
                        }
                        day++
                    }
                }
            }
        }
    }
}

private fun weekdayShort(weekday: Int): String = when (weekday) {
    1 -> "Пн"
    2 -> "Вт"
    3 -> "Ср"
    4 -> "Чт"
    5 -> "Пт"
    6 -> "Сб"
    7 -> "Вс"
    else -> ""
}

private fun priorityColor(priorityRaw: Int): Color = when (priorityRaw) {
    TaskPriority.HIGH.raw -> AppColors.PriorityHigh
    TaskPriority.MEDIUM.raw -> AppColors.PriorityMedium
    TaskPriority.LOW.raw -> AppColors.PriorityLow
    else -> AppColors.PriorityMedium
}

private fun recurrenceSummary(rule: RecurrenceRuleEntity): String {
    val frequency = RecurrenceFrequency.values().firstOrNull { it.raw == rule.frequency } ?: RecurrenceFrequency.WEEKLY
    val interval = rule.intervalValue.coerceAtLeast(1)
    return when (frequency) {
        RecurrenceFrequency.DAILY -> if (interval == 1) "Каждый день" else "Каждые $interval дня"
        RecurrenceFrequency.WEEKLY -> {
            val days = weekdaysSummary(rule.weekdaysMask)
            val prefix = if (interval == 1) "Каждую неделю" else "Каждые $interval недели"
            if (days.isBlank()) prefix else "$prefix: $days"
        }
        RecurrenceFrequency.MONTHLY -> {
            val day = rule.dayOfMonth.coerceAtLeast(1)
            val prefix = if (interval == 1) "Каждый месяц" else "Каждые $interval месяца"
            "$prefix, $day числа"
        }
        RecurrenceFrequency.YEARLY -> {
            val day = rule.dayOfMonth.coerceAtLeast(1)
            val month = rule.monthOfYear.coerceAtLeast(1)
            val prefix = if (interval == 1) "Каждый год" else "Каждые $interval года"
            "$prefix, $day.$month"
        }
    }
}

private fun weekdaysSummary(mask: Int): String {
    val days = listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
    return days.mapNotNull { (num, label) ->
        val isSet = (mask and (1 shl num)) != 0
        if (isSet) label else null
    }.joinToString(", ")
}

private fun LocalDate.formatRussianShort(): String {
    val locale = Locale("ru")
    val month = this.month.getDisplayName(TextStyle.SHORT, locale)
    return "${this.dayOfMonth} $month"
}

private fun LocalDate.formatRussianLong(): String {
    val locale = Locale("ru")
    val month = this.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }
    return "${this.dayOfMonth} $month ${this.year}"
}

@Composable
private fun IosField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (value.isEmpty()) {
            Text(placeholder, color = AppColors.GrayText, fontSize = 16.sp)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.Label, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Blue),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title.isNotEmpty()) {
            Text(
                title.uppercase(),
                fontSize = 12.sp,
                color = AppColors.GrayText,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFFFFF))
                .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun IosThinDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.SeparatorLight))
}

private val SF_TO_MATERIAL: Map<String, androidx.compose.ui.graphics.vector.ImageVector> = mapOf(
    "tag.fill" to Icons.Filled.Label,
    "person.fill" to Icons.Filled.Person,
    "briefcase.fill" to Icons.Filled.Work,
    "house.fill" to Icons.Filled.Home,
    "book.fill" to Icons.Filled.MenuBook,
    "heart.fill" to Icons.Filled.Favorite,
    "figure.run" to Icons.Filled.DirectionsRun,
    "paintpalette.fill" to Icons.Filled.Palette,
    "cart.fill" to Icons.Filled.ShoppingCart,
    "car.fill" to Icons.Filled.DirectionsCar,
    "airplane" to Icons.Filled.Flight,
    "gamecontroller.fill" to Icons.Filled.SportsEsports,
    
    "tag" to Icons.Filled.Label,
    "person" to Icons.Filled.Person,
    "work" to Icons.Filled.Work,
    "home" to Icons.Filled.Home,
    "school" to Icons.Filled.School,
    "favorite" to Icons.Filled.Favorite,
    "fitness_center" to Icons.Filled.FitnessCenter,
    "palette" to Icons.Filled.Palette,
    "shopping_cart" to Icons.Filled.ShoppingCart,
    "directions_car" to Icons.Filled.DirectionsCar,
    "flight" to Icons.Filled.Flight,
    "sports_esports" to Icons.Filled.SportsEsports
)

private fun categoryIcon(iconName: String?): androidx.compose.ui.graphics.vector.ImageVector? {
    return iconName?.let { SF_TO_MATERIAL[it] }
}

@Composable
private fun IosDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .widthIn(min = 200.dp)
    ) {
        content()
    }
}

@Composable
private fun IosMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = AppColors.Label,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(title, color = tint, fontSize = 16.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun IosMenuDivider() {
    Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
}
