package com.caloracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking foods that required cloud API fallback.
 * Used to identify gaps in TFLite model coverage for future training data collection.
 *
 * These logs can be exported periodically to improve the on-device model.
 */
@Entity(tableName = "cloud_fallback_logs")
data class CloudFallbackLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Timestamp when the fallback occurred */
    val timestamp: Long,

    /** Food name identified by cloud API */
    val foodName: String,

    /** Top prediction from TFLite (if any) - what the local model thought it was */
    val localPrediction: String?,

    /** Confidence of local prediction (0.0 if no prediction) */
    val localConfidence: Float,

    /** Reason for fallback (e.g., "low_confidence", "no_prediction", "tflite_error") */
    val fallbackReason: String,

    /** URI to the original image (for potential training data export) */
    val imageUri: String?,

    /** Whether this log has been exported/processed */
    val exported: Boolean = false
)
