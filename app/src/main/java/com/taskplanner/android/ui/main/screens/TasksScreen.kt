package com.taskplanner.android.ui.main.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.ui.LocalAppGraph
import com.taskplanner.android.ui.components.GradientFab
import com.taskplanner.android.ui.components.PriorityStyle
import com.taskplanner.android.ui.main.tasks.TasksViewModel
import com.taskplanner.android.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TasksScreen(
    padding: PaddingValues,
    userId: String,
    initialDate: LocalDate?,
    onInitialDateConsumed: () -> Unit,
    initialTaskId: String?,
    onInitialTaskIdConsumed: () -> Unit
) {
    val graph = LocalAppGraph.current
    val vm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(userId, graph.taskRepository, graph.categoryRepository))

    val tasks by vm.tasks.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val categories by vm.categories.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val currentSort by vm.currentSort.collectAsState()

    var detailTask by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(initialDate) {
        if (initialDate != null) {
            vm.selectDate(initialDate)
            onInitialDateConsumed()
        }
    }

    LaunchedEffect(initialTaskId, tasks) {
        if (initialTaskId == null) return@LaunchedEffect
        val found = tasks.firstOrNull { it.id == initialTaskId }
        if (found != null) {
            detailTask = found
            onInitialTaskIdConsumed()
        }
    }

    
    LaunchedEffect(tasks) {
        val id = detailTask?.id ?: return@LaunchedEffect
        val updated = tasks.firstOrNull { it.id == id }
        if (updated != null) detailTask = updated
    }

    val categoryById = remember(categories) { categories.associateBy { it.id } }

    var showEditor by remember { mutableStateOf(false) }
    var editorMode: EditorMode by remember { mutableStateOf(EditorMode.Create) }

    var filterMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var showCategoryFilter by remember { mutableStateOf(false) }
    val categoryFilter by vm.categoryFilter.collectAsState()
    val calendarTasks by vm.calendarTasks.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    var showingCalendar by remember { mutableStateOf(false) }

    
    var reorderList by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }
    var isDragging by remember { mutableStateOf(false) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeight = 78.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(tasks, isDragging) {
        if (!isDragging) reorderList = tasks
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SystemBackground)
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "Задачи",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Label,
                    modifier = Modifier.align(Alignment.Center)
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosRoundIconButton(
                        icon = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                        onClick = {
                            searchActive = !searchActive
                            vm.setSearchActive(searchActive)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IosRoundIconButton(
                        icon = if (categoryFilter.isEmpty()) Icons.Filled.Label else Icons.Filled.Label,
                        onClick = { showCategoryFilter = true },
                        tint = if (categoryFilter.isEmpty()) null else AppColors.Orange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        IosRoundIconButton(
                            icon = Icons.Filled.Sort,
                            onClick = { sortMenu = true }
                        )
                        DropdownMenu(
                            expanded = sortMenu,
                            onDismissRequest = { sortMenu = false },
                            modifier = Modifier.background(Color.White, RoundedCornerShape(14.dp)).widthIn(min = 220.dp)
                        ) {
                            val currentFilter by vm.currentFilter.collectAsState()
                            val currentSortVal = currentSort
                            
                            Text("Показать", fontSize = 12.sp, color = AppColors.GrayText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                            listOf(
                                Triple(TasksViewModel.Filter.ALL, "Все", Icons.Filled.List),
                                Triple(TasksViewModel.Filter.COMPLETED, "Выполненные", Icons.Filled.CheckCircle),
                                Triple(TasksViewModel.Filter.INCOMPLETE, "Невыполненные", Icons.Filled.RadioButtonUnchecked)
                            ).forEach { (f, label, icon) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 16.sp, color = AppColors.Label) },
                                    trailingIcon = { if (currentFilter == f) Icon(Icons.Filled.Check, null, tint = AppColors.Blue, modifier = Modifier.size(18.dp)) },
                                    onClick = { vm.setFilter(f); sortMenu = false }
                                )
                                Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            
                            Text("Сортировка", fontSize = 12.sp, color = AppColors.GrayText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                            listOf(
                                Triple(TasksViewModel.Sort.CUSTOM, "Без", Icons.Filled.Sort),
                                Triple(TasksViewModel.Sort.PRIORITY_DESC, "Приоритет ↓", Icons.Filled.KeyboardArrowDown),
                                Triple(TasksViewModel.Sort.PRIORITY_ASC, "Приоритет ↑", Icons.Filled.KeyboardArrowUp)
                            ).forEach { (s, label, icon) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 16.sp, color = AppColors.Label) },
                                    trailingIcon = { if (currentSortVal == s) Icon(Icons.Filled.Check, null, tint = AppColors.Blue, modifier = Modifier.size(18.dp)) },
                                    onClick = { vm.setSort(s); sortMenu = false }
                                )
                                if (s != TasksViewModel.Sort.PRIORITY_ASC)
                                    Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = searchActive) {
                SearchBar(
                    vm = vm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            if (!searchActive) {
                DateRow(
                    selectedDate = selectedDate,
                    onPrev = { vm.selectDate(selectedDate.minusDays(1)) },
                    onNext = { vm.selectDate(selectedDate.plusDays(1)) },
                    onToday = { vm.selectDate(LocalDate.now()) },
                    onOpenCalendar = { showingCalendar = true }
                )

                if (reorderList.isEmpty()) {
                    Text("Пока нет задач", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        itemsIndexed(reorderList, key = { _, t -> t.id }) { index, task ->
                            val isCompleted = task.status == TaskStatus.COMPLETED.raw
                            val timeText = vm.formatTaskTime(task)
                            val category = task.categoryId?.let { categoryById[it] }
                            val alpha by animateFloatAsState(if (isCompleted) 0.55f else 1f, label = "taskAlpha")

                            val subtasksCount = produceState<Int?>(initialValue = null, task.id) {
                                value = vm.countSubtasks(task.id)
                            }.value

                            TaskCard(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .height(itemHeight)
                                    .graphicsLayer {
                                        if (draggingIndex == index) translationY = dragOffsetY
                                    }
                                    .zIndex(if (draggingIndex == index) 1f else 0f)
                                    .alpha(alpha)
                                    .let { base ->
                                        if (currentSort == TasksViewModel.Sort.CUSTOM) {
                                            base.pointerInput(reorderList, draggingIndex, dragOffsetY) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        isDragging = true
                                                        draggingIndex = index
                                                    },
                                                    onDragCancel = {
                                                        isDragging = false
                                                        draggingIndex = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        isDragging = false
                                                        draggingIndex = null
                                                        dragOffsetY = 0f
                                                        vm.persistCustomOrder(reorderList.map { it.id })
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                        dragOffsetY += dragAmount.y
                                                        val deltaIndex = (dragOffsetY / itemHeightPx).roundToInt()
                                                        if (deltaIndex != 0) {
                                                            val newIndex = (currentIndex + deltaIndex).coerceIn(0, reorderList.lastIndex)
                                                            if (newIndex != currentIndex) {
                                                                val mutable = reorderList.toMutableList()
                                                                val moved = mutable.removeAt(currentIndex)
                                                                mutable.add(newIndex, moved)
                                                                reorderList = mutable.toList()
                                                                draggingIndex = newIndex
                                                                dragOffsetY -= (deltaIndex * itemHeightPx)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        } else {
                                            base
                                        }
                                    },
                                task = task,
                                timeText = timeText,
                                category = category,
                                subtasksCount = subtasksCount,
                                onToggleCompleted = { vm.toggleCompletion(task.id) },
                                onClick = { detailTask = task }
                            )
                        }
                    }
                }
            } else {
                SearchResults(
                    results = searchResults,
                    searchQuery = vm.searchQuery.value,
                    onSelect = { task ->
                        val date = TimeUtils.localDateFromMillis(task.date)
                        vm.selectDate(date)
                        searchActive = false
                        vm.setSearchActive(false)
                        detailTask = task
                    }
                )
            }
        }

        GradientFab(
            onClick = {
                editorMode = EditorMode.Create
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }

    if (detailTask != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        TaskDetailBottomSheet(
            vm = vm,
            sheetState = sheetState,
            task = detailTask!!,
            category = detailTask!!.categoryId?.let { categoryById[it] },
            timeText = vm.formatTaskTime(detailTask!!),
            onDismiss = { detailTask = null },
            onEdit = {
                editorMode = EditorMode.Edit(detailTask!!)
                showEditor = true
            },
            onDelete = {
                vm.deleteTask(detailTask!!.id)
                detailTask = null
            }
        )
    }

    if (showEditor) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        TaskEditorBottomSheet(
            mode = editorMode,
            sheetState = sheetState,
            defaultDate = selectedDate,
            categories = categories,
            onDismiss = { showEditor = false },
            onSave = { data ->
                when (val mode = editorMode) {
                    is EditorMode.Create -> vm.createTask(
                        date = data.date,
                        title = data.title,
                        description = data.description,
                        imageData = data.imageData,
                        priority = data.priority,
                        categoryId = data.categoryId,
                        startTime = data.startTime,
                        hasReminder = data.hasReminder,
                        reminderOffsetMinutes = data.reminderOffsetMinutes
                    )
                    is EditorMode.Edit -> vm.updateTask(
                        taskId = mode.task.id,
                        date = data.date,
                        title = data.title,
                        description = data.description,
                        imageData = data.imageData,
                        removeImage = data.removeImage,
                        priority = data.priority,
                        categoryId = data.categoryId,
                        startTime = data.startTime,
                        hasReminder = data.hasReminder,
                        reminderOffsetMinutes = data.reminderOffsetMinutes
                    )
                }
                showEditor = false
            }
        )
    }

    if (showCategoryFilter) {
        CategoryFilterSheet(
            categories = categories,
            selectedIds = categoryFilter,
            onToggle = { vm.toggleCategoryFilter(it) },
            onClear = { vm.clearCategoryFilter() },
            onDismiss = { showCategoryFilter = false }
        )
    }

    if (showingCalendar) {
        CalendarDialog(
            selectedDate = selectedDate,
            tasks = calendarTasks,
            onSelect = { vm.selectDate(it) },
            onDismiss = { showingCalendar = false }
        )
    }
}

private sealed interface EditorMode {
    data object Create : EditorMode
    data class Edit(val task: TaskEntity) : EditorMode
}

private data class TaskEditorData(
    val date: LocalDate,
    val title: String,
    val description: String?,
    val imageData: ByteArray?,
    val removeImage: Boolean = false,
    val priority: TaskPriority,
    val categoryId: String?,
    val startTime: LocalTime?,
    val hasReminder: Boolean,
    val reminderOffsetMinutes: Int
)

@Composable
private fun IosRoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color? = null
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(AppColors.Blue.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Blue,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DateRow(
    selectedDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val locale = java.util.Locale("ru")
    val dayName = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", locale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    val dayMonth = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", locale))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Предыдущий день",
                tint = AppColors.Blue
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenCalendar)
        ) {
            Text(
                text = "$dayName,",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Label,
                lineHeight = 26.sp
            )
            Text(
                text = dayMonth,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Label,
                lineHeight = 26.sp
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Следующий день",
                tint = AppColors.Blue
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.Blue.copy(alpha = if (isToday) 0.12f else 0.06f))
                .clickable(onClick = onToday)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Сегодня",
                color = AppColors.Blue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SearchBar(vm: TasksViewModel, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val initial = remember { vm.currentSearchPeriod() }
    var from by remember { mutableStateOf(initial.first) }
    var to by remember { mutableStateOf(initial.second) }
    var periodEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        SectionCard(title = "Ключевые слова") {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                if (query.isEmpty()) Text("Поиск", color = AppColors.GrayText, fontSize = 16.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it; vm.updateSearchQuery(it) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.Label, fontSize = 16.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Blue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        SectionCard(title = "Период") {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Фильтр по периоду", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(
                    checked = periodEnabled,
                    onCheckedChange = { periodEnabled = it
                        vm.searchPeriodEnabled.value = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA),
                        uncheckedBorderColor = Color.Transparent)
                )
            }
            if (periodEnabled) {
                IosThinDivider()
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    DatePickerDialog(ctx, { _, y, m, d ->
                        from = LocalDate.of(y, m + 1, d); vm.updateSearchPeriod(from, to)
                    }, from.year, from.monthValue - 1, from.dayOfMonth).show()
                }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("С", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                    Text(from.toString(), fontSize = 16.sp, color = AppColors.Blue, fontWeight = FontWeight.Medium)
                }
                IosThinDivider()
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    DatePickerDialog(ctx, { _, y, m, d ->
                        to = LocalDate.of(y, m + 1, d); vm.updateSearchPeriod(from, to)
                    }, to.year, to.monthValue - 1, to.dayOfMonth).show()
                }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("По", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                    Text(to.toString(), fontSize = 16.sp, color = AppColors.Blue, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SearchResults(results: List<TaskEntity>, searchQuery: String, onSelect: (TaskEntity) -> Unit) {
    if (searchQuery.isEmpty()) return
    
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Search, null, tint = AppColors.GrayText, modifier = Modifier.size(40.dp))
                Text("Ничего не найдено", color = AppColors.GrayText, fontSize = 16.sp)
            }
        }
        return
    }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("РЕЗУЛЬТАТЫ", fontSize = 12.sp, color = AppColors.GrayText,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFFFFF))
                .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(12.dp))
        ) {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(results, key = { _, t -> t.id }) { idx, task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(task) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontSize = 16.sp, color = AppColors.Label, maxLines = 1)
                            if (!task.description.isNullOrBlank() && task.description!!.contains(searchQuery, ignoreCase = true)) {
                                Text(task.description!!, fontSize = 13.sp, color = AppColors.GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            task.date?.let { ms ->
                                Text(TimeUtils.localDateFromMillis(ms).toString(), fontSize = 12.sp, color = AppColors.GrayText)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = AppColors.GrayText, modifier = Modifier.size(18.dp))
                    }
                    if (idx < results.size - 1) {
                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    modifier: Modifier,
    task: TaskEntity,
    timeText: String?,
    category: CategoryEntity?,
    subtasksCount: Int?,
    onToggleCompleted: () -> Unit,
    onClick: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.COMPLETED.raw
    val containerColor = PriorityStyle.cardBg(task.priority)
    val dotColor = PriorityStyle.dotColor(task.priority)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onToggleCompleted() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AppColors.Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                        val stroke = 1.6.dp.toPx()
                        drawCircle(
                            color = AppColors.GrayText.copy(alpha = 0.6f),
                            radius = (size.minDimension - stroke) / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) AppColors.GrayText else AppColors.Label,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if ((subtasksCount ?: 0) > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+$subtasksCount",
                            fontSize = 12.sp,
                            color = AppColors.Blue
                        )
                    }
                    if (task.originType == com.taskplanner.android.core.model.TaskOriginType.GOAL_STEP.raw) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = AppColors.Yellow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (timeText != null) {
                    Text(
                        timeText,
                        fontSize = 13.sp,
                        color = AppColors.GrayText
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            
            if (task.hasReminder) {
                Icon(
                    Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = AppColors.Orange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (category != null) {
                val catColor = colorFromHex(category.colorHex)
                val catIcon = categoryIcon(category.iconName) ?: Icons.Filled.Label
                Icon(
                    imageVector = catIcon,
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TaskDetailBottomSheet(
    vm: TasksViewModel,
    sheetState: SheetState,
    task: TaskEntity,
    category: CategoryEntity?,
    timeText: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subtasks by vm.observeSubtasks(task.id).collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()
    var isAddingSubtask by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }

    val dateText = remember(task.date) { TimeUtils.localDateFromMillis(task.date).toString() }
    val priorityTitle = remember(task.priority) {
        when (task.priority) {
            TaskPriority.LOW.raw -> "Низкий"
            TaskPriority.MEDIUM.raw -> "Средний"
            TaskPriority.HIGH.raw -> "Высокий"
            else -> "Средний"
        }
    }
    val priorityColor = remember(task.priority) { taskPriorityColor(task.priority) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemBackground,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.originType == com.taskplanner.android.core.model.TaskOriginType.GOAL_STEP.raw) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.Yellow)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        task.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Label,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                if (!task.description.isNullOrBlank()) {
                    Text(
                        task.description!!,
                        fontSize = 15.sp,
                        color = AppColors.GrayText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                
                if (task.imageData != null) {
                    val bmp = remember(task.imageData) {
                        BitmapFactory.decodeByteArray(task.imageData, 0, task.imageData.size)
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Подзадачи", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                            color = AppColors.Label, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(AppColors.Blue)
                            .clickable { isAddingSubtask = !isAddingSubtask },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    
                    if (subtasks.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFFFFF))
                            .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(12.dp))) {
                            Column {
                                subtasks.forEachIndexed { idx, sub ->
                                    val completed = sub.status == TaskStatus.COMPLETED.raw
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { vm.toggleCompletion(sub.id) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.size(22.dp).clip(CircleShape)
                                            .background(if (completed) AppColors.Green else Color.Transparent)
                                            .border(BorderStroke(1.5.dp, if (completed) AppColors.Green else Color(0xFFBBBBBB)), CircleShape),
                                            contentAlignment = Alignment.Center) {
                                            if (completed) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        }
                                        Text(
                                            sub.title,
                                            fontSize = 15.sp,
                                            color = if (completed) AppColors.GrayText else AppColors.Label,
                                            textDecoration = if (completed) TextDecoration.LineThrough else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (idx < subtasks.size - 1) {
                                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 46.dp))
                                    }
                                }
                            }
                        }
                    }

                    
                    if (isAddingSubtask) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color.White).border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape)
                                    .border(BorderStroke(1.5.dp, Color(0xFFBBBBBB)), CircleShape))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (newSubtaskTitle.isEmpty()) Text("Название подзадачи", color = AppColors.GrayText, fontSize = 15.sp)
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = newSubtaskTitle,
                                        onValueChange = { newSubtaskTitle = it },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.Label, fontSize = 15.sp),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Blue),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (newSubtaskTitle.isNotBlank()) {
                                    Text("Добавить", color = AppColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable {
                                            vm.createSubtask(task.id, newSubtaskTitle)
                                            newSubtaskTitle = ""; isAddingSubtask = false
                                        })
                                }
                            }
                        }
                    }
                }

                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IosDetailPill(
                        icon = Icons.Filled.CalendarMonth,
                        iconTint = AppColors.Blue,
                        title = "Дата",
                        value = dateText
                    )
                    IosDetailPill(
                        icon = Icons.Filled.AccessTime,
                        iconTint = AppColors.Blue,
                        title = "Время",
                        value = timeText ?: "Без времени"
                    )
                    IosDetailPill(
                        icon = Icons.Filled.PriorityHigh,
                        iconTint = priorityColor,
                        title = "Приоритет",
                        value = priorityTitle
                    )
                    IosDetailPill(
                        icon = Icons.Filled.Notifications,
                        iconTint = if (task.hasReminder) AppColors.Orange else AppColors.GrayText,
                        title = "Напоминание",
                        value = if (task.hasReminder) "За ${task.reminderOffsetMinutes} мин" else "Отключено"
                    )
                    IosDetailPill(
                        icon = Icons.Filled.Label,
                        iconTint = category?.let { colorFromHex(it.colorHex) } ?: AppColors.GrayText,
                        title = "Категория",
                        value = category?.name ?: "Без категории"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Blue)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Редактировать", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Red)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun IosDetailPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, color = AppColors.GrayText)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColors.Label)
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    valueColor: Color = AppColors.Label
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.GrayText)
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = valueColor)
    }
}

private fun taskPriorityColor(priorityRaw: Int): Color {
    return when (priorityRaw) {
        TaskPriority.LOW.raw -> Color(0xFF33B885)
        TaskPriority.MEDIUM.raw -> Color(0xFFF5BA33)
        TaskPriority.HIGH.raw -> Color(0xFFED5770)
        else -> Color(0xFF999999)
    }
}

private fun colorFromHex(hex: String): Color {
    val clean = hex.removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return Color(0xFF007AFF)
    return if (clean.length <= 6) {
        Color((0xFF000000 or value).toInt())
    } else {
        Color(value.toInt())
    }
}

private val SF_TO_MATERIAL: Map<String, androidx.compose.ui.graphics.vector.ImageVector> = mapOf(
    "tag.fill" to Icons.Filled.Label,
    "person.fill" to Icons.Filled.Person,
    "briefcase.fill" to Icons.Filled.Work,
    "house.fill" to Icons.Filled.Home,
    "book.fill" to Icons.Filled.School,
    "heart.fill" to Icons.Filled.Favorite,
    "figure.run" to Icons.Filled.FitnessCenter,
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
@OptIn(ExperimentalMaterial3Api::class)
private fun TaskEditorBottomSheet(
    mode: EditorMode,
    sheetState: SheetState,
    defaultDate: LocalDate,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TaskEditorData) -> Unit
) {
    val ctx = LocalContext.current
    val existing = (mode as? EditorMode.Edit)?.task

    var date by remember(existing) {
        val d = existing?.let { TimeUtils.localDateFromMillis(it.date) } ?: defaultDate
        mutableStateOf(d)
    }
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var priority by remember(existing) {
        val p = TaskPriority.values().firstOrNull { it.raw == existing?.priority } ?: TaskPriority.MEDIUM
        mutableStateOf(p)
    }
    var categoryId by remember(existing) { mutableStateOf(existing?.categoryId) }

    var showingTime by remember(existing) { mutableStateOf(existing?.startTime != null) }
    var startTime by remember(existing) {
        val t = existing?.startTime?.let { TimeUtils.localTimeFromMillis(it) }
        mutableStateOf<LocalTime?>(t)
    }

    var hasReminder by remember(existing) { mutableStateOf(existing?.hasReminder ?: false) }
    var reminderOffset by remember(existing) { mutableIntStateOf(existing?.reminderOffsetMinutes ?: 15) }

    var imageBytes by remember(existing) { mutableStateOf(existing?.imageData) }
    var imageRemoved by remember(existing) { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) { imageBytes = bytes; imageRemoved = false }
        }
    }

    var priorityMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val canSave = title.trim().isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        modifier = Modifier.fillMaxHeight(0.94f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Отмена",
                    color = AppColors.Blue,
                    fontSize = 17.sp,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
                Text(
                    text = if (existing == null) "Новая задача" else "Редактировать",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Сохранить",
                    color = if (canSave) AppColors.Blue else AppColors.GrayText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = if (canSave) Modifier.clickable {
                        onSave(
                            TaskEditorData(
                                date = date,
                                title = title.trim(),
                                description = description.ifBlank { null },
                                imageData = imageBytes,
                                removeImage = imageRemoved,
                                priority = priority,
                                categoryId = categoryId,
                                startTime = if (showingTime) startTime else null,
                                hasReminder = hasReminder && showingTime,
                                reminderOffsetMinutes = reminderOffset
                            )
                        )
                    } else Modifier
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionCard(title = "Основное") {
                    IosTextFieldRow(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Название задачи",
                        singleLine = true
                    )
                    IosThinDivider()
                    IosTextFieldRow(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Описание",
                        singleLine = false,
                        minHeight = 90.dp
                    )
                }

                SectionCard(title = "Дата и время") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Дата", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                        IosPillButton(
                            text = formatDateRu(date),
                            onClick = {
                                DatePickerDialog(
                                    ctx,
                                    { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                                    date.year,
                                    date.monthValue - 1,
                                    date.dayOfMonth
                                ).show()
                            }
                        )
                    }

                    IosThinDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Указать время", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                        Switch(
                            checked = showingTime,
                            onCheckedChange = {
                                showingTime = it
                                if (!it) {
                                    startTime = null
                                    hasReminder = false
                                } else if (startTime == null) {
                                    startTime = LocalTime.of(9, 0)
                                }
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.Green,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE5E5EA),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    AnimatedVisibility(visible = showingTime) {
                        Column {
                            IosThinDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Время начала", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                                IosPillButton(
                                    text = startTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "09:00",
                                    onClick = {
                                        val init = startTime ?: LocalTime.of(9, 0)
                                        TimePickerDialog(
                                            ctx,
                                            { _, h, m -> startTime = LocalTime.of(h, m) },
                                            init.hour,
                                            init.minute,
                                            true
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }

                SectionCard(title = "Категория") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Категория", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                        Box {
                            val selected = categories.firstOrNull { it.id == categoryId }
                            Row(
                                modifier = Modifier.clickable { categoryMenu = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selected != null) {
                                    Icon(
                                        imageVector = categoryIcon(selected.iconName) ?: Icons.Filled.Label,
                                        contentDescription = null,
                                        tint = colorFromHex(selected.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = selected?.name ?: "Без категории",
                                    color = AppColors.Blue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AppColors.Blue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IosDropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                IosMenuItem("Без категории", Icons.Filled.Label, AppColors.GrayText) { categoryId = null; categoryMenu = false }
                                if (categories.isNotEmpty()) IosMenuDivider()
                                categories.forEach { cat ->
                                    IosMenuItem(
                                        cat.name,
                                        categoryIcon(cat.iconName) ?: Icons.Filled.Label,
                                        colorFromHex(cat.colorHex)
                                    ) { categoryId = cat.id; categoryMenu = false }
                                }
                            }
                        }
                    }
                }

                SectionCard(title = "Приоритет") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PriorityChip(
                            title = "Низкий",
                            color = taskPriorityColor(TaskPriority.LOW.raw),
                            selected = priority == TaskPriority.LOW,
                            onClick = { priority = TaskPriority.LOW }
                        )
                        PriorityChip(
                            title = "Средний",
                            color = taskPriorityColor(TaskPriority.MEDIUM.raw),
                            selected = priority == TaskPriority.MEDIUM,
                            onClick = { priority = TaskPriority.MEDIUM }
                        )
                        PriorityChip(
                            title = "Высокий",
                            color = taskPriorityColor(TaskPriority.HIGH.raw),
                            selected = priority == TaskPriority.HIGH,
                            onClick = { priority = TaskPriority.HIGH }
                        )
                    }
                }

                SectionCard(title = "Напоминание") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Включить напоминание", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it },
                            enabled = showingTime,
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.Green,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE5E5EA),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    if (hasReminder && showingTime) {
                        IosThinDivider()
                        val options = listOf(0, 5, 10, 15, 30, 60)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { opt ->
                                val sel = reminderOffset == opt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) AppColors.Blue.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { reminderOffset = opt }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    val label = if (opt == 0) "В момент" else "${opt} мин"
                                    Text(
                                        label,
                                        color = if (sel) AppColors.Blue else AppColors.GrayText,
                                        fontSize = 13.sp,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                SectionCard(title = "Изображение") {
                    if (imageBytes != null) {
                        val bmp = remember(imageBytes) { BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { imageBytes = null; imageRemoved = true },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = AppColors.Red)
                        ) { Text("Удалить изображение") }
                    }
                    TextButton(onClick = { imagePicker.launch("image/*") }) {
                        Text(if (imageBytes != null) "Изменить изображение" else "Выбрать изображение")
                    }
                }
            }
        }
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
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFFFFF))
                .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(12.dp))
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
private fun IosTextFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 24.dp
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (value.isEmpty()) {
            Text(placeholder, color = AppColors.GrayText, fontSize = 16.sp)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.Label, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Blue),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
        )
    }
}

@Composable
private fun IosThinDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.SeparatorLight))
}

@Composable
private fun IosPillButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SystemGroupedBackground, RoundedCornerShape(8.dp))
            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 15.sp, color = AppColors.Blue, fontWeight = FontWeight.Medium)
    }
}

private fun formatDateRu(d: LocalDate): String {
    val locale = java.util.Locale("ru")
    return d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", locale))
}

@Composable
private fun RowScope.PriorityChip(title: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) color.copy(alpha = 0.22f) else AppColors.SystemGroupedBackground
    val textColor = if (selected) color else AppColors.Label
    val borderColor = if (selected) color else Color.Transparent
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun CalendarDialog(
    selectedDate: LocalDate,
    tasks: List<TaskEntity>,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = remember { LocalDate.now() }

    
    val monthTasksRaw by rememberUpdatedState(tasks)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Календарь") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Text("←") }
                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru")).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Text("→") }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.GrayText,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }
                }

                val firstOfMonth = currentMonth.atDay(1)
                val leadingEmpty = (firstOfMonth.dayOfWeek.value + 6) % 7
                val days = buildList<LocalDate?> {
                    repeat(leadingEmpty) { add(null) }
                    for (day in 1..currentMonth.lengthOfMonth()) add(currentMonth.atDay(day))
                }

                
                val taskCountByDate = remember(monthTasksRaw, currentMonth) {
                    monthTasksRaw
                        .filter { it.deletedAt == null }
                        .groupBy { TimeUtils.localDateFromMillis(it.date) }
                        .mapValues { it.value.size }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(320.dp)
                ) {
                    items(days) { day ->
                        if (day == null) {
                            Spacer(modifier = Modifier.height(44.dp))
                        } else {
                            val isSelected = day == selectedDate
                            val isToday = day == today
                            val taskCount = taskCountByDate[day] ?: 0
                            val bg = when {
                                isSelected -> AppColors.Blue
                                isToday -> AppColors.Blue.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                            val fg = when {
                                isSelected -> Color.White
                                isToday -> AppColors.Blue
                                else -> AppColors.Label
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable {
                                        onSelect(day)
                                        onDismiss()
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    color = fg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (taskCount > 0) {
                                    Text(
                                        text = taskCount.toString(),
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else AppColors.Blue,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategoryFilterSheet(
    categories: List<CategoryEntity>,
    selectedIds: Set<String?>,
    onToggle: (String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Фильтр по категориям", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f), color = AppColors.Label)
                if (selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Сбросить", color = AppColors.Blue) }
                }
            }
            
            Surface(
                color = Color.White, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(12.dp))
                    .clickable { onToggle(null) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Label, contentDescription = null, tint = AppColors.GrayText, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Без категории", modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                    if (null in selectedIds) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Blue, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            categories.forEach { cat ->
                val catColor = colorFromHex(cat.colorHex ?: "#007AFF")
                Surface(
                    color = Color.White, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(12.dp))
                        .clickable { onToggle(cat.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(catColor))
                        Spacer(modifier = Modifier.width(10.dp))
                        categoryIcon(cat.iconName ?: "")?.let { Icon(it, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat.name, modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label)
                        if (cat.id in selectedIds) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Blue, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
