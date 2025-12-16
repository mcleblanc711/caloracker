package com.caloracker.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for date and time operations.
 */
object DateUtils {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Get start of day timestamp (00:00:00) for a given date.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Start of day timestamp
     */
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get end of day timestamp (23:59:59.999) for a given date.
     *
     * @param timestamp Timestamp in milliseconds
     * @return End of day timestamp
     */
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Get current timestamp.
     *
     * @return Current time in milliseconds
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Get start of today.
     *
     * @return Start of today timestamp
     */
    fun getStartOfToday(): Long {
        return getStartOfDay(getCurrentTimestamp())
    }

    /**
     * Get end of today.
     *
     * @return End of today timestamp
     */
    fun getEndOfToday(): Long {
        return getEndOfDay(getCurrentTimestamp())
    }

    /**
     * Format timestamp as date string.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date string (e.g., "Jan 15, 2025")
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp as date and time string.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date-time string (e.g., "Jan 15, 2025 14:30")
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp as time string.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted time string (e.g., "14:30")
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    /**
     * Check if a timestamp is today.
     *
     * @param timestamp Timestamp to check
     * @return True if timestamp is today, false otherwise
     */
    fun isToday(timestamp: Long): Boolean {
        val today = getStartOfToday()
        val tomorrow = getStartOfDay(today + 24 * 60 * 60 * 1000)
        return timestamp >= today && timestamp < tomorrow
    }

    /**
     * Get relative date string (e.g., "Today", "Yesterday", or formatted date).
     *
     * @param timestamp Timestamp in milliseconds
     * @return Relative date string
     */
    fun getRelativeDateString(timestamp: Long): String {
        val startOfDay = getStartOfDay(timestamp)
        val today = getStartOfToday()

        return when {
            startOfDay == today -> "Today"
            startOfDay == today - 24 * 60 * 60 * 1000 -> "Yesterday"
            else -> formatDate(timestamp)
        }
    }
}
