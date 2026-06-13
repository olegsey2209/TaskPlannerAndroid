package com.taskplanner.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.taskplanner.android.ui.TaskPlannerApp
import com.taskplanner.android.ui.theme.TaskPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskPlannerTheme {
                TaskPlannerApp()
            }
        }
    }
}

