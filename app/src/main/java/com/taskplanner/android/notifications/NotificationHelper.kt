package com.taskplanner.android.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.data.local.entities.TaskEntity

object NotificationHelper {

    const val CHANNEL_ID = "task_reminders"
    const val CHANNEL_NAME = "Напоминания о задачах"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о предстоящих задачах"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, task: TaskEntity) {
        val startTimeMillis = task.startTime ?: return
        if (!task.hasReminder || task.deletedAt != null || task.status != TaskStatus.PLANNED.raw) {
            cancelReminder(context, task.id)
            return
        }

        val offsetMs = task.reminderOffsetMinutes * 60_000L
        val triggerAt = startTimeMillis - offsetMs

        if (triggerAt <= System.currentTimeMillis()) {
            cancelReminder(context, task.id)
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("userId", task.userId)
            putExtra("taskTitle", task.title ?: "")
            putExtra("taskDescription", task.description ?: "")
            putExtra("reminderOffsetMinutes", task.reminderOffsetMinutes)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(taskId.hashCode())
    }
}
