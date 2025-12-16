package com.caloracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a food log entry.
 * Stores information about food consumed including nutrition data.
 */
@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: Long,              // Unix timestamp in milliseconds
    val foodName: String,
    val portion: String,         // e.g., "100g", "1 cup", "1 serving"
    val calories: Double,
    val protein: Double,         // grams
    val carbs: Double,          // grams
    val fat: Double,            // grams
    val imageUri: String? = null // Optional food image path
)
