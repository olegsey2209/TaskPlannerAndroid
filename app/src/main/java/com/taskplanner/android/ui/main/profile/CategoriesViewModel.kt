package com.taskplanner.android.ui.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskplanner.android.data.local.entities.CategoryEntity
import com.taskplanner.android.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val userId: String,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.observeAll(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createCategory(name: String, iconName: String = "tag", colorHex: String = "#007AFF") {
        viewModelScope.launch {
            categoryRepository.createCategory(
                userId = userId,
                name = name,
                iconName = iconName,
                colorHex = colorHex
            )
        }
    }

    fun updateCategory(id: String, name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            categoryRepository.updateCategory(userId, id, name, iconName, colorHex)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            categoryRepository.softDeleteCategory(userId, id)
        }
    }

    class Factory(
        private val userId: String,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(userId, categoryRepository) as T
        }
    }
}
