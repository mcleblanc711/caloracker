package com.caloracker.domain.model

/**
 * Domain model representing a food item with nutritional information.
 * Used throughout the app for food analysis and tracking.
 */
data class Food(
    val name: String,
    val portion: String,
    val nutrition: NutritionInfo,
    val imageUri: String? = null
)

/**
 * Nutritional information for a food item.
 */
data class NutritionInfo(
    val calories: Double,
    val protein: Double,  // grams
    val carbs: Double,    // grams
    val fat: Double       // grams
) {
    /**
     * Format nutrition info as a readable string.
     */
    fun formatSummary(): String {
        return "${calories.toInt()}cal | P:${protein.toInt()}g | C:${carbs.toInt()}g | F:${fat.toInt()}g"
    }
}

/**
 * Result of food detection from TensorFlow Lite model.
 * Contains predicted food labels with confidence scores.
 */
data class FoodDetectionResult(
    val predictions: List<FoodPrediction>,
    val topConfidence: Float = predictions.firstOrNull()?.confidence ?: 0f,
    val source: DetectionSource = DetectionSource.LOCAL,
    val suggestCloudFallback: Boolean = false,
    val cloudFallbackReason: String? = null
)

/**
 * Source of food detection result.
 */
enum class DetectionSource {
    LOCAL,      // TensorFlow Lite on-device
    CLOUD,      // Claude Vision API
    HYBRID      // Local detection with cloud-enhanced nutrition data
}

/**
 * A single food prediction from TensorFlow Lite model.
 */
data class FoodPrediction(
    val label: String,
    val confidence: Float,
    val displayName: String = label.replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.capitalize() }
)
