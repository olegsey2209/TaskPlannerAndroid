package com.taskplanner.android.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.taskplanner.android.MainActivity
import com.taskplanner.android.R
import com.taskplanner.android.core.model.TaskStatus
import com.taskplanner.android.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val userId = intent.getStringExtra("userId") ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = AppDatabase.get(context).taskDao().getByIdAny(userId, taskId)
                if (task == null ||
                    task.deletedAt != null ||
                    !task.hasReminder ||
                    task.status != TaskStatus.PLANNED.raw
                ) {
                    NotificationHelper.cancelReminder(context, taskId)
                    return@launch
                }

                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("taskId", taskId)
                }
                val tapPi = PendingIntent.getActivity(
                    context, taskId.hashCode(), tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val body = when {
                    task.reminderOffsetMinutes == 0 -> "Сейчас"
                    task.reminderOffsetMinutes < 60 -> "Через ${task.reminderOffsetMinutes} минут"
                    else -> "Через ${task.reminderOffsetMinutes / 60} ч"
                }

                val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                    .setColor(0xFF4D78F6.toInt())
                    .setContentTitle(task.title)
                    .setContentText(if (!task.description.isNullOrBlank()) task.description else body)
                    .setSubText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(tapPi)
                    .build()

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(taskId.hashCode(), notification)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
