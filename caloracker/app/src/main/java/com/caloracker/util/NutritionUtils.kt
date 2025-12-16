package com.caloracker.util

import com.caloracker.data.remote.dto.UsdaFood
import com.caloracker.data.remote.dto.UsdaFoodNutrient
import com.caloracker.data.remote.dto.UsdaFoodResponse
import com.caloracker.domain.model.NutritionInfo

/**
 * Utility functions for extracting nutrition information from USDA API responses.
 */
object NutritionUtils {

    // USDA nutrient IDs (these are standard across the USDA database)
    private const val ENERGY_KCAL_ID = 1008
    private const val PROTEIN_ID = 1003
    private const val CARBS_ID = 1005
    private const val FAT_ID = 1004

    // Alternative nutrient names (case-insensitive matching)
    private val ENERGY_NAMES = setOf("energy", "calories", "kcal")
    private val PROTEIN_NAMES = setOf("protein")
    private val CARBS_NAMES = setOf("carbohydrate", "carbohydrates", "carbs")
    private val FAT_NAMES = setOf("fat", "total lipid (fat)", "total fat")

    /**
     * Extract nutrition information from USDA food search result.
     * Uses nutrient IDs and names to find calories, protein, carbs, and fat.
     *
     * @param usdaFood The USDA food item from search results
     * @return NutritionInfo object, or null if essential nutrients are missing
     */
    fun extractNutritionFromSearchResult(usdaFood: UsdaFood): NutritionInfo? {
        val nutrients = usdaFood.foodNutrients ?: return null

        val calories = findNutrientValue(nutrients, ENERGY_KCAL_ID, ENERGY_NAMES) ?: 0.0
        val protein = findNutrientValue(nutrients, PROTEIN_ID, PROTEIN_NAMES) ?: 0.0
        val carbs = findNutrientValue(nutrients, CARBS_ID, CARBS_NAMES) ?: 0.0
        val fat = findNutrientValue(nutrients, FAT_ID, FAT_NAMES) ?: 0.0

        // Return null if all values are zero (likely missing data)
        if (calories == 0.0 && protein == 0.0 && carbs == 0.0 && fat == 0.0) {
            return null
        }

        return NutritionInfo(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat
        )
    }

    /**
     * Extract nutrition information from USDA detailed food response.
     *
     * @param usdaFoodResponse The detailed USDA food response
     * @return NutritionInfo object, or null if essential nutrients are missing
     */
    fun extractNutritionFromDetailedResponse(usdaFoodResponse: UsdaFoodResponse): NutritionInfo? {
        val nutrients = usdaFoodResponse.foodNutrients ?: return null

        val calories = findNutrientValue(nutrients, ENERGY_KCAL_ID, ENERGY_NAMES) ?: 0.0
        val protein = findNutrientValue(nutrients, PROTEIN_ID, PROTEIN_NAMES) ?: 0.0
        val carbs = findNutrientValue(nutrients, CARBS_ID, CARBS_NAMES) ?: 0.0
        val fat = findNutrientValue(nutrients, FAT_ID, FAT_NAMES) ?: 0.0

        // Return null if all values are zero (likely missing data)
        if (calories == 0.0 && protein == 0.0 && carbs == 0.0 && fat == 0.0) {
            return null
        }

        return NutritionInfo(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat
        )
    }

    /**
     * Find a nutrient value by ID or name from a list of nutrients.
     *
     * @param nutrients List of USDA nutrients
     * @param nutrientId The nutrient ID to search for (primary method)
     * @param nutrientNames Alternative names to search for (fallback method)
     * @return The nutrient value, or null if not found
     */
    private fun findNutrientValue(
        nutrients: List<UsdaFoodNutrient>,
        nutrientId: Int,
        nutrientNames: Set<String>
    ): Double? {
        // First try to find by ID (most reliable)
        nutrients.find { it.nutrientId == nutrientId }?.let {
            return it.value
        }

        // Fallback to finding by name (case-insensitive)
        nutrients.find { nutrient ->
            nutrientNames.any { name ->
                nutrient.nutrientName.lowercase().contains(name.lowercase())
            }
        }?.let {
            return it.value
        }

        return null
    }

    /**
     * Format serving size for display.
     *
     * @param servingSize The serving size value
     * @param servingSizeUnit The serving size unit
     * @return Formatted string (e.g., "100g", "1 cup")
     */
    fun formatServingSize(servingSize: Double?, servingSizeUnit: String?): String {
        if (servingSize == null || servingSizeUnit == null) {
            return "100g" // Default serving
        }

        return if (servingSize % 1.0 == 0.0) {
            "${servingSize.toInt()}$servingSizeUnit"
        } else {
            "$servingSize$servingSizeUnit"
        }
    }

    /**
     * Check if nutrition info has reasonable values.
     * Used to validate data quality from USDA API.
     *
     * @param nutrition The nutrition info to validate
     * @return true if values seem reasonable, false otherwise
     */
    fun isReasonableNutrition(nutrition: NutritionInfo): Boolean {
        // Basic sanity checks
        if (nutrition.calories < 0 || nutrition.calories > 10000) return false
        if (nutrition.protein < 0 || nutrition.protein > 1000) return false
        if (nutrition.carbs < 0 || nutrition.carbs > 1000) return false
        if (nutrition.fat < 0 || nutrition.fat > 1000) return false

        // At least one macronutrient should be present
        return nutrition.calories > 0 ||
                nutrition.protein > 0 ||
                nutrition.carbs > 0 ||
                nutrition.fat > 0
    }
}
