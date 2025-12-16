package com.caloracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request structure for Claude API Messages endpoint.
 * Used to send vision requests with food images.
 *
 * API Documentation: https://docs.anthropic.com/claude/reference/messages_post
 */
data class ClaudeRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("max_tokens")
    val maxTokens: Int,

    @SerializedName("messages")
    val messages: List<Message>
)

/**
 * Message in the conversation.
 */
data class Message(
    @SerializedName("role")
    val role: String, // "user" or "assistant"

    @SerializedName("content")
    val content: List<ContentBlock>
)

/**
 * Content block - can be text or image.
 */
sealed class ContentBlock {
    /**
     * Text content block.
     */
    data class Text(
        @SerializedName("type")
        val type: String = "text",

        @SerializedName("text")
        val text: String
    ) : ContentBlock()

    /**
     * Image content block with base64-encoded image data.
     */
    data class Image(
        @SerializedName("type")
        val type: String = "image",

        @SerializedName("source")
        val source: ImageSource
    ) : ContentBlock()
}

/**
 * Image source with base64 data.
 */
data class ImageSource(
    @SerializedName("type")
    val type: String = "base64",

    @SerializedName("media_type")
    val mediaType: String, // e.g., "image/jpeg", "image/png"

    @SerializedName("data")
    val data: String // Base64-encoded image data
)

/**
 * Helper function to create a food analysis request.
 *
 * @param model The Claude model to use
 * @param base64Image Base64-encoded image data
 * @param mediaType Image media type (e.g., "image/jpeg")
 * @return ClaudeRequest configured for food analysis
 */
fun createFoodAnalysisRequest(
    model: String,
    base64Image: String,
    mediaType: String = "image/jpeg"
): ClaudeRequest {
    val prompt = """
        Analyze this food image and provide a detailed nutrition analysis.

        Please identify:
        1. The primary food item(s) you see
        2. 3-5 alternative possibilities if there's any ambiguity
        3. Estimated portion size
        4. Nutritional information per serving:
           - Calories (kcal)
           - Protein (grams)
           - Carbohydrates (grams)
           - Fat (grams)

        Format your response as JSON with this structure:
        {
          "primaryFood": {
            "name": "food name",
            "portion": "estimated portion size",
            "calories": number,
            "protein": number,
            "carbs": number,
            "fat": number
          },
          "alternatives": [
            {
              "name": "alternative food name",
              "portion": "estimated portion size",
              "calories": number,
              "protein": number,
              "carbs": number,
              "fat": number
            }
          ]
        }

        Provide only the JSON response, no additional text.
    """.trimIndent()

    return ClaudeRequest(
        model = model,
        maxTokens = 1024,
        messages = listOf(
            Message(
                role = "user",
                content = listOf(
                    ContentBlock.Image(
                        source = ImageSource(
                            mediaType = mediaType,
                            data = base64Image
                        )
                    ),
                    ContentBlock.Text(
                        text = prompt
                    )
                )
            )
        )
    )
}
