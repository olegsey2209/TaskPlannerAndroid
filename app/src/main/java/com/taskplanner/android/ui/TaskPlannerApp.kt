package com.taskplanner.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.taskplanner.android.di.AppGraph
import com.taskplanner.android.ui.auth.AuthScreen
import com.taskplanner.android.ui.main.MainScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TaskPlannerApp() {
    val context = LocalContext.current
    val graph = remember { AppGraph(context) }
    val lifecycleOwner = remember(context) { context as? LifecycleOwner }
    val scope = rememberCoroutineScope()

    var isBootstrapping by remember { mutableStateOf(true) }
    var lastForegroundSyncAt by remember { mutableStateOf(0L) }
    val currentUserId by graph.userPrefs.observeCurrentUserId().collectAsState(initial = null)

    LaunchedEffect(currentUserId) {
        val uid = currentUserId
        if (!uid.isNullOrBlank()) {
            if (graph.validateCurrentAccount(uid)) {
                graph.restoreCurrentUserNameFromAuth(uid)
                graph.setCurrentUserIdForTrigger(uid)
                graph.syncEngine.forceSync(uid)
                graph.recurrenceRepository.generateUpcomingTasksIfNeeded(uid)
                isBootstrapping = false

                while (true) {
                    delay(15_000L)
                    if (!graph.validateCurrentAccount(uid)) break
                }
            } else {
                isBootstrapping = false
            }
        } else {
            graph.setCurrentUserIdForTrigger(null)
            isBootstrapping = false
        }
    }

    DisposableEffect(lifecycleOwner, currentUserId) {
        val owner = lifecycleOwner
        if (owner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

                val uid = currentUserId
                if (uid.isNullOrBlank()) return@LifecycleEventObserver

                val now = System.currentTimeMillis()
                if (now - lastForegroundSyncAt < 2_000L) return@LifecycleEventObserver
                lastForegroundSyncAt = now

                scope.launch {
                    if (graph.validateCurrentAccount(uid)) {
                        graph.restoreCurrentUserNameFromAuth(uid)
                        graph.setCurrentUserIdForTrigger(uid)
                        graph.syncEngine.forceSync(uid)
                    }
                }
            }

            owner.lifecycle.addObserver(observer)
            onDispose {
                owner.lifecycle.removeObserver(observer)
            }
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
            key(currentUserId) {
                MainScreen(userId = currentUserId!!)
            }
        }
    }
}
