package com.taskplanner.android.di

import android.app.NotificationManager
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.taskplanner.android.data.local.AppDatabase
import com.taskplanner.android.data.prefs.UserPrefs
import com.taskplanner.android.data.repository.CategoryRepository
import com.taskplanner.android.data.repository.GoalRepository
import com.taskplanner.android.data.repository.RecurrenceRepository
import com.taskplanner.android.data.repository.StatisticsRepository
import com.taskplanner.android.data.repository.TaskRepository
import com.taskplanner.android.data.repository.TemplateRepository
import com.taskplanner.android.data.repository.UserRepository
import com.taskplanner.android.notifications.NotificationHelper
import com.taskplanner.android.sync.SyncEngine
import com.taskplanner.android.sync.SyncTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)

    val userPrefs = UserPrefs(appContext)

    val syncEngine = SyncEngine(appContext, db)

    
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
    val taskRepository = TaskRepository(db.taskDao(), db.goalDao(), db.goalStepDao(), db.recurrenceRuleDao(), trigger, appContext)
    val goalRepository = GoalRepository(db.goalDao(), db.goalStepDao(), db.taskDao(), trigger, appContext)
    val templateRepository = TemplateRepository(db.scheduleTemplateDao(), db.scheduleTemplateItemDao(), db.templateApplicationDao(), db.taskDao(), trigger)
    val recurrenceRepository = RecurrenceRepository(db.recurrenceRuleDao(), db.taskDao(), trigger)
    val statisticsRepository = StatisticsRepository(db.taskDao(), db.goalDao(), db.categoryDao())

    fun observeUserProfile(userId: String) = db.userProfileDao().observeById(userId)

    suspend fun restoreCurrentUserNameFromAuth(userId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.uid != userId) return

        val displayName = user.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: return
        userRepository.getOrCreateUser(
            uid = userId,
            email = user.email,
            username = displayName
        )
    }

    suspend fun clearDeletedAccountSession(userId: String) {
        setCurrentUserIdForTrigger(null)

        withContext(Dispatchers.IO) {
            db.taskDao().getAllForUser(userId).forEach { task ->
                NotificationHelper.cancelReminder(appContext, task.id)
            }
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            db.clearAllTables()
        }

        userPrefs.clearCurrentUserId()
    }

    suspend fun validateCurrentAccount(userId: String): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null || user.uid != userId) {
            clearDeletedAccountSession(userId)
            return false
        }

        return try {
            user.reload().await()
            true
        } catch (error: FirebaseAuthException) {
            val invalidAccountCodes = setOf(
                "ERROR_USER_NOT_FOUND",
                "ERROR_USER_DISABLED",
                "ERROR_INVALID_USER_TOKEN",
                "ERROR_USER_TOKEN_EXPIRED"
            )

            if (error.errorCode in invalidAccountCodes) {
                auth.signOut()
                clearDeletedAccountSession(userId)
                false
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
    }
}
