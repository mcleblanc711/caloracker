package com.caloracker.data.repository

import android.util.Log
import com.caloracker.BuildConfig
import com.caloracker.data.local.dao.FoodLogDao
import com.caloracker.data.local.entity.FoodLog
import com.caloracker.data.remote.RetrofitClient
import com.caloracker.data.remote.dto.FoodAnalysisResponse
import com.caloracker.data.remote.dto.createFoodAnalysisRequest
import com.caloracker.domain.model.Food
import com.caloracker.domain.model.FoodAnalysisResult
import com.caloracker.domain.model.NutritionInfo
import com.caloracker.domain.model.Result
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for food-related data operations.
 * Handles both remote API calls (Claude vision) and local database operations (Room).
 *
 * SECURITY: Never logs API keys or sensitive data.
 */
class FoodRepository(
    private val foodLogDao: FoodLogDao,
    private val claudeApi: com.caloracker.data.remote.ClaudeApi = RetrofitClient.claudeApi
) {

    private val gson = Gson()
    private val TAG = "FoodRepository"

    /**
     * Analyze a food image using Claude's vision API.
     *
     * @param base64Image Base64-encoded image data
     * @param mediaType Image media type (e.g., "image/jpeg")
     * @return Result containing food analysis or error
     */
    suspend fun analyzeFoodImage(
        base64Image: String,
        mediaType: String = "image/jpeg"
    ): Result<FoodAnalysisResult> {
        return try {
            // Validate API key is configured
            if (!RetrofitClient.isApiKeyConfigured()) {
                return Result.Error("API key not configured. Please set CLAUDE_API_KEY environment variable.")
            }

            // Create request
            val request = createFoodAnalysisRequest(
                model = BuildConfig.CLAUDE_MODEL,
                base64Image = base64Image,
                mediaType = mediaType
            )

            // Make API call (NEVER log the API key!)
            val response = claudeApi.analyzeFood(
                apiKey = BuildConfig.CLAUDE_API_KEY,
                apiVersion = BuildConfig.CLAUDE_API_VERSION,
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val claudeResponse = response.body()!!

                // Extract text from response
                val responseText = claudeResponse.content
                    .firstOrNull { it.type == "text" }
                    ?.text

                if (responseText.isNullOrBlank()) {
                    return Result.Error("Empty response from Claude API")
                }

                // Parse JSON response
                val analysisResponse = try {
                    gson.fromJson(responseText, FoodAnalysisResponse::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Claude response", e)
                    return Result.Error("Failed to parse food analysis: ${e.message}")
                }

                // Convert to domain models
                val primaryFood = analysisResponse.primaryFood.toDomainModel()
                val alternatives = analysisResponse.alternatives.map { it.toDomainModel() }

                Result.Success(FoodAnalysisResult(primaryFood, alternatives))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Claude API error: $errorMsg")
                Result.Error("Failed to analyze food: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception analyzing food", e)
            Result.Error("Error analyzing food: ${e.message}", e)
        }
    }

    /**
     * Save a food log entry to the local database.
     *
     * @param food The food to log
     * @param date Timestamp for the log entry
     * @return Result with the inserted row ID
     */
    suspend fun logFood(food: Food, date: Long): Result<Long> {
        return try {
            val foodLog = FoodLog(
                date = date,
                foodName = food.name,
                portion = food.portion,
                calories = food.nutrition.calories,
                protein = food.nutrition.protein,
                carbs = food.nutrition.carbs,
                fat = food.nutrition.fat,
                imageUri = food.imageUri
            )

            val id = foodLogDao.insert(foodLog)
            Result.Success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging food", e)
            Result.Error("Failed to save food log: ${e.message}", e)
        }
    }

    /**
     * Get all food logs for a specific date.
     *
     * @param startOfDay Timestamp for start of day
     * @param endOfDay Timestamp for end of day
     * @return Flow of food logs mapped to domain models
     */
    fun getLogsForDate(startOfDay: Long, endOfDay: Long): Flow<List<Food>> {
        return foodLogDao.getLogsForDate(startOfDay, endOfDay)
            .map { logs -> logs.map { it.toDomainModel() } }
    }

    /**
     * Get total calories for a specific date.
     *
     * @param startOfDay Timestamp for start of day
     * @param endOfDay Timestamp for end of day
     * @return Total calories
     */
    suspend fun getTotalCaloriesForDate(startOfDay: Long, endOfDay: Long): Double {
        return foodLogDao.getTotalCaloriesForDate(startOfDay, endOfDay)
    }

    /**
     * Get total macros for a specific date.
     *
     * @param startOfDay Timestamp for start of day
     * @param endOfDay Timestamp for end of day
     * @return NutritionInfo with totals
     */
    suspend fun getTotalMacrosForDate(startOfDay: Long, endOfDay: Long): NutritionInfo {
        val macros = foodLogDao.getTotalMacrosForDate(startOfDay, endOfDay)
        return NutritionInfo(
            calories = 0.0, // Calories not summed here, use getTotalCaloriesForDate
            protein = macros.protein,
            carbs = macros.carbs,
            fat = macros.fat
        )
    }

    /**
     * Delete a food log entry.
     *
     * @param foodLog The food log to delete
     */
    suspend fun deleteLog(foodLog: FoodLog): Result<Unit> {
        return try {
            foodLogDao.delete(foodLog)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting food log", e)
            Result.Error("Failed to delete food log: ${e.message}", e)
        }
    }

    /**
     * Convert DTO FoodItem to domain model.
     */
    private fun com.caloracker.data.remote.dto.FoodItem.toDomainModel(): Food {
        return Food(
            name = this.name,
            portion = this.portion,
            nutrition = NutritionInfo(
                calories = this.calories,
                protein = this.protein,
                carbs = this.carbs,
                fat = this.fat
            )
        )
    }

    /**
     * Convert FoodLog entity to domain model.
     */
    private fun FoodLog.toDomainModel(): Food {
        return Food(
            name = this.foodName,
            portion = this.portion,
            nutrition = NutritionInfo(
                calories = this.calories,
                protein = this.protein,
                carbs = this.carbs,
                fat = this.fat
            ),
            imageUri = this.imageUri
        )
    }
}
