package com.taskplanner.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.taskplanner.android.di.AppGraph
import com.taskplanner.android.ui.auth.AuthScreen
import com.taskplanner.android.ui.main.MainScreen

@Composable
fun TaskPlannerApp() {
    val context = LocalContext.current
    val graph = remember { AppGraph(context) }

    var isBootstrapping by remember { mutableStateOf(true) }
    val currentUserId by graph.userPrefs.observeCurrentUserId().collectAsState(initial = null)

    LaunchedEffect(currentUserId) {
        isBootstrapping = false
        val uid = currentUserId
        if (!uid.isNullOrBlank()) {
            graph.setCurrentUserIdForTrigger(uid)
            graph.syncEngine.forceSync(uid)
            
            graph.recurrenceRepository.generateUpcomingTasksIfNeeded(uid)
        } else {
            graph.setCurrentUserIdForTrigger(null)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppGraph provides graph) {
        if (isBootstrapping) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@CompositionLocalProvider
        }

        if (currentUserId.isNullOrBlank()) {
            AuthScreen()
        } else {
            MainScreen(userId = currentUserId!!)
        }
    }
}
