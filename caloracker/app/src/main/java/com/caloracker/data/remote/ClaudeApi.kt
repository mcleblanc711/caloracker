package com.caloracker.data.remote

import com.caloracker.data.remote.dto.ClaudeRequest
import com.caloracker.data.remote.dto.ClaudeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service interface for Claude API.
 *
 * API Documentation: https://docs.anthropic.com/claude/reference/messages_post
 *
 * SECURITY NOTE: Never log or hardcode API keys!
 */
interface ClaudeApi {

    /**
     * Send a message to Claude with vision capabilities.
     * Used for food image analysis.
     *
     * @param apiKey Claude API key from environment variable (via BuildConfig)
     * @param apiVersion API version (e.g., "2023-06-01")
     * @param request The Claude request with image and prompt
     * @return Response containing Claude's analysis
     */
    @POST("v1/messages")
    suspend fun analyzeFood(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") apiVersion: String,
        @Header("content-type") contentType: String = "application/json",
        @Body request: ClaudeRequest
    ): Response<ClaudeResponse>
}
