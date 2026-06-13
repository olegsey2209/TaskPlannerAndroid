package com.taskplanner.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AppColors.Blue,
    onPrimary = Color.White,
    secondary = AppColors.GrayText,
    onSecondary = Color.White,
    background = AppColors.SystemBackground,
    onBackground = AppColors.Label,
    surface = AppColors.SystemBackground,
    onSurface = AppColors.Label,
    surfaceVariant = AppColors.SecondarySystemBackground,
    onSurfaceVariant = AppColors.SecondaryLabel,
    outline = AppColors.SeparatorLight,
    error = AppColors.Red,
    
    surfaceTint = Color.Transparent
)

@Composable
fun TaskPlannerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
