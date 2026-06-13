package com.taskplanner.android.ui.main.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.ui.LocalAppGraph
import com.taskplanner.android.ui.main.profile.CategoriesViewModel
import com.taskplanner.android.ui.theme.AccentGradient
import com.taskplanner.android.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(padding: PaddingValues, userId: String, onOpenCategories: () -> Unit = {}) {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()

    val vm: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(userId, graph.categoryRepository))
    val categories by vm.categories.collectAsState()
    val userProfile by remember(userId) { graph.observeUserProfile(userId) }.collectAsState(initial = null)

    val isSyncing by graph.syncEngine.isSyncing.collectAsState()
    val lastSyncMillis by graph.syncEngine.observeLastSyncDate().collectAsState(initial = null)
    val isOnline by graph.syncEngine.isOnline.collectAsState()
    val lastError by graph.syncEngine.lastError.collectAsState()

    var syncExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SystemGroupedBackground)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        ProfileCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(AccentGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = userProfile?.username?.takeIf { it.isNotBlank() }
                        ?: userProfile?.email?.substringBefore('@') ?: "Пользователь"
                    Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = AppColors.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!userProfile?.email.isNullOrBlank()) {
                        Text(userProfile?.email ?: "", color = AppColors.GrayText, fontSize = 14.sp, maxLines = 1)
                    }
                }
            }
        }

        
        ProfileCard(onClick = onOpenCategories) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Label, null, tint = AppColors.Blue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Управление категориями", fontSize = 16.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = AppColors.GrayText, modifier = Modifier.size(20.dp))
            }
        }

        
        ProfileCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { syncExpanded = !syncExpanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Синхронизация", fontSize = 16.sp, color = AppColors.Label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(
                        if (syncExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        null, tint = AppColors.Blue, modifier = Modifier.size(22.dp)
                    )
                }
                AnimatedVisibility(visible = syncExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp)
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Wifi, null, tint = AppColors.GrayText, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Статус", fontSize = 15.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                            Text(if (isOnline) "Онлайн" else "Офлайн", fontSize = 15.sp, color = if (isOnline) AppColors.Green else AppColors.Red, fontWeight = FontWeight.Medium)
                        }
                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessTime, null, tint = AppColors.GrayText, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Последняя синх.", fontSize = 15.sp, color = AppColors.Label, modifier = Modifier.weight(1f))
                            Text(
                                lastSyncMillis?.let { formatSyncDateShort(it) } ?: "Никогда",
                                fontSize = 13.sp, color = AppColors.GrayText, fontWeight = FontWeight.Medium
                            )
                        }
                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { if (!isSyncing) graph.syncEngine.forceSync(userId) }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isSyncing) "Синхронизация…" else "Синхронизировать сейчас",
                                fontSize = 15.sp,
                                color = if (isSyncing) AppColors.GrayText else AppColors.Blue
                            )
                        }
                    }
                }
            }
        }

        
        ProfileCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { accountExpanded = !accountExpanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Аккаунт", fontSize = 16.sp, color = AppColors.Label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(
                        if (accountExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        null, tint = AppColors.Blue, modifier = Modifier.size(22.dp)
                    )
                }
                AnimatedVisibility(visible = accountExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showChangePassword = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Key, null, tint = AppColors.Label, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Изменить пароль", fontSize = 16.sp, color = AppColors.Label)
                        }
                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showLogoutConfirm = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ExitToApp, null, tint = AppColors.Blue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Выйти из аккаунта", fontSize = 16.sp, color = AppColors.Red)
                        }
                        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showDeleteAccountConfirm = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Delete, null, tint = AppColors.Red, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Удалить аккаунт", fontSize = 16.sp, color = AppColors.Red)
                        }
                    }
                }
            }
        }

                Spacer(Modifier.height(16.dp))
    }

    

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы можете снова войти в любое время.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    scope.launch {
                        FirebaseAuth.getInstance().signOut()
                        graph.setCurrentUserIdForTrigger(null)
                        graph.userPrefs.setCurrentUserId("")
                    }
                }) { Text("Выйти", color = AppColors.Red) }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Отмена") } }
        )
    }
    if (showChangePassword) {
        Box(modifier = Modifier.fillMaxSize()) {
            ChangePasswordDialog(onDismiss = { showChangePassword = false })
        }
    }

    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("Удалить аккаунт?") },
            text = { Text("Все данные будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountConfirm = false
                    scope.launch {
                        FirebaseAuth.getInstance().currentUser?.delete()
                        graph.setCurrentUserIdForTrigger(null)
                        graph.userPrefs.setCurrentUserId("")
                    }
                }) { Text("Удалить", color = AppColors.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountConfirm = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun ProfileCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFFFFF))
            .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

@Composable
private fun CategoryRow(category: CategoryEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(parseHex(category.colorHex)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(category.name, modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Edit, null, tint = AppColors.GrayText, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, null, tint = AppColors.Red, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CategoryEditorDialog(initial: CategoryEntity?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initial?.colorHex ?: "#007AFF") }
    val palette = listOf("#007AFF","#34C759","#FF9500","#FF3B30","#AF52DE","#FF2D55","#64D2FF","#5856D6","#FF64D2","#A1887F")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая категория" else "Изменить категорию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                Text("Цвет", fontSize = 13.sp, color = AppColors.GrayText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.take(5).forEach { hex -> ColorDot(hex, hex.equals(selectedColor, true)) { selectedColor = hex } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.drop(5).forEach { hex -> ColorDot(hex, hex.equals(selectedColor, true)) { selectedColor = hex } }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { val t = name.trim(); if (t.isNotEmpty()) onSave(t, initial?.iconName ?: "tag", selectedColor) }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(parseHex(hex)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

private fun parseHex(hex: String?): Color {
    if (hex.isNullOrBlank()) return AppColors.Blue
    return try {
        val c = hex.removePrefix("#"); val v = c.toLong(16)
        if (c.length == 6) Color(v or 0xFF000000) else Color(v)
    } catch (e: Exception) { AppColors.Blue }
}

private fun formatSyncDateShort(millis: Long): String {
    val fmt = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("ru"))
    return fmt.format(Date(millis))
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    
    val hasMinLength = newPassword.length >= 8
    val hasDigit = newPassword.any { it.isDigit() }
    val hasLower = newPassword.any { it.isLowerCase() }
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val noSpaces = !newPassword.contains(' ')
    val canSave = !success && currentPassword.isNotBlank() && hasMinLength && hasDigit && hasLower && hasUpper && hasSpecial && noSpaces && newPassword == confirmPassword

    Box(modifier = Modifier.fillMaxSize().background(AppColors.SystemGroupedBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Отмена", color = AppColors.Blue, fontSize = 16.sp)
                }
                Text("Изменение пароля", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.align(Alignment.Center))
                TextButton(
                    onClick = {
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        val email = user?.email ?: ""
                        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
                        user?.reauthenticate(credential)?.addOnSuccessListener {
                            user.updatePassword(newPassword).addOnSuccessListener {
                                success = true; error = null; onDismiss()
                            }.addOnFailureListener { e -> error = e.localizedMessage ?: "Ошибка" }
                        }?.addOnFailureListener { error = "Неверный текущий пароль" }
                    },
                    enabled = canSave,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text("Сохранить", color = if (canSave) AppColors.Blue else AppColors.GrayText, fontSize = 16.sp)
                }
            }
            Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp)

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                PwdSectionCard("Текущий пароль") {
                    PwdField(value = currentPassword, onValueChange = { currentPassword = it }, placeholder = "Введите текущий пароль")
                }

                
                PwdSectionCard("Новый пароль") {
                    PwdField(value = newPassword, onValueChange = { newPassword = it }, placeholder = "Новый пароль")
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.SeparatorLight))
                    PwdField(value = confirmPassword, onValueChange = { confirmPassword = it }, placeholder = "Повторите новый пароль")
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.SeparatorLight))
                    
                    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Требования к паролю:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Label)
                        PwdRequirement("Минимум 8 символов", hasMinLength)
                        PwdRequirement("Хотя бы одна цифра", hasDigit)
                        PwdRequirement("Хотя бы одна строчная буква", hasLower)
                        PwdRequirement("Хотя бы одна заглавная буква", hasUpper)
                        PwdRequirement("Хотя бы один спецсимвол", hasSpecial)
                        PwdRequirement("Без пробелов", noSpaces)
                        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                            PwdRequirement("Пароли совпадают", false)
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, color = AppColors.Red, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun PwdSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title.uppercase(), fontSize = 12.sp, color = AppColors.GrayText, modifier = Modifier.padding(start = 4.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(12.dp))) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun PwdField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (value.isEmpty()) Text(placeholder, color = AppColors.GrayText, fontSize = 16.sp)
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.Label, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Blue),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PwdRequirement(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (met) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            null, tint = if (met) AppColors.Green else AppColors.Red,
            modifier = Modifier.size(18.dp)
        )
        Text(text, fontSize = 14.sp, color = if (met) AppColors.Label else AppColors.Red)
    }
}
