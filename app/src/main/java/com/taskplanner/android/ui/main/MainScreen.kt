package com.taskplanner.android.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskplanner.android.ui.main.screens.GoalsScreen
import com.taskplanner.android.ui.main.screens.ProfileScreen
import com.taskplanner.android.ui.main.screens.ScheduleScreen
import com.taskplanner.android.ui.main.screens.StatisticsScreen
import com.taskplanner.android.ui.main.screens.CategoriesScreen
import com.taskplanner.android.ui.main.screens.TasksScreen
import com.taskplanner.android.ui.theme.AppColors
import java.time.LocalDate

private enum class Tab(val title: String, val icon: ImageVector) {
    STATISTICS("Статистика", Icons.Filled.BarChart),
    GOALS("Цели", Icons.Filled.TrackChanges),
    TASKS("Задачи", Icons.Filled.Checklist),
    SCHEDULE("Расписание", Icons.Filled.CalendarMonth),
    PROFILE("Профиль", Icons.Filled.Person)
}

@Composable
fun MainScreen(userId: String) {
    var selected by remember { mutableStateOf(Tab.TASKS) }
    var showCategories by remember { mutableStateOf(false) }
    var tasksDateRequest by remember { mutableStateOf<LocalDate?>(null) }
    var tasksTaskIdRequest by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppColors.SystemBackground,
        bottomBar = {
            IosTabBar(
                selected = selected,
                onSelect = { selected = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (selected) {
                Tab.STATISTICS -> StatisticsScreen(PaddingValues(0.dp), userId)
                Tab.GOALS -> GoalsScreen(
                    padding = PaddingValues(0.dp),
                    userId = userId,
                    onNavigateToTasks = { date, taskId ->
                        tasksDateRequest = date
                        tasksTaskIdRequest = taskId
                        selected = Tab.TASKS
                    }
                )
                Tab.TASKS -> TasksScreen(
                    padding = PaddingValues(0.dp),
                    userId = userId,
                    initialDate = tasksDateRequest,
                    onInitialDateConsumed = { tasksDateRequest = null },
                    initialTaskId = tasksTaskIdRequest,
                    onInitialTaskIdConsumed = { tasksTaskIdRequest = null }
                )
                Tab.SCHEDULE -> ScheduleScreen(PaddingValues(0.dp), userId)
                Tab.PROFILE -> ProfileScreen(
                    padding = PaddingValues(0.dp),
                    userId = userId,
                    onOpenCategories = { showCategories = true }
                )
            }
        }
    }
    if (showCategories) {
        CategoriesScreen(userId = userId, onBack = { showCategories = false })
    }
}

@Composable
private fun IosTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(modifier = Modifier.background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.SeparatorLight))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Tab.values().forEach { tab ->
                IosTabItem(tab = tab, isSelected = tab == selected, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun IosTabItem(tab: Tab, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) AppColors.Blue else AppColors.GrayText
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = tab.icon, contentDescription = tab.title, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.size(2.dp))
        Text(
            text = tab.title,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
