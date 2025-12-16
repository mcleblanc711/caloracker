package com.caloracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from USDA FoodData Central API for a specific food item.
 * Contains detailed nutrition information.
 */
data class UsdaFoodResponse(
    @SerializedName("fdcId")
    val fdcId: Int,

    @SerializedName("description")
    val description: String,

    @SerializedName("dataType")
    val dataType: String?,

    @SerializedName("foodNutrients")
    val foodNutrients: List<UsdaFoodNutrient>?,

    @SerializedName("servingSize")
    val servingSize: Double?,

    @SerializedName("servingSizeUnit")
    val servingSizeUnit: String?
)
