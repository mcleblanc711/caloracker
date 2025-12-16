package com.caloracker.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.caloracker.data.local.AppDatabase
import com.caloracker.data.repository.FoodRepository
import com.caloracker.domain.model.Food
import com.caloracker.domain.model.NutritionInfo
import com.caloracker.util.DateUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for the History screen.
 * Manages historical food logs by date.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodRepository

    // Selected date (defaults to yesterday)
    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    // Live data for food logs
    private val _foodLogs = MutableLiveData<List<Food>>()
    val foodLogs: LiveData<List<Food>> = _foodLogs

    // Live data for totals
    private val _totalCalories = MutableLiveData<Double>()
    val totalCalories: LiveData<Double> = _totalCalories

    private val _totalMacros = MutableLiveData<NutritionInfo>()
    val totalMacros: LiveData<NutritionInfo> = _totalMacros

    init {
        // Initialize repository
        val database = AppDatabase.getDatabase(application)
        repository = FoodRepository(database.foodLogDao())

        // Set selected date to yesterday by default
        val yesterday = DateUtils.getStartOfToday() - 24 * 60 * 60 * 1000
        _selectedDate.value = yesterday

        // Load logs for selected date
        loadLogsForSelectedDate()
    }

    /**
     * Set the selected date and load its logs.
     *
     * @param timestamp The date timestamp
     */
    fun setSelectedDate(timestamp: Long) {
        _selectedDate.value = timestamp
        loadLogsForSelectedDate()
    }

    /**
     * Load food logs for the currently selected date.
     */
    private fun loadLogsForSelectedDate() {
        val timestamp = _selectedDate.value ?: return
        val startOfDay = DateUtils.getStartOfDay(timestamp)
        val endOfDay = DateUtils.getEndOfDay(timestamp)

        // Load food logs
        viewModelScope.launch {
            repository.getLogsForDate(startOfDay, endOfDay)
                .asLiveData()
                .observeForever { logs ->
                    _foodLogs.value = logs
                }
        }

        // Load totals
        loadTotals(startOfDay, endOfDay)
    }

    /**
     * Load total calories and macros for the selected date.
     */
    private fun loadTotals(startOfDay: Long, endOfDay: Long) {
        viewModelScope.launch {
            // Get total calories
            val calories = repository.getTotalCaloriesForDate(startOfDay, endOfDay)
            _totalCalories.value = calories

            // Get total macros
            val macros = repository.getTotalMacrosForDate(startOfDay, endOfDay)
            _totalMacros.value = macros
        }
    }

    /**
     * Format selected date for display.
     */
    fun formatSelectedDate(): String {
        val timestamp = _selectedDate.value ?: return ""
        return DateUtils.formatDate(timestamp)
    }

    /**
     * Format calories summary.
     */
    fun formatCaloriesSummary(calories: Double): String {
        return "Calories: ${calories.toInt()} cal"
    }

    /**
     * Format macros summary.
     */
    fun formatMacrosSummary(macros: NutritionInfo): String {
        return "Protein: ${macros.protein.toInt()}g | " +
                "Carbs: ${macros.carbs.toInt()}g | " +
                "Fat: ${macros.fat.toInt()}g"
    }
}
