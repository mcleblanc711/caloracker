package com.caloracker.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.caloracker.data.ml.FoodDetectionService
import com.caloracker.data.remote.RetrofitClient
import com.caloracker.data.remote.dto.UsdaFood
import com.caloracker.domain.model.DetectionSource
import com.caloracker.domain.model.Food
import com.caloracker.domain.model.FoodDetectionResult
import com.caloracker.domain.model.FoodPrediction
import com.caloracker.domain.model.NutritionInfo
import com.caloracker.domain.model.Result
import com.caloracker.util.NutritionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for food detection and nutrition lookup.
 * Coordinates between TensorFlow Lite local detection and USDA API nutrition data.
 */
class NutritionRepository(private val context: Context) {

    private val foodDetectionService = FoodDetectionService(context)
    private val usdaApi = RetrofitClient.usdaApi

    companion object {
        private const val TAG = "NutritionRepository"

        // Confidence thresholds for cloud fallback decisions
        const val HIGH_CONFIDENCE_THRESHOLD = 0.7f    // Trust local result fully
        const val MEDIUM_CONFIDENCE_THRESHOLD = 0.4f  // Suggest cloud fallback
        // Below MEDIUM_CONFIDENCE_THRESHOLD: automatic cloud fallback recommended
    }

    /**
     * Initialize the TensorFlow Lite model.
     * Should be called once when the app starts.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            foodDetectionService.initialize()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize food detection service", e)
            Result.Error("Failed to initialize food detection: ${e.message}", e)
        }
    }

    /**
     * Detect food from an image using TensorFlow Lite.
     * Runs entirely on-device, no network required.
     *
     * Returns confidence-based fallback suggestions:
     * - High confidence (>= 0.7): No fallback needed
     * - Medium confidence (0.4-0.7): Suggest cloud fallback as option
     * - Low confidence (< 0.4): Recommend automatic cloud fallback
     *
     * @param bitmap The food image to analyze
     * @return Result containing list of food predictions with confidence scores and fallback metadata
     */
    suspend fun detectFoodFromImage(bitmap: Bitmap): Result<FoodDetectionResult> =
        withContext(Dispatchers.IO) {
            try {
                val predictions = foodDetectionService.classifyImage(bitmap)

                if (predictions.isEmpty()) {
                    // No predictions at all - strongly recommend cloud fallback
                    return@withContext Result.Success(
                        FoodDetectionResult(
                            predictions = emptyList(),
                            topConfidence = 0f,
                            source = DetectionSource.LOCAL,
                            suggestCloudFallback = true,
                            cloudFallbackReason = "Could not identify food in image"
                        )
                    )
                }

                // Convert service predictions to domain predictions
                val domainPredictions = predictions.map { prediction ->
                    FoodPrediction(
                        label = prediction.label,
                        confidence = prediction.confidence,
                        displayName = prediction.displayName
                    )
                }

                val topConfidence = domainPredictions.first().confidence

                // Determine if cloud fallback should be suggested based on confidence
                val (suggestFallback, fallbackReason) = when {
                    topConfidence >= HIGH_CONFIDENCE_THRESHOLD -> {
                        false to null
                    }
                    topConfidence >= MEDIUM_CONFIDENCE_THRESHOLD -> {
                        true to "Confidence is moderate (${(topConfidence * 100).toInt()}%). Cloud analysis may improve accuracy."
                    }
                    else -> {
                        true to "Low confidence (${(topConfidence * 100).toInt()}%). Recommend cloud analysis for better results."
                    }
                }

                Result.Success(
                    FoodDetectionResult(
                        predictions = domainPredictions,
                        topConfidence = topConfidence,
                        source = DetectionSource.LOCAL,
                        suggestCloudFallback = suggestFallback,
                        cloudFallbackReason = fallbackReason
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error detecting food from image", e)
                Result.Error("Food detection failed: ${e.message}", e)
            }
        }

    /**
     * Search for nutrition information using USDA API.
     * Requires internet connection.
     *
     * @param foodName The name of the food to search for
     * @return Result containing a Food object with nutrition info, or error
     */
    suspend fun searchNutritionInfo(foodName: String): Result<Food> =
        withContext(Dispatchers.IO) {
            try {
                val response = usdaApi.searchFoods(
                    query = foodName,
                    pageSize = 5
                )

                if (!response.isSuccessful) {
                    return@withContext Result.Error(
                        "USDA API request failed: ${response.code()} ${response.message()}"
                    )
                }

                val searchResponse = response.body()
                val foods = searchResponse?.foods

                if (foods.isNullOrEmpty()) {
                    return@withContext Result.Error(
                        "No nutrition data found for '$foodName'. Try manual entry."
                    )
                }

                // Find the best match (first result is usually best)
                val bestMatch = findBestNutritionMatch(foods, foodName)
                    ?: return@withContext Result.Error(
                        "No valid nutrition data found for '$foodName'"
                    )

                Result.Success(bestMatch)
            } catch (e: Exception) {
                Log.e(TAG, "Error searching nutrition info", e)
                Result.Error(
                    "Failed to fetch nutrition data: ${e.message}. Check internet connection.",
                    e
                )
            }
        }

    /**
     * Get detailed nutrition information for a specific food by FDC ID.
     *
     * @param fdcId The USDA FDC ID of the food
     * @return Result containing detailed Food object
     */
    suspend fun getFoodDetails(fdcId: Int): Result<Food> = withContext(Dispatchers.IO) {
        try {
            val response = usdaApi.getFoodDetails(fdcId)

            if (!response.isSuccessful) {
                return@withContext Result.Error(
                    "Failed to get food details: ${response.code()}"
                )
            }

            val foodResponse = response.body()
                ?: return@withContext Result.Error("Empty response from USDA API")

            val nutrition = NutritionUtils.extractNutritionFromDetailedResponse(foodResponse)
                ?: return@withContext Result.Error("No nutrition data available")

            val servingSize = NutritionUtils.formatServingSize(
                foodResponse.servingSize,
                foodResponse.servingSizeUnit
            )

            val food = Food(
                name = foodResponse.description,
                portion = servingSize,
                nutrition = nutrition
            )

            Result.Success(food)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting food details", e)
            Result.Error("Failed to get food details: ${e.message}", e)
        }
    }

    /**
     * Find the best nutrition match from USDA search results.
     * Prefers results with complete nutrition data.
     *
     * @param foods List of USDA foods from search
     * @param searchTerm The original search term
     * @return Food object with nutrition info, or null if no valid match
     */
    private fun findBestNutritionMatch(foods: List<UsdaFood>, searchTerm: String): Food? {
        for (usdaFood in foods) {
            val nutrition = NutritionUtils.extractNutritionFromSearchResult(usdaFood)

            if (nutrition != null && NutritionUtils.isReasonableNutrition(nutrition)) {
                return Food(
                    name = usdaFood.description,
                    portion = "100g", // USDA data is typically per 100g
                    nutrition = nutrition
                )
            }
        }

        return null
    }

    /**
     * Create a manual food entry when API data is not available.
     * Used as a fallback when USDA API doesn't have the food.
     *
     * @param name Food name
     * @param calories Calories per serving
     * @param protein Protein in grams
     * @param carbs Carbohydrates in grams
     * @param fat Fat in grams
     * @param portion Portion size (e.g., "100g", "1 cup")
     * @return Food object
     */
    fun createManualEntry(
        name: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        portion: String = "100g"
    ): Food {
        return Food(
            name = name,
            portion = portion,
            nutrition = NutritionInfo(
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat
            )
        )
    }

    /**
     * Clean up resources when repository is no longer needed.
     */
    fun cleanup() {
        foodDetectionService.close()
    }
}
