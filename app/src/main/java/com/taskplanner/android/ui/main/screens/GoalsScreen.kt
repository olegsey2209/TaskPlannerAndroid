package com.taskplanner.android.ui.main.screens

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskplanner.android.core.model.GoalStatus
import com.taskplanner.android.core.model.TaskPriority
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.data.local.entities.GoalEntity
import com.taskplanner.android.data.local.entities.GoalStepEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.ui.LocalAppGraph
import com.taskplanner.android.ui.main.goals.GoalsViewModel
import com.taskplanner.android.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GoalsScreen(
    padding: PaddingValues,
    userId: String,
    onNavigateToTasks: (LocalDate, String?) -> Unit
) {
    val graph = LocalAppGraph.current
    val vm: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory(userId, graph.goalRepository, graph.categoryRepository))
    val goals by vm.goals.collectAsState()
    val categories by vm.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val trimmedQuery = remember(searchQuery) { searchQuery.trim() }
    val filteredGoals = remember(goals, trimmedQuery) {
        if (trimmedQuery.isBlank()) goals
        else goals.filter { it.title.contains(trimmedQuery, ignoreCase = true) }
    }

    var expandedGoalId by remember { mutableStateOf<String?>(null) }

    var goalEditorMode: GoalEditorMode? by remember { mutableStateOf(null) }
    var stepEditorMode: StepEditorMode? by remember { mutableStateOf(null) }
    var stepToTask by remember { mutableStateOf<GoalStepEntity?>(null) }

    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    var stepToDelete by remember { mutableStateOf<GoalStepEntity?>(null) }
    var stepToDeleteTask by remember { mutableStateOf<GoalStepEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SystemBackground)
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "Цели",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Label,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

            when {
                goals.isEmpty() -> GoalsEmptyState()
                trimmedQuery.isNotBlank() && filteredGoals.isEmpty() -> GoalsSearchEmptyState()
                else -> {
                    GoalsList(
                        goals = filteredGoals,
                        expandedGoalId = expandedGoalId,
                        onToggleExpand = { goalId ->
                            expandedGoalId = if (expandedGoalId == goalId) null else goalId
                        },
                        categories = categories,
                        viewModel = vm,
                        onEditGoal = { goalEditorMode = GoalEditorMode.Edit(it) },
                        onDeleteGoal = { goalToDelete = it },
                        onAddStep = { goalId -> stepEditorMode = StepEditorMode.Create(goalId) },
                        onEditStep = { stepEditorMode = StepEditorMode.Edit(it) },
                        onDeleteStep = { stepToDelete = it },
                        onAddTaskFromStep = { stepToTask = it },
                        onGoToTask = { task ->
                            onNavigateToTasks(TimeUtils.localDateFromMillis(task.date), task.id)
                        },
                        onDeleteTaskFromStep = { stepToDeleteTask = it }
                    )
                }
            }
        }

        AddGoalButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            onClick = { goalEditorMode = GoalEditorMode.Create }
        )
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Удалить цель?") },
            text = { Text("Все шаги этой цели также будут удалены, но связанные задачи останутся.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteGoal(goal.id)
                        if (expandedGoalId == goal.id) expandedGoalId = null
                        goalToDelete = null
                    }
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { goalToDelete = null }) { Text("Отмена") } }
        )
    }

    stepToDelete?.let { step ->
        AlertDialog(
            onDismissRequest = { stepToDelete = null },
            title = { Text("Удалить шаг?") },
            text = { Text("Задача на день останется, но шаг цели удалится.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteStep(step.id)
                        stepToDelete = null
                    }
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { stepToDelete = null }) { Text("Отмена") } }
        )
    }

    stepToDeleteTask?.let { step ->
        AlertDialog(
            onDismissRequest = { stepToDeleteTask = null },
            title = { Text("Удалить задачу?") },
            text = { Text("Задача будет удалена из дня, но шаг цели останется.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteTaskFromStep(step.id)
                        stepToDeleteTask = null
                    }
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { stepToDeleteTask = null }) { Text("Отмена") } }
        )
    }

    goalEditorMode?.let { mode ->
        GoalEditorBottomSheet(
            mode = mode,
            onDismiss = { goalEditorMode = null },
            onSave = { title, description ->
                when (mode) {
                    is GoalEditorMode.Create -> vm.createGoal(title, description)
                    is GoalEditorMode.Edit -> vm.updateGoal(mode.goal.id, title, description)
                }
                goalEditorMode = null
            }
        )
    }

    stepEditorMode?.let { mode ->
        StepEditorBottomSheet(
            mode = mode,
            onDismiss = { stepEditorMode = null },
            onSave = { title, description ->
                when (mode) {
                    is StepEditorMode.Create -> vm.createStep(mode.goalId, title, description)
                    is StepEditorMode.Edit -> vm.updateStep(mode.step.id, title, description)
                }
                stepEditorMode = null
            }
        )
    }

    stepToTask?.let { step ->
        StepToTaskBottomSheet(
            step = step,
            categories = categories,
            onDismiss = { stepToTask = null },
            onCreate = { description, date, time, priority, hasReminder, reminderOffset, categoryId, imageData ->
                vm.createTaskFromStepDetailed(
                    stepId = step.id,
                    description = description,
                    date = date,
                    startTime = time,
                    priority = priority,
                    hasReminder = hasReminder,
                    reminderOffsetMinutes = reminderOffset,
                    categoryId = categoryId,
                    imageData = imageData,
                    onCreated = { taskId ->
                        stepToTask = null
                        onNavigateToTasks(date, taskId)
                    }
                )
            }
        )
    }
}

private sealed class GoalEditorMode {
    object Create : GoalEditorMode()
    data class Edit(val goal: GoalEntity) : GoalEditorMode()
}

private sealed class StepEditorMode {
    data class Create(val goalId: String) : StepEditorMode()
    data class Edit(val step: GoalStepEntity) : StepEditorMode()
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.SystemGroupedBackground)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = AppColors.GrayText, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("Поиск", color = AppColors.GrayText, fontSize = 16.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.Label, fontSize = 16.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Blue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GoalsList(
    goals: List<GoalEntity>,
    expandedGoalId: String?,
    onToggleExpand: (String) -> Unit,
    categories: List<CategoryEntity>,
    viewModel: GoalsViewModel,
    onEditGoal: (GoalEntity) -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit,
    onAddStep: (String) -> Unit,
    onEditStep: (GoalStepEntity) -> Unit,
    onDeleteStep: (GoalStepEntity) -> Unit,
    onAddTaskFromStep: (GoalStepEntity) -> Unit,
    onGoToTask: (TaskEntity) -> Unit,
    onDeleteTaskFromStep: (GoalStepEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(goals, key = { it.id }) { goal ->
            val isExpanded = expandedGoalId == goal.id
            val steps by if (isExpanded) {
                viewModel.stepsForGoal(goal.id).collectAsState(initial = emptyList())
            } else {
                remember { mutableStateOf(emptyList()) }
            }
            GoalCard(
                goal = goal,
                steps = steps,
                isExpanded = isExpanded,
                onToggleExpand = { onToggleExpand(goal.id) },
                viewModel = viewModel,
                categories = categories,
                onEditGoal = { onEditGoal(goal) },
                onDeleteGoal = { onDeleteGoal(goal) },
                onAddStep = { onAddStep(goal.id) },
                onEditStep = onEditStep,
                onDeleteStep = onDeleteStep,
                onAddTaskFromStep = onAddTaskFromStep,
                onGoToTask = onGoToTask,
                onDeleteTaskFromStep = onDeleteTaskFromStep
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    steps: List<GoalStepEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: GoalsViewModel,
    categories: List<CategoryEntity>,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onAddStep: () -> Unit,
    onEditStep: (GoalStepEntity) -> Unit,
    onDeleteStep: (GoalStepEntity) -> Unit,
    onAddTaskFromStep: (GoalStepEntity) -> Unit,
    onGoToTask: (TaskEntity) -> Unit,
    onDeleteTaskFromStep: (GoalStepEntity) -> Unit
) {
    val progress = goal.progressCached.coerceIn(0.0, 1.0)
    val completedSteps = remember(steps) { steps.count { it.isCompleted } }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    if (!goal.description.isNullOrBlank()) {
                        Text(
                            goal.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (goal.status == GoalStatus.COMPLETED.raw) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Завершена", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34C759))
                        }
                    }
                }

                CircularProgress(
                    progress = progress.toFloat(),
                    modifier = Modifier.size(50.dp)
                )
            }

            LinearProgressIndicator(
                progress = progress.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = if (progress >= 1.0) Color(0xFF34C759) else Color(0xFF0A84FF),
                trackColor = Color(0x1A000000)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "Шаги ($completedSteps/${steps.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    steps.forEach { step ->
                        GoalStepRow(
                            step = step,
                            viewModel = viewModel,
                            categories = categories,
                            onEditStep = { onEditStep(step) },
                            onDeleteStep = { onDeleteStep(step) },
                            onAddTask = { onAddTaskFromStep(step) },
                            onGoToTask = onGoToTask,
                            onDeleteTask = { onDeleteTaskFromStep(step) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    TextButton(onClick = onAddStep) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить шаг")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleExpand,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isExpanded) "Свернуть" else "Развернуть",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                var goalMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { goalMenuExpanded = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = AppColors.GrayText, modifier = Modifier.size(20.dp))
                    }
                    IosDropdownMenu(expanded = goalMenuExpanded, onDismissRequest = { goalMenuExpanded = false }) {
                        IosMenuItem("Редактировать", Icons.Filled.Edit, AppColors.Blue) { goalMenuExpanded = false; onEditGoal() }
                        IosMenuDivider()
                        IosMenuItem("Удалить", Icons.Filled.Delete, AppColors.Red) { goalMenuExpanded = false; onDeleteGoal() }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalStepRow(
    step: GoalStepEntity,
    viewModel: GoalsViewModel,
    categories: List<CategoryEntity>,
    onEditStep: () -> Unit,
    onDeleteStep: () -> Unit,
    onAddTask: () -> Unit,
    onGoToTask: (TaskEntity) -> Unit,
    onDeleteTask: () -> Unit
) {
    val linkedTask by viewModel.linkedTaskForStep(step.id).collectAsState(initial = null)
    var menuExpanded by remember(step.id) { mutableStateOf(false) }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleStep(step.id) }) {
                Icon(
                    imageVector = if (step.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (step.isCompleted) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (step.isCompleted) AppColors.GrayText else AppColors.Label
                    )
                    if (linkedTask != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.TaskAlt,
                            contentDescription = null,
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (!step.description.isNullOrBlank()) {
                    Text(
                        text = step.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.GrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = AppColors.GrayText)
                }
                IosDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (linkedTask == null) {
                        IosMenuItem("Добавить на день", Icons.Outlined.CalendarMonth, AppColors.Blue) { menuExpanded = false; onAddTask() }
                    } else {
                        IosMenuItem("Перейти к задаче", Icons.Outlined.CalendarMonth, AppColors.Blue) { menuExpanded = false; linkedTask?.let(onGoToTask) }
                        IosMenuDivider()
                        IosMenuItem("Удалить задачу", Icons.Filled.Delete, AppColors.Red) { menuExpanded = false; onDeleteTask() }
                    }
                    IosMenuDivider()
                    IosMenuItem("Редактировать шаг", Icons.Filled.Edit, AppColors.Blue) { menuExpanded = false; onEditStep() }
                    IosMenuDivider()
                    IosMenuItem("Удалить шаг", Icons.Filled.Delete, AppColors.Red) { menuExpanded = false; onDeleteStep() }
                }
            }
        }
    }
}

@Composable
private fun CircularProgress(progress: Float, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    val strokeWidth = 6.dp
    val trackColor = Color.Black.copy(alpha = 0.08f)
    val gradient = if (clamped >= 1f) {
        Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF34C759)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFFAF52DE)))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        if (clamped >= 1f) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(16.dp))
        } else {
            Text(
                text = "${(clamped * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddGoalButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
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
private fun GoalsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.TaskAlt,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.35f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Здесь пока пусто :(", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Нажмите на + как только у вас появится цель",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoalsSearchEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.35f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Попробуйте изменить запрос", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.75f))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GoalEditorBottomSheet(
    mode: GoalEditorMode,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(mode) { mutableStateOf(if (mode is GoalEditorMode.Edit) mode.goal.title else "") }
    var description by remember(mode) { mutableStateOf(if (mode is GoalEditorMode.Edit) (mode.goal.description ?: "") else "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 20.dp)
        ) {
            SheetHeader(
                title = if (mode is GoalEditorMode.Edit) "Редактировать цель" else "Новая цель",
                confirmText = "Сохранить",
                confirmEnabled = title.trim().isNotEmpty(),
                onCancel = onDismiss,
                onConfirm = { onSave(title.trim(), description.trim().ifBlank { null }) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionCard(title = "Задача") {
                    IosField(value = title, onValueChange = { title = it }, placeholder = "Название цели")
                    IosThinDivider()
                    IosField(value = description, onValueChange = { description = it }, placeholder = "Описание (необязательно)", singleLine = false, minLines = 3)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StepEditorBottomSheet(
    mode: StepEditorMode,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(mode) { mutableStateOf(if (mode is StepEditorMode.Edit) mode.step.title else "") }
    var description by remember(mode) { mutableStateOf(if (mode is StepEditorMode.Edit) (mode.step.description ?: "") else "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        dragHandle = null,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 20.dp)
                .statusBarsPadding()
        ) {
            SheetHeader(
                title = if (mode is StepEditorMode.Edit) "Редактировать шаг" else "Новый шаг",
                confirmText = if (mode is StepEditorMode.Edit) "Сохранить" else "Добавить",
                confirmEnabled = title.trim().isNotEmpty(),
                onCancel = onDismiss,
                onConfirm = { onSave(title.trim(), description.trim().ifBlank { null }) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionCard(title = "Шаг цели") {
                    IosField(value = title, onValueChange = { title = it }, placeholder = "Название шага")
                    IosThinDivider()
                    IosField(value = description, onValueChange = { description = it },
                        placeholder = "Описание (необязательно)", singleLine = false, minLines = 3)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StepToTaskBottomSheet(
    step: GoalStepEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onCreate: (
        description: String?,
        date: LocalDate,
        time: LocalTime?,
        priority: TaskPriority,
        hasReminder: Boolean,
        reminderOffsetMinutes: Int,
        categoryId: String?,
        imageData: ByteArray?
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var description by remember(step.id) { mutableStateOf(step.description ?: "") }
    var selectedDate by remember(step.id) { mutableStateOf(LocalDate.now()) }
    var timeEnabled by remember(step.id) { mutableStateOf(false) }
    var selectedTime by remember(step.id) { mutableStateOf<LocalTime?>(null) }
    var priority by remember(step.id) { mutableStateOf(TaskPriority.MEDIUM) }
    var hasReminder by remember(step.id) { mutableStateOf(false) }
    var reminderOffset by remember(step.id) { mutableIntStateOf(15) }
    var categoryId by remember(step.id) { mutableStateOf<String?>(null) }
    var imageData by remember(step.id) { mutableStateOf<ByteArray?>(null) }

    var showCalendar by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageData = uri?.let { readBytesFromUri(context, it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.SystemGroupedBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 20.dp)
        ) {
            SheetHeader(
                title = "Создать задачу",
                confirmText = "Создать",
                confirmEnabled = true,
                onCancel = onDismiss,
                onConfirm = {
                    onCreate(
                        description.trim().ifBlank { null },
                        selectedDate,
                        if (timeEnabled) selectedTime else null,
                        priority,
                        hasReminder && timeEnabled,
                        reminderOffset,
                        categoryId,
                        imageData
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
                    Text(step.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColors.Label)
                    IosThinDivider()
                    IosField(value = description, onValueChange = { description = it },
                        placeholder = "Описание (необязательно)", singleLine = false, minLines = 2)
                }

                SectionCard(title = "Дата и время") {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showCalendar = true }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Дата", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        Text(formatDate(selectedDate), color = AppColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    IosThinDivider()
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Указать время", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(
                            checked = timeEnabled,
                            onCheckedChange = { timeEnabled = it; if (!it) { selectedTime = null; hasReminder = false } },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green,
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA),
                                uncheckedBorderColor = Color.Transparent)
                        )
                    }
                    if (timeEnabled) {
                        IosThinDivider()
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            val cur = selectedTime ?: LocalTime.of(9, 0)
                            TimePickerDialog(context, { _, h, m -> selectedTime = LocalTime.of(h, m) }, cur.hour, cur.minute, true).show()
                        }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Время начала", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                            Text(selectedTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "09:00",
                                color = AppColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                SectionCard(title = "Категория") {
                    CategoryPicker(categories = categories, selectedCategoryId = categoryId, onSelected = { categoryId = it })
                }

                SectionCard(title = "Приоритет") {
                    PriorityPicker(selected = priority, onSelected = { priority = it })
                }

                SectionCard(title = "Напоминание") {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Включить напоминание", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(
                            checked = hasReminder && timeEnabled,
                            onCheckedChange = { if (timeEnabled) hasReminder = it },
                            enabled = timeEnabled,
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = AppColors.Green,
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E5EA),
                                uncheckedBorderColor = Color.Transparent)
                        )
                    }
                    if (hasReminder && timeEnabled) {
                        IosThinDivider()
                        ReminderOffsetPicker(selected = reminderOffset, onSelected = { reminderOffset = it })
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showCalendar) {
        CalendarDialog(
            initial = selectedDate,
            onDismiss = { showCalendar = false },
            onSelected = {
                selectedDate = it
                showCalendar = false
            }
        )
    }
}

@Composable
private fun SheetHeader(
    title: String,
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
        TextButton(onClick = onCancel) { Text("Отмена") }
        Spacer(modifier = Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) }
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
private fun PriorityPicker(selected: TaskPriority, onSelected: (TaskPriority) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        PriorityChip("Низкий", selected == TaskPriority.LOW) { onSelected(TaskPriority.LOW) }
        PriorityChip("Средний", selected == TaskPriority.MEDIUM) { onSelected(TaskPriority.MEDIUM) }
        PriorityChip("Высокий", selected == TaskPriority.HIGH) { onSelected(TaskPriority.HIGH) }
    }
}

@Composable
private fun PriorityChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() }
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ReminderOffsetPicker(selected: Int, onSelected: (Int) -> Unit) {
    val options = listOf(0, 5, 10, 15, 30, 60)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        options.forEach { minutes ->
            val label = if (minutes == 0) "В момент" else "За ${minutes} мин"
            val isSelected = selected == minutes
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onSelected(minutes) }
            ) {
                Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onSelected: (String?) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { menuExpanded = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Категория", modifier = Modifier.weight(1f))
        Text(selectedName ?: "Без категории", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
        DropdownMenuItem(
            text = { Text("Без категории") },
            onClick = {
                menuExpanded = false
                onSelected(null)
            }
        )
        categories.forEach { cat ->
            DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                    menuExpanded = false
                    onSelected(cat.id)
                }
            )
        }
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

private fun formatDate(date: LocalDate): String {
    val locale = Locale("ru")
    val month = date.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }
    return "${date.dayOfMonth} $month ${date.year}"
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Throwable) {
        null
    }
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
private fun IosThinDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.SeparatorLight))
}

@Composable
private fun IosDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = androidx.compose.ui.Modifier
            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .widthIn(min = 200.dp)
    ) { content() }
}

@Composable
private fun IosMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = AppColors.Label,
    onClick: () -> Unit
) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(title, color = tint, fontSize = 16.sp) },
        leadingIcon = { androidx.compose.material3.Icon(icon, null, tint = tint, modifier = androidx.compose.ui.Modifier.size(20.dp)) },
        onClick = onClick,
        modifier = androidx.compose.ui.Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun IosMenuDivider() {
    androidx.compose.material3.Divider(
        color = AppColors.SeparatorLight,
        thickness = 0.5.dp,
        modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp)
    )
}
