package com.taskplanner.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    
    val Blue = Color(0xFF007AFF)
    val Purple = Color(0xFFAF52DE)
    val Green = Color(0xFF34C759)
    val Orange = Color(0xFFFF9500)
    val Red = Color(0xFFFF3B30)
    val Yellow = Color(0xFFFFCC00)
    val Pink = Color(0xFFFF2D55)
    val Teal = Color(0xFF5AC8FA)
    val Indigo = Color(0xFF5856D6)

    
    val Label = Color(0xFF000000)
    val SecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.60f)
    val TertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.30f)
    val GrayText = Color(0xFF8E8E93)
    val Separator = Color(0xFFC6C6C8)
    val SeparatorLight = Color(0xFFE5E5EA)

    
    
    val SystemBackground = Color(0xFFFFFFFF)
    val SecondarySystemBackground = Color(0xFFFFFFFF)
    val TertiarySystemBackground = Color(0xFFFFFFFF)
    
    val SystemGroupedBackground = Color(0xFFF2F2F7)

    
    val FieldBackground = Color(0x14000000)

    
    val PriorityLow = Green
    val PriorityMedium = Orange
    val PriorityHigh = Red

    
    val PriorityLowBg = Color(0x1F34C759)    
    val PriorityMediumBg = Color(0x1FFF9500) 
    val PriorityHighBg = Color(0x1FFF3B30)   

    
    val TaskCardLowBg = Color(0xFFE8F8EC)    
    val TaskCardMediumBg = Color(0xFFFFF4D6) 
    val TaskCardHighBg = Color(0xFFFFE4E1)   
}

val AccentGradient: Brush
    get() = Brush.linearGradient(colors = listOf(AppColors.Blue, AppColors.Purple))

val FabGradient: Brush
    get() = Brush.linearGradient(colors = listOf(Color(0xFF4A8AF4), Color(0xFFA251E3)))
