package com.taskplanner.android.di

import android.content.Context
import com.taskplanner.android.data.local.AppDatabase
import com.taskplanner.android.data.prefs.UserPrefs
import com.taskplanner.android.data.repository.CategoryRepository
import com.taskplanner.android.data.repository.GoalRepository
import com.taskplanner.android.data.repository.RecurrenceRepository
import com.taskplanner.android.data.repository.StatisticsRepository
import com.taskplanner.android.data.repository.TaskRepository
import com.taskplanner.android.data.repository.TemplateRepository
import com.taskplanner.android.data.repository.UserRepository
import com.taskplanner.android.sync.SyncEngine
import com.taskplanner.android.sync.SyncTrigger

class AppGraph(context: Context) {
    private val db = AppDatabase.get(context)

    val userPrefs = UserPrefs(context)

    val syncEngine = SyncEngine(context.applicationContext, db)

    
    val firebaseApiKey: String = com.google.firebase.FirebaseApp.getInstance().options.apiKey

    
    @Volatile
    private var currentUserIdForTrigger: String? = null

    fun setCurrentUserIdForTrigger(userId: String?) {
        currentUserIdForTrigger = userId
    }

    private val trigger: SyncTrigger = SyncTrigger {
        currentUserIdForTrigger?.let { uid ->
            if (uid.isNotBlank()) syncEngine.triggerSync(uid)
        }
    }

    val userRepository = UserRepository(db.userProfileDao(), trigger)
    val categoryRepository = CategoryRepository(db.categoryDao(), trigger)
    val taskRepository = TaskRepository(db.taskDao(), db.goalDao(), db.goalStepDao(), db.recurrenceRuleDao(), trigger, context)
    val goalRepository = GoalRepository(db.goalDao(), db.goalStepDao(), db.taskDao(), trigger)
    val templateRepository = TemplateRepository(db.scheduleTemplateDao(), db.scheduleTemplateItemDao(), db.templateApplicationDao(), db.taskDao(), trigger)
    val recurrenceRepository = RecurrenceRepository(db.recurrenceRuleDao(), db.taskDao(), trigger)
    val statisticsRepository = StatisticsRepository(db.taskDao(), db.goalDao(), db.categoryDao())

    fun observeUserProfile(userId: String) = db.userProfileDao().observeById(userId)
}
