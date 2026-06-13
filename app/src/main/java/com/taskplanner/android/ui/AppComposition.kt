package com.taskplanner.android.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.taskplanner.android.di.AppGraph

val LocalAppGraph = staticCompositionLocalOf<AppGraph> {
    error("AppGraph not provided")
}

