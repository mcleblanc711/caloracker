package com.caloracker.data.remote

import com.caloracker.data.remote.dto.UsdaSearchResponse
import com.caloracker.data.remote.dto.UsdaFoodResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for USDA FoodData Central API.
 *
 * API Documentation: https://fdc.nal.usda.gov/api-guide.html
 *
 * NOTE: This is a free, public API that requires no authentication!
 * No API keys needed - completely free to use.
 */
interface UsdaApi {

    /**
     * Search for foods by name.
     * Returns a list of foods matching the search query with basic nutrition info.
     *
     * @param query Food name to search for (e.g., "apple", "chicken breast")
     * @param dataType Filter by data type (Foundation, SR Legacy, etc.)
     * @param pageSize Number of results to return (max 200)
     * @param pageNumber Page number for pagination
     * @return Response containing list of matching foods
     */
    @GET("foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("dataType") dataType: String = "Foundation,SR Legacy",
        @Query("pageSize") pageSize: Int = 25,
        @Query("pageNumber") pageNumber: Int = 1
    ): Response<UsdaSearchResponse>

    /**
     * Get detailed nutrition information for a specific food by FDC ID.
     *
     * @param fdcId The FDC ID of the food item
     * @param format Response format (full or abridged)
     * @return Response containing detailed food and nutrition information
     */
    @GET("food/{fdcId}")
    suspend fun getFoodDetails(
        @Path("fdcId") fdcId: Int,
        @Query("format") format: String = "abridged"
    ): Response<UsdaFoodResponse>
}
