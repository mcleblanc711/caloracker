package com.caloracker.ui.confirmation

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.caloracker.data.local.AppDatabase
import com.caloracker.data.repository.FoodRepository
import com.caloracker.data.repository.NutritionRepository
import com.caloracker.domain.model.DetectionSource
import com.caloracker.domain.model.Food
import com.caloracker.domain.model.FoodDetectionResult
import com.caloracker.domain.model.NutritionInfo
import com.caloracker.domain.model.Result
import com.caloracker.util.DateUtils
import com.caloracker.util.ImageUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for the Food Confirmation screen.
 * Handles food image analysis with TFLite (local) first, falling back to Claude API for low confidence.
 */
class FoodConfirmationViewModel(application: Application) : AndroidViewModel(application) {

    private val foodRepository: FoodRepository
    private val nutritionRepository: NutritionRepository
    private val context = application

    // Selected food (initially the primary food from detection)
    private val _selectedFood = MutableLiveData<Food>()
    val selectedFood: LiveData<Food> = _selectedFood

    // Alternative food suggestions
    private val _alternatives = MutableLiveData<List<Food>>()
    val alternatives: LiveData<List<Food>> = _alternatives

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Loading status message for user feedback
    private val _loadingStatus = MutableLiveData<String>()
    val loadingStatus: LiveData<String> = _loadingStatus

    // Error message
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Success state (food logged)
    private val _foodLogged = MutableLiveData<Boolean>()
    val foodLogged: LiveData<Boolean> = _foodLogged

    // Detection source for UI indication
    private val _detectionSource = MutableLiveData<DetectionSource>()
    val detectionSource: LiveData<DetectionSource> = _detectionSource

    // TFLite initialization state
    private var isTfLiteInitialized = false

    companion object {
        private const val TAG = "FoodConfirmationVM"
    }

    init {
        // Initialize repositories
        val database = AppDatabase.getDatabase(application)
        foodRepository = FoodRepository(database.foodLogDao())
        nutritionRepository = NutritionRepository(application)

        // Initialize TFLite model
        initializeTfLite()
    }

    private fun initializeTfLite() {
        viewModelScope.launch {
            when (val result = nutritionRepository.initialize()) {
                is Result.Success -> {
                    isTfLiteInitialized = true
                    Log.d(TAG, "TFLite model initialized successfully")
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to initialize TFLite: ${result.message}")
                    // Continue without TFLite - will fall back to cloud
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Analyze food image using hybrid approach:
     * 1. Try TFLite (local, fast, free) first
     * 2. If confidence is low, automatically fall back to Claude API
     *
     * @param imageUri URI of the captured food image
     */
    fun analyzeFoodImage(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _loadingStatus.value = "Analyzing image locally..."

            try {
                // First, try local TFLite detection
                if (isTfLiteInitialized) {
                    val bitmap = loadBitmapFromUri(imageUri)
                    if (bitmap != null) {
                        when (val localResult = nutritionRepository.detectFoodFromImage(bitmap)) {
                            is Result.Success -> {
                                val detection = localResult.data

                                // Check if we should use local results or fall back to cloud
                                if (detection.predictions.isNotEmpty() &&
                                    detection.topConfidence >= NutritionRepository.MEDIUM_CONFIDENCE_THRESHOLD) {
                                    // Good enough confidence - use local results
                                    handleLocalDetectionSuccess(detection, imageUri)
                                    return@launch
                                } else {
                                    // Low confidence - fall back to cloud
                                    Log.d(TAG, "Low confidence (${detection.topConfidence}), falling back to cloud")
                                    _loadingStatus.value = "Getting better results from cloud..."
                                }
                            }
                            is Result.Error -> {
                                Log.w(TAG, "Local detection failed: ${localResult.message}")
                                _loadingStatus.value = "Trying cloud analysis..."
                            }
                            is Result.Loading -> {}
                        }
                    }
                } else {
                    _loadingStatus.value = "Using cloud analysis..."
                }

                // Fall back to Claude API
                analyzeWithCloudApi(imageUri)

            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing food", e)
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle successful local TFLite detection.
     * Converts predictions to Food objects and fetches nutrition from USDA.
     */
    private suspend fun handleLocalDetectionSuccess(detection: FoodDetectionResult, imageUri: Uri) {
        _detectionSource.value = DetectionSource.LOCAL
        _loadingStatus.value = "Fetching nutrition data..."

        val predictions = detection.predictions
        if (predictions.isEmpty()) {
            _error.value = "No food detected"
            _isLoading.value = false
            return
        }

        // Get nutrition info for the top prediction from USDA
        val topPrediction = predictions.first()
        val nutritionResult = nutritionRepository.searchNutritionInfo(topPrediction.displayName)

        val primaryFood = when (nutritionResult) {
            is Result.Success -> nutritionResult.data.copy(imageUri = imageUri.toString())
            is Result.Error -> {
                // Create a placeholder food with estimated nutrition
                Food(
                    name = topPrediction.displayName,
                    portion = "1 serving",
                    nutrition = estimateNutrition(topPrediction.displayName),
                    imageUri = imageUri.toString()
                )
            }
            is Result.Loading -> return
        }

        _selectedFood.value = primaryFood

        // Convert remaining predictions to alternatives
        val alternatives = predictions.drop(1).map { prediction ->
            Food(
                name = prediction.displayName,
                portion = "1 serving",
                nutrition = NutritionInfo(0.0, 0.0, 0.0, 0.0), // Will be fetched when selected
                imageUri = imageUri.toString()
            )
        }
        _alternatives.value = alternatives

        _isLoading.value = false
        Log.d(TAG, "Local detection complete: ${primaryFood.name} (${(detection.topConfidence * 100).toInt()}% confidence)")
    }

    /**
     * Analyze food using Claude Vision API (cloud fallback).
     */
    private suspend fun analyzeWithCloudApi(imageUri: Uri) {
        _detectionSource.value = DetectionSource.CLOUD

        // Convert image to base64
        val base64Image = ImageUtils.convertImageToBase64(context, imageUri)

        if (base64Image.isNullOrEmpty()) {
            _error.value = "Failed to process image"
            _isLoading.value = false
            return
        }

        // Get media type
        val mediaType = ImageUtils.getMediaType(imageUri.toString())

        // Call Claude API
        when (val result = foodRepository.analyzeFoodImage(base64Image, mediaType)) {
            is Result.Success -> {
                val analysis = result.data
                _selectedFood.value = analysis.primaryFood.copy(imageUri = imageUri.toString())
                _alternatives.value = analysis.alternatives.map {
                    it.copy(imageUri = imageUri.toString())
                }
                _error.value = null
                Log.d(TAG, "Cloud detection complete: ${analysis.primaryFood.name}")
            }
            is Result.Error -> {
                _error.value = result.message
            }
            is Result.Loading -> {}
        }

        _isLoading.value = false
    }

    /**
     * Load bitmap from URI for TFLite processing.
     */
    private fun loadBitmapFromUri(uri: Uri): android.graphics.Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI", e)
            null
        }
    }

    /**
     * Estimate nutrition for foods without USDA data.
     * Returns generic estimates based on food category.
     */
    private fun estimateNutrition(foodName: String): NutritionInfo {
        // Very rough estimates - user should verify
        return NutritionInfo(
            calories = 200.0,
            protein = 10.0,
            carbs = 25.0,
            fat = 8.0
        )
    }

    /**
     * Select a food from alternatives.
     * If the food has no nutrition data (calories = 0), fetch it from USDA.
     *
     * @param food The food to select
     */
    fun selectFood(food: Food) {
        // If nutrition data is missing, fetch it
        if (food.nutrition.calories == 0.0) {
            viewModelScope.launch {
                _isLoading.value = true
                _loadingStatus.value = "Fetching nutrition for ${food.name}..."

                val nutritionResult = nutritionRepository.searchNutritionInfo(food.name)
                val updatedFood = when (nutritionResult) {
                    is Result.Success -> nutritionResult.data.copy(imageUri = food.imageUri)
                    is Result.Error -> {
                        // Use estimated nutrition if USDA lookup fails
                        food.copy(nutrition = estimateNutrition(food.name))
                    }
                    is Result.Loading -> food
                }

                _selectedFood.value = updatedFood
                _isLoading.value = false
            }
        } else {
            _selectedFood.value = food
        }
    }

    /**
     * Log the selected food to the database.
     */
    fun logSelectedFood() {
        val food = _selectedFood.value ?: return

        viewModelScope.launch {
            _isLoading.value = true

            when (foodRepository.logFood(food, DateUtils.getCurrentTimestamp())) {
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

    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        nutritionRepository.cleanup()
    }
}
