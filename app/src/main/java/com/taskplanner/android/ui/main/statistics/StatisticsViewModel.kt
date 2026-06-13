package com.taskplanner.android.ui.main.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskplanner.android.data.repository.StatisticsData
import com.taskplanner.android.data.repository.StatisticsPeriod
import com.taskplanner.android.data.repository.StatisticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val userId: String,
    private val repository: StatisticsRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatisticsPeriod.WEEK)
    val selectedPeriod: StateFlow<StatisticsPeriod> = _selectedPeriod.asStateFlow()

    private val _data = MutableStateFlow(StatisticsData())
    val data: StateFlow<StatisticsData> = _data.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        load()
    }

    fun setPeriod(period: StatisticsPeriod) {
        if (_selectedPeriod.value == period) return
        _selectedPeriod.value = period
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _loading.value = true
            val stats = repository.getStatistics(userId, _selectedPeriod.value)
            _data.update { stats }
            _loading.value = false
        }
    }

    class Factory(
        private val userId: String,
        private val repository: StatisticsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(userId, repository) as T
        }
    }
}

