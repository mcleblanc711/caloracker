package com.caloracker.util

/**
 * Application-wide constants.
 */
object Constants {

    /**
     * Database constants.
     */
    object Database {
        const val NAME = "caloracker.db"
        const val VERSION = 1
    }

    /**
     * Default daily nutrition goals.
     */
    object NutritionGoals {
        const val DEFAULT_CALORIES = 2000.0
        const val DEFAULT_PROTEIN = 150.0  // grams
        const val DEFAULT_CARBS = 250.0    // grams
        const val DEFAULT_FAT = 65.0       // grams
    }

    /**
     * Image constants.
     */
    object Image {
        const val MAX_SIZE_BYTES = 5 * 1024 * 1024 // 5MB
        const val QUALITY = 85
        const val MAX_DIMENSION = 1024
    }

    /**
     * API constants.
     */
    object Api {
        const val TIMEOUT_SECONDS = 30L
        const val MAX_RETRIES = 3
    }

    /**
     * Navigation argument keys.
     */
    object NavArgs {
        const val FOOD_ANALYSIS_RESULT = "food_analysis_result"
        const val SELECTED_DATE = "selected_date"
        const val IMAGE_URI = "image_uri"
    }

    /**
     * Shared preferences keys.
     */
    object Prefs {
        const val NAME = "caloracker_prefs"
        const val DAILY_CALORIE_GOAL = "daily_calorie_goal"
        const val DAILY_PROTEIN_GOAL = "daily_protein_goal"
        const val DAILY_CARBS_GOAL = "daily_carbs_goal"
        const val DAILY_FAT_GOAL = "daily_fat_goal"
    }

    /**
     * Permission request codes.
     */
    object PermissionRequestCodes {
        const val CAMERA = 100
        const val STORAGE = 101
    }

    /**
     * Result codes for activities.
     */
    object ResultCodes {
        const val IMAGE_CAPTURED = 200
        const val IMAGE_SELECTED = 201
    }
}
