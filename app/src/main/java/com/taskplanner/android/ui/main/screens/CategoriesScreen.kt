package com.taskplanner.android.ui.main.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.ui.LocalAppGraph
import com.taskplanner.android.ui.main.profile.CategoriesViewModel
import com.taskplanner.android.ui.theme.AppColors

@Composable
fun CategoriesScreen(userId: String, onBack: () -> Unit) {
    val graph = LocalAppGraph.current
    val vm: CategoriesViewModel = viewModel(
        factory = CategoriesViewModel.Factory(userId, graph.categoryRepository)
    )
    val categories by vm.categories.collectAsState()

    var showAddCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SystemGroupedBackground)
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Filled.ChevronLeft, null, tint = AppColors.Blue)
                Text("Профиль", color = AppColors.Blue, fontSize = 16.sp)
            }
            Text(
                "Категории",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(onClick = { showAddCategory = true }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Add, null, tint = AppColors.Blue)
            }
        }
        Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFFFFF))
                    .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(14.dp))
            ) {
                Column {
                    if (categories.isEmpty()) {
                        Text(
                            "Нет категорий",
                            color = AppColors.GrayText,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        categories.forEachIndexed { idx, cat ->
                            CatRow(
                                category = cat,
                                onEdit = { editingCategory = cat },
                                onDelete = { pendingDelete = cat }
                            )
                            if (idx < categories.size - 1) {
                                Divider(
                                    color = AppColors.SeparatorLight,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        CategoryEditorDialogCat(null, onDismiss = { showAddCategory = false }) { name, icon, color ->
            vm.createCategory(name, icon, color)
            showAddCategory = false
        }
    }
    editingCategory?.let { cat ->
        CategoryEditorDialogCat(cat, onDismiss = { editingCategory = null }) { name, icon, color ->
            vm.updateCategory(cat.id, name, icon, color)
            editingCategory = null
        }
    }
    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить категорию?") },
            text = { Text("Категория «${cat.name}» будет удалена.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCategory(cat.id); pendingDelete = null }) {
                    Text("Удалить", color = AppColors.Red)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun CatRow(category: CategoryEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { menuExpanded = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        val catIcon = SF_TO_MATERIAL[category.iconName ?: "tag.fill"] ?: Icons.Filled.Label
        Icon(
            catIcon,
            contentDescription = null,
            tint = parseCatHex(category.colorHex),
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(category.name, modifier = Modifier.weight(1f), fontSize = 16.sp, color = AppColors.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box {
            Icon(Icons.Filled.MoreHoriz, null, tint = AppColors.GrayText, modifier = Modifier.size(20.dp).clickable { menuExpanded = true })
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color.White, RoundedCornerShape(14.dp)).widthIn(min = 160.dp)) {
                DropdownMenuItem(
                    text = { Text("Редактировать", fontSize = 15.sp, color = AppColors.Blue) },
                    leadingIcon = { Icon(Icons.Filled.Edit, null, tint = AppColors.Blue, modifier = Modifier.size(18.dp)) },
                    onClick = { menuExpanded = false; onEdit() }
                )
                Divider(color = AppColors.SeparatorLight, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                DropdownMenuItem(
                    text = { Text("Удалить", fontSize = 15.sp, color = AppColors.Red) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = AppColors.Red, modifier = Modifier.size(18.dp)) },
                    onClick = { menuExpanded = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun CategoryEditorDialogCat(initial: CategoryEntity?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initial?.colorHex ?: "#007AFF") }
    var selectedIcon by remember { mutableStateOf(initial?.iconName ?: "tag") }
    val palette = listOf("#007AFF","#34C759","#FF9500","#FF3B30","#AF52DE","#FF2D55","#64D2FF","#5856D6","#FF6900","#A1887F")
    
    val icons = listOf(
        "tag.fill" to Icons.Filled.Label,
        "person.fill" to Icons.Filled.Person,
        "briefcase.fill" to Icons.Filled.Work,
        "house.fill" to Icons.Filled.Home,
        "book.fill" to Icons.Filled.School,
        "heart.fill" to Icons.Filled.Favorite,
        "figure.run" to Icons.Filled.FitnessCenter,
        "paintpalette.fill" to Icons.Filled.Palette,
        "cart.fill" to Icons.Filled.ShoppingCart,
        "car.fill" to Icons.Filled.DirectionsCar,
        "airplane" to Icons.Filled.Flight,
        "gamecontroller.fill" to Icons.Filled.SportsEsports
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая категория" else "Изменить категорию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Цвет", fontSize = 13.sp, color = AppColors.GrayText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.take(5).forEach { hex ->
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(parseCatHex(hex)).clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center) {
                            if (hex.equals(selectedColor, true)) Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.drop(5).forEach { hex ->
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(parseCatHex(hex)).clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center) {
                            if (hex.equals(selectedColor, true)) Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Text("Иконка", fontSize = 13.sp, color = AppColors.GrayText)
                
                icons.chunked(4).forEach { rowIcons ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowIcons.forEach { (name, vec) ->
                            val catColor = parseCatHex(selectedColor)
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (name == selectedIcon) catColor.copy(alpha = 0.18f) else Color(0xFFF2F2F7))
                                .border(androidx.compose.foundation.BorderStroke(if (name == selectedIcon) 2.dp else 0.dp, if (name == selectedIcon) catColor else Color.Transparent), RoundedCornerShape(12.dp))
                                .clickable { selectedIcon = name },
                                contentAlignment = Alignment.Center) {
                                Icon(vec, null, tint = if (name == selectedIcon) catColor else AppColors.Label, modifier = Modifier.size(24.dp))
                            }
                        }
                        
                        repeat(4 - rowIcons.size) { Spacer(Modifier.size(48.dp)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { val t = name.trim(); if (t.isNotEmpty()) onSave(t, selectedIcon, selectedColor) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun parseCatHex(hex: String?): Color {
    if (hex.isNullOrBlank()) return AppColors.Blue
    return try {
        val c = hex.removePrefix("#"); val v = c.toLong(16)
        if (c.length == 6) Color(v or 0xFF000000) else Color(v)
    } catch (e: Exception) { AppColors.Blue }
}

private val SF_TO_MATERIAL: Map<String, androidx.compose.ui.graphics.vector.ImageVector> = mapOf(
    "tag.fill" to Icons.Filled.Label,
    "person.fill" to Icons.Filled.Person,
    "briefcase.fill" to Icons.Filled.Work,
    "house.fill" to Icons.Filled.Home,
    "book.fill" to Icons.Filled.School,
    "heart.fill" to Icons.Filled.Favorite,
    "figure.run" to Icons.Filled.FitnessCenter,
    "paintpalette.fill" to Icons.Filled.Palette,
    "cart.fill" to Icons.Filled.ShoppingCart,
    "car.fill" to Icons.Filled.DirectionsCar,
    "airplane" to Icons.Filled.Flight,
    "gamecontroller.fill" to Icons.Filled.SportsEsports,
    "tag" to Icons.Filled.Label,
    "person" to Icons.Filled.Person,
    "work" to Icons.Filled.Work,
    "home" to Icons.Filled.Home,
    "school" to Icons.Filled.School,
    "favorite" to Icons.Filled.Favorite,
    "fitness_center" to Icons.Filled.FitnessCenter,
    "palette" to Icons.Filled.Palette,
    "shopping_cart" to Icons.Filled.ShoppingCart,
    "directions_car" to Icons.Filled.DirectionsCar,
    "flight" to Icons.Filled.Flight,
    "sports_esports" to Icons.Filled.SportsEsports
)
