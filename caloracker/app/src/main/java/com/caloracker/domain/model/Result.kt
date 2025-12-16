package com.caloracker.domain.model

/**
 * Sealed class representing the result of an operation.
 * Used for error handling throughout the app.
 */
sealed class Result<out T> {
    /**
     * Successful result with data.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Error result with message.
     */
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()

    /**
     * Loading state.
     */
    object Loading : Result<Nothing>()
}

/**
 * Extension function to check if result is success.
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Extension function to check if result is error.
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Extension function to get data or null.
 */
fun <T> Result<T>.dataOrNull(): T? = (this as? Result.Success)?.data

/**
 * Extension function to get error message or null.
 */
fun <T> Result<T>.errorOrNull(): String? = (this as? Result.Error)?.message
