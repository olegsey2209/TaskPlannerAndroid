package com.taskplanner.android.data.repository

import com.taskplanner.android.core.model.GoalStatus
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.core.util.TimeUtils
import com.taskplanner.android.data.local.dao.CategoryDao
import com.taskplanner.android.data.local.dao.GoalDao
import com.taskplanner.android.data.local.dao.TaskDao
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

enum class StatisticsPeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

data class StatisticsData(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionRate: Double = 0.0,
    val totalGoals: Int = 0,
    val completedGoals: Int = 0,
    val goalsProgress: Double = 0.0,
    val tasksByPriority: Map<Int, Int> = emptyMap(),
    val tasksByCategory: Map<String, Int> = emptyMap(),
    val dailyCompletion: Map<LocalDate, Int> = emptyMap(),
    val hourlyCompletion: Map<Int, Int>? = null,
    val goalProgress: List<Pair<String, Double>> = emptyList()
)

class StatisticsRepository(
    private val taskDao: TaskDao,
    private val goalDao: GoalDao,
    private val categoryDao: CategoryDao
) {
    suspend fun getStatistics(userId: String, period: StatisticsPeriod): StatisticsData {
        val now = LocalDate.now()
        val startDate = when (period) {
            StatisticsPeriod.DAY -> now
            StatisticsPeriod.WEEK -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            StatisticsPeriod.MONTH -> now.withDayOfMonth(1)
            StatisticsPeriod.YEAR -> now.withDayOfYear(1)
        }
        val endExclusive = when (period) {
            StatisticsPeriod.DAY -> startDate.plusDays(1)
            StatisticsPeriod.WEEK -> startDate.plusWeeks(1)
            StatisticsPeriod.MONTH -> startDate.plusMonths(1)
            StatisticsPeriod.YEAR -> startDate.plusYears(1)
        }

        val startMillis = TimeUtils.startOfDayMillis(startDate)
        val endMillis = TimeUtils.startOfDayMillis(endExclusive)

        val tasks = taskDao.getForRange(userId, startMillis, endMillis)
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED.raw }
        val completionRate = if (totalTasks > 0) completedTasks.toDouble() / totalTasks.toDouble() else 0.0

        val tasksByPriority = (0..2).associateWith { p -> tasks.count { it.priority == p } }

        val categoriesById = categoryDao.getAll(userId).associateBy { it.id }
        val tasksByCategory = buildMap<String, Int> {
            tasks.forEach { task ->
                val name = task.categoryId?.let { categoriesById[it]?.name } ?: "Без категории"
                put(name, (get(name) ?: 0) + 1)
            }
        }

        val dailyCompletion = LinkedHashMap<LocalDate, Int>()
        var cursor = startDate
        while (cursor.isBefore(endExclusive)) {
            val dayStart = TimeUtils.startOfDayMillis(cursor)
            val dayEnd = TimeUtils.startOfDayMillis(cursor.plusDays(1))
            val count = tasks.count { t ->
                t.status == TaskStatus.COMPLETED.raw && t.date >= dayStart && t.date < dayEnd
            }
            dailyCompletion[cursor] = count
            cursor = cursor.plusDays(1)
        }

        val hourlyCompletion = if (period == StatisticsPeriod.DAY) {
            val dayStart = TimeUtils.startOfDayMillis(startDate)
            (0..23).associateWith { hour ->
                val hourStart = dayStart + hour * 3_600_000L
                val hourEnd = hourStart + 3_600_000L
                tasks.count { t ->
                    t.status == TaskStatus.COMPLETED.raw &&
                    (t.completedAt ?: t.date).let { ts -> ts >= hourStart && ts < hourEnd }
                }
            }
        } else null

        val goals = goalDao.getAll(userId)
        val totalGoals = goals.size
        val completedGoals = goals.count { it.status == GoalStatus.COMPLETED.raw }
        val goalsProgress = if (totalGoals > 0) goals.sumOf { it.progressCached } / totalGoals.toDouble() else 0.0

        val goalProgress = goals.map { it.title to it.progressCached }

        return StatisticsData(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            hourlyCompletion = hourlyCompletion,
            completionRate = completionRate,
            totalGoals = totalGoals,
            completedGoals = completedGoals,
            goalsProgress = goalsProgress,
            tasksByPriority = tasksByPriority,
            tasksByCategory = tasksByCategory,
            dailyCompletion = dailyCompletion,
            goalProgress = goalProgress
        )
    }
}

