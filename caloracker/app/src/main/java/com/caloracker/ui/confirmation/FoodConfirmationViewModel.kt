package com.caloracker.ui.confirmation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.caloracker.data.local.AppDatabase
import com.caloracker.data.repository.FoodRepository
import com.caloracker.domain.model.Food
import com.caloracker.domain.model.FoodAnalysisResult
import com.caloracker.domain.model.Result
import com.caloracker.util.DateUtils
import com.caloracker.util.ImageUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for the Food Confirmation screen.
 * Handles food image analysis with Claude API and logging.
 */
class FoodConfirmationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodRepository
    private val context = application

    // Selected food (initially the primary food from Claude)
    private val _selectedFood = MutableLiveData<Food>()
    val selectedFood: LiveData<Food> = _selectedFood

    // Alternative food suggestions
    private val _alternatives = MutableLiveData<List<Food>>()
    val alternatives: LiveData<List<Food>> = _alternatives

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Success state (food logged)
    private val _foodLogged = MutableLiveData<Boolean>()
    val foodLogged: LiveData<Boolean> = _foodLogged

    init {
        // Initialize repository
        val database = AppDatabase.getDatabase(application)
        repository = FoodRepository(database.foodLogDao())
    }

    /**
     * Analyze food image using Claude vision API.
     *
     * @param imageUri URI of the captured food image
     */
    fun analyzeFoodImage(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Convert image to base64
                val base64Image = ImageUtils.convertImageToBase64(context, imageUri)

                if (base64Image.isNullOrEmpty()) {
                    _error.value = "Failed to process image"
                    _isLoading.value = false
                    return@launch
                }

                // Get media type
                val mediaType = ImageUtils.getMediaType(imageUri.toString())

                // Call Claude API to analyze food
                when (val result = repository.analyzeFoodImage(base64Image, mediaType)) {
                    is Result.Success -> {
                        val analysis = result.data
                        _selectedFood.value = analysis.primaryFood.copy(imageUri = imageUri.toString())
                        _alternatives.value = analysis.alternatives.map {
                            it.copy(imageUri = imageUri.toString())
                        }
                        _error.value = null
                    }
                    is Result.Error -> {
                        _error.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handling loading state
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Select a food from alternatives.
     *
     * @param food The food to select
     */
    fun selectFood(food: Food) {
        _selectedFood.value = food
    }

    /**
     * Log the selected food to the database.
     */
    fun logSelectedFood() {
        val food = _selectedFood.value ?: return

        viewModelScope.launch {
            _isLoading.value = true

            when (repository.logFood(food, DateUtils.getCurrentTimestamp())) {
                is Result.Success -> {
                    _foodLogged.value = true
                }
                is Result.Error -> {
                    _error.value = "Failed to log food"
                }
                is Result.Loading -> {
                    // Handled by _isLoading
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}
