package com.caloracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from Claude API Messages endpoint.
 */
data class ClaudeResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: List<ContentResponse>,

    @SerializedName("model")
    val model: String,

    @SerializedName("stop_reason")
    val stopReason: String? = null,

    @SerializedName("usage")
    val usage: Usage? = null
)

/**
 * Content in the response.
 */
data class ContentResponse(
    @SerializedName("type")
    val type: String,

    @SerializedName("text")
    val text: String? = null
)

/**
 * Token usage information.
 */
data class Usage(
    @SerializedName("input_tokens")
    val inputTokens: Int,

    @SerializedName("output_tokens")
    val outputTokens: Int
)

/**
 * Parsed food analysis from Claude's response.
 * This matches the JSON structure we ask Claude to return.
 */
data class FoodAnalysisResponse(
    @SerializedName("primaryFood")
    val primaryFood: FoodItem,

    @SerializedName("alternatives")
    val alternatives: List<FoodItem> = emptyList()
)

/**
 * Individual food item with nutritional information.
 */
data class FoodItem(
    @SerializedName("name")
    val name: String,

    @SerializedName("portion")
    val portion: String,

    @SerializedName("calories")
    val calories: Double,

    @SerializedName("protein")
    val protein: Double,

    @SerializedName("carbs")
    val carbs: Double,

    @SerializedName("fat")
    val fat: Double
)

/**
 * Error response from Claude API.
 */
data class ClaudeErrorResponse(
    @SerializedName("type")
    val type: String,

    @SerializedName("error")
    val error: ErrorDetail
)

/**
 * Error detail.
 */
data class ErrorDetail(
    @SerializedName("type")
    val type: String,

    @SerializedName("message")
    val message: String
)
