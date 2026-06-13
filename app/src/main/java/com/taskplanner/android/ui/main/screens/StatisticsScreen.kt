package com.taskplanner.android.ui.main.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskplanner.android.data.repository.StatisticsPeriod
import com.taskplanner.android.ui.LocalAppGraph
import com.taskplanner.android.ui.main.statistics.StatisticsViewModel
import com.taskplanner.android.ui.theme.AppColors
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(padding: PaddingValues, userId: String) {
    val graph = LocalAppGraph.current
    val vm: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.Factory(userId, graph.statisticsRepository)
    )

    val loading by vm.loading.collectAsState()
    val data by vm.data.collectAsState()
    val period by vm.selectedPeriod.collectAsState()

    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp)) {
                Text(
                    text = "Статистика",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Label,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        item {
            PeriodSelector(selected = period, onSelect = vm::setPeriod)
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            return@LazyColumn
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Задачи",
                    value = "${data.totalTasks}",
                    subtitle = "Выполнено: ${data.completedTasks}",
                    progress = data.completionRate,
                    color = AppColors.Blue,
                    icon = Icons.Filled.CheckCircle
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Цели",
                    value = "${data.completedGoals}/${data.totalGoals}",
                    subtitle = "${(data.goalsProgress * 100).roundToInt()}% выполнено",
                    progress = data.goalsProgress,
                    color = Color(0xFF34C759),
                    icon = Icons.Outlined.TrackChanges
                )
            }
        }

        val hasChartData = data.hourlyCompletion?.values?.any { it > 0 } == true ||
            data.dailyCompletion.values.any { it > 0 }
        if (hasChartData || data.dailyCompletion.isNotEmpty()) {
            item {
                ChartCard(title = "Динамика выполнения") {
                    val hourly = data.hourlyCompletion
                    if (hourly != null) {
                        HourlyBars(hourly = hourly)
                    } else {
                        DailyBars(daily = data.dailyCompletion, period = period)
                    }
                }
            }
        }

        if (data.tasksByPriority.isNotEmpty()) {
            item {
                ChartCard(title = "По приоритету") {
                    PriorityBars(counts = data.tasksByPriority)
                }
            }
        }

        if (data.tasksByCategory.isNotEmpty()) {
            item {
                ChartCard(title = "По категориям") {
                    val sorted = data.tasksByCategory.entries.sortedByDescending { it.value }
                    sorted.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.key, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${entry.value}", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text("задач", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }

        if (data.totalGoals > 0) {
            item {
                ChartCard(title = "Прогресс по целям") {
                    val list = data.goalProgress.take(5)
                    list.forEach { (title, progress) ->
                        GoalProgressRow(title = title, progress = progress)
                    }
                    val remaining = data.goalProgress.size - list.size
                    if (remaining > 0) {
                        Text(
                            "и еще $remaining...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: StatisticsPeriod, onSelect: (StatisticsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SystemGroupedBackground)
            .padding(2.dp)
    ) {
        PeriodChip("День", selected == StatisticsPeriod.DAY) { onSelect(StatisticsPeriod.DAY) }
        PeriodChip("Неделя", selected == StatisticsPeriod.WEEK) { onSelect(StatisticsPeriod.WEEK) }
        PeriodChip("Месяц", selected == StatisticsPeriod.MONTH) { onSelect(StatisticsPeriod.MONTH) }
        PeriodChip("Год", selected == StatisticsPeriod.YEAR) { onSelect(StatisticsPeriod.YEAR) }
    }
}

@Composable
private fun RowScope.PeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(7.dp))
            .then(
                if (selected) Modifier
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(7.dp))
                    .background(Color.White)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = AppColors.Label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    progress: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Card(
        modifier = modifier
            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("${(progress * 100).roundToInt()}%", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppColors.Label)
            Text(subtitle, fontSize = 13.sp, color = AppColors.Label)
            Text(title, fontSize = 13.sp, color = AppColors.GrayText)
            LinearProgressIndicator(
                progress = progress.toFloat().coerceIn(0f, 1f),
                color = color,
                trackColor = Color(0x1A000000),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .border(BorderStroke(0.5.dp, AppColors.SeparatorLight), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun DailyBars(daily: Map<java.time.LocalDate, Int>, period: com.taskplanner.android.data.repository.StatisticsPeriod) {
    when (period) {
        com.taskplanner.android.data.repository.StatisticsPeriod.WEEK -> {
            // Неделя — 7 баров, подписи дней
            val entries = daily.entries.toList()
            val max = (entries.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                entries.forEach { (date, count) ->
                    val h = (count.toFloat() / max).coerceIn(0f, 1f)
                    val label = date.format(DateTimeFormatter.ofPattern("EE", java.util.Locale("ru"))).take(2).replaceFirstChar { it.uppercase() }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        if (count > 0) Text("$count", fontSize = 9.sp, color = AppColors.Blue, fontWeight = FontWeight.SemiBold)
                        else Spacer(Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height((110 * h).dp.coerceAtLeast(if (count > 0) 6.dp else 2.dp))
                            .background(if (count > 0) MaterialTheme.colorScheme.primary else Color(0xFFE5E5EA), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }
        }
        com.taskplanner.android.data.repository.StatisticsPeriod.MONTH -> {
            // Месяц — группируем по неделям (4-5 баров)
            val byWeek = daily.entries.groupBy { it.key.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()) }
                .toSortedMap()
                .map { (_, entries) -> entries.minOf { it.key } to entries.sumOf { it.value } }
            val max = (byWeek.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                byWeek.forEach { (weekStart, count) ->
                    val h = (count.toFloat() / max).coerceIn(0f, 1f)
                    val label = weekStart.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale("ru")))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        if (count > 0) Text("$count", fontSize = 9.sp, color = AppColors.Blue, fontWeight = FontWeight.SemiBold)
                        else Spacer(Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height((110 * h).dp.coerceAtLeast(if (count > 0) 6.dp else 2.dp))
                            .background(if (count > 0) MaterialTheme.colorScheme.primary else Color(0xFFE5E5EA), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 8.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }
        }
        com.taskplanner.android.data.repository.StatisticsPeriod.YEAR -> {
            // Год — группируем по месяцам (12 баров)
            val byMonth = daily.entries.groupBy { it.key.monthValue }
                .toSortedMap()
                .map { (month, entries) -> month to entries.sumOf { it.value } }
            val max = (byMonth.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
            val monthNames = listOf("Янв","Фев","Мар","Апр","Май","Июн","Июл","Авг","Сен","Окт","Ноя","Дек")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                byMonth.forEach { (month, count) ->
                    val h = (count.toFloat() / max).coerceIn(0f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        if (count > 0) Text("$count", fontSize = 8.sp, color = AppColors.Blue, fontWeight = FontWeight.SemiBold)
                        else Spacer(Modifier.height(13.dp))
                        Box(modifier = Modifier.fillMaxWidth().height((110 * h).dp.coerceAtLeast(if (count > 0) 6.dp else 2.dp))
                            .background(if (count > 0) MaterialTheme.colorScheme.primary else Color(0xFFE5E5EA), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        Spacer(Modifier.height(4.dp))
                        Text(monthNames.getOrElse(month - 1) { "" }, fontSize = 8.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }
        }
        else -> {
            // День — fallback (должен использоваться HourlyBars)
            val entries = daily.entries.toList()
            val max = (entries.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                entries.forEach { (date, count) ->
                    val h = (count.toFloat() / max).coerceIn(0f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxWidth().height((110 * h).dp.coerceAtLeast(2.dp))
                            .background(if (count > 0) MaterialTheme.colorScheme.primary else Color(0xFFE5E5EA), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        Spacer(Modifier.height(4.dp))
                        Text(date.format(DateTimeFormatter.ofPattern("d.MM")), fontSize = 8.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyBars(hourly: Map<Int, Int>) {
    val max = (hourly.values.maxOrNull() ?: 0).coerceAtLeast(1)
    // Показываем только часы 0,6,12,18 как метки
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        (0..23).forEach { hour ->
            val count = hourly[hour] ?: 0
            val h = (count.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            val label = if (hour % 6 == 0) "%02d".format(hour) else ""
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((100 * h).dp.coerceAtLeast(if (count > 0) 4.dp else 1.dp))
                        .background(
                            if (count > 0) MaterialTheme.colorScheme.primary else Color(0xFFE5E5EA),
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(label, fontSize = 8.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PriorityBars(counts: Map<Int, Int>) {
    val items = listOf(
        Triple("Низкий", counts[0] ?: 0, Color(0xFF34C759)),
        Triple("Средний", counts[1] ?: 0, Color(0xFFFF9500)),
        Triple("Высокий", counts[2] ?: 0, Color(0xFFFF3B30))
    )
    val max = (items.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val maxH = 160.dp

    Row(
        modifier = Modifier.fillMaxWidth().height(maxH + 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { (label, count, color) ->
            val frac = (count.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.width(80.dp).fillMaxHeight()
            ) {
                Text("$count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.Label)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height((maxH * frac).coerceAtLeast(4.dp))
                        .background(color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 11.sp, color = AppColors.GrayText)
            }
        }
    }
}

@Composable
private fun GoalProgressRow(title: String, progress: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            CircularProgressIndicator(
                progress = 1f,
                color = Color(0xFFE5E5EA),
                strokeWidth = 4.dp,
                modifier = Modifier.size(44.dp)
            )
            CircularProgressIndicator(
                progress = progress.toFloat().coerceIn(0f, 1f),
                color = Color(0xFF5856D6),
                strokeWidth = 4.dp,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
