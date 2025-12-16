package com.caloracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from USDA FoodData Central API search endpoint.
 * Contains a list of foods matching the search query.
 */
data class UsdaSearchResponse(
    @SerializedName("foods")
    val foods: List<UsdaFood>?,

    @SerializedName("totalHits")
    val totalHits: Int?,

    @SerializedName("currentPage")
    val currentPage: Int?,

    @SerializedName("totalPages")
    val totalPages: Int?
)

/**
 * Represents a food item from USDA search results.
 */
data class UsdaFood(
    @SerializedName("fdcId")
    val fdcId: Int,

    @SerializedName("description")
    val description: String,

    @SerializedName("dataType")
    val dataType: String?,

    @SerializedName("brandOwner")
    val brandOwner: String?,

    @SerializedName("foodNutrients")
    val foodNutrients: List<UsdaFoodNutrient>?
)

/**
 * Represents a nutrient in a food item.
 */
data class UsdaFoodNutrient(
    @SerializedName("nutrientId")
    val nutrientId: Int?,

    @SerializedName("nutrientName")
    val nutrientName: String,

    @SerializedName("nutrientNumber")
    val nutrientNumber: String?,

    @SerializedName("unitName")
    val unitName: String?,

    @SerializedName("value")
    val value: Double
)
