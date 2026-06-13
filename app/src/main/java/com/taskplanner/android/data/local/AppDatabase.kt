package com.taskplanner.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taskplanner.android.data.local.dao.CategoryDao
import com.taskplanner.android.data.local.dao.GoalDao
import com.taskplanner.android.data.local.dao.GoalStepDao
import com.taskplanner.android.data.local.dao.RecurrenceRuleDao
import com.taskplanner.android.data.local.dao.ScheduleTemplateDao
import com.taskplanner.android.data.local.dao.ScheduleTemplateItemDao
import com.taskplanner.android.data.local.dao.TaskDao
import com.taskplanner.android.data.local.dao.TemplateApplicationDao
import com.taskplanner.android.data.local.dao.UserProfileDao
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.data.local.entities.GoalEntity
import com.taskplanner.android.data.local.entities.GoalStepEntity
import com.taskplanner.android.data.local.entities.RecurrenceRuleEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateEntity
import com.taskplanner.android.data.local.entities.ScheduleTemplateItemEntity
import com.taskplanner.android.data.local.entities.TaskEntity
import com.taskplanner.android.data.local.entities.TemplateApplicationEntity
import com.taskplanner.android.data.local.entities.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        GoalStepEntity::class,
        TaskEntity::class,
        ScheduleTemplateEntity::class,
        ScheduleTemplateItemEntity::class,
        TemplateApplicationEntity::class,
        RecurrenceRuleEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun goalStepDao(): GoalStepDao
    abstract fun taskDao(): TaskDao
    abstract fun scheduleTemplateDao(): ScheduleTemplateDao
    abstract fun scheduleTemplateItemDao(): ScheduleTemplateItemDao
    abstract fun templateApplicationDao(): TemplateApplicationDao
    abstract fun recurrenceRuleDao(): RecurrenceRuleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE goal_steps ADD COLUMN plannedDate INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN imageBase64 TEXT")
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_planner.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
