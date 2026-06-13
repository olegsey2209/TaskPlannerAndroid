package com.taskplanner.android

import android.app.Application
import com.google.firebase.FirebaseApp
import com.taskplanner.android.notifications.NotificationHelper

class TaskPlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationHelper.createChannel(this)
    }
}
