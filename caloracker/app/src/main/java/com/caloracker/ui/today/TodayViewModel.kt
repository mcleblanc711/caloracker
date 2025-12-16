package com.caloracker.ui.today

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
import com.caloracker.util.Constants
import com.caloracker.util.DateUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for the Today screen.
 * Manages today's food logs and nutrition totals.
 */
class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodRepository
    private val startOfToday = DateUtils.getStartOfToday()
    private val endOfToday = DateUtils.getEndOfToday()

    // Daily nutrition goals
    private val calorieGoal = Constants.NutritionGoals.DEFAULT_CALORIES
    private val proteinGoal = Constants.NutritionGoals.DEFAULT_PROTEIN
    private val carbsGoal = Constants.NutritionGoals.DEFAULT_CARBS
    private val fatGoal = Constants.NutritionGoals.DEFAULT_FAT

    // Live data for food logs
    val foodLogs: LiveData<List<Food>>

    // Live data for totals
    private val _totalCalories = MutableLiveData<Double>()
    val totalCalories: LiveData<Double> = _totalCalories

    private val _totalMacros = MutableLiveData<NutritionInfo>()
    val totalMacros: LiveData<NutritionInfo> = _totalMacros

    // Calorie progress (0-100)
    private val _calorieProgress = MutableLiveData<Int>()
    val calorieProgress: LiveData<Int> = _calorieProgress

    init {
        // Initialize repository
        val database = AppDatabase.getDatabase(application)
        repository = FoodRepository(database.foodLogDao())

        // Observe food logs for today
        foodLogs = repository.getLogsForDate(startOfToday, endOfToday).asLiveData()

        // Load totals
        loadTotals()
    }

    /**
     * Load total calories and macros for today.
     */
    fun loadTotals() {
        viewModelScope.launch {
            // Get total calories
            val calories = repository.getTotalCaloriesForDate(startOfToday, endOfToday)
            _totalCalories.value = calories

            // Calculate calorie progress percentage
            val progress = ((calories / calorieGoal) * 100).toInt().coerceIn(0, 100)
            _calorieProgress.value = progress

            // Get total macros
            val macros = repository.getTotalMacrosForDate(startOfToday, endOfToday)
            _totalMacros.value = macros
        }
    }

    /**
     * Get calorie goal.
     */
    fun getCalorieGoal(): Double = calorieGoal

    /**
     * Get protein goal.
     */
    fun getProteinGoal(): Double = proteinGoal

    /**
     * Get carbs goal.
     */
    fun getCarbsGoal(): Double = carbsGoal

    /**
     * Get fat goal.
     */
    fun getFatGoal(): Double = fatGoal

    /**
     * Format calories display text.
     */
    fun formatCalories(consumed: Double): String {
        return "${consumed.toInt()} / ${calorieGoal.toInt()} cal"
    }

    /**
     * Format macros display text.
     */
    fun formatMacros(value: Double): String {
        return "${value.toInt()}g"
    }
}
