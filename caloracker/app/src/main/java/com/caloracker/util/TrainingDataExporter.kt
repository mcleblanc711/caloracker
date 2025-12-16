package com.caloracker.util

import android.content.Context
import android.util.Log
import com.caloracker.data.local.AppDatabase
import com.caloracker.data.local.dao.FallbackCount
import com.caloracker.data.local.entity.CloudFallbackLog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting cloud fallback logs for training data collection.
 * Helps identify foods that TFLite model doesn't recognize well.
 */
class TrainingDataExporter(context: Context) {

    private val cloudFallbackLogDao = AppDatabase.getDatabase(context).cloudFallbackLogDao()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val filesDir = context.filesDir

    companion object {
        private const val TAG = "TrainingDataExporter"
        private const val EXPORT_FOLDER = "training_data_exports"
        private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
    }

    /**
     * Get a summary of foods that needed cloud fallback in the last week.
     * Useful for identifying model gaps.
     */
    suspend fun getWeeklySummary(): WeeklySummary = withContext(Dispatchers.IO) {
        val oneWeekAgo = System.currentTimeMillis() - SEVEN_DAYS_MS

        val totalCount = cloudFallbackLogDao.getCountSince(oneWeekAgo)
        val foodCounts = cloudFallbackLogDao.getWeeklySummary(oneWeekAgo)
        val logs = cloudFallbackLogDao.getLogsSince(oneWeekAgo)

        // Group by fallback reason
        val reasonBreakdown = logs.groupBy { it.fallbackReason }
            .mapValues { it.value.size }

        WeeklySummary(
            startDate = Date(oneWeekAgo),
            endDate = Date(),
            totalFallbacks = totalCount,
            uniqueFoods = foodCounts.size,
            topFoods = foodCounts.take(10),
            reasonBreakdown = reasonBreakdown
        )
    }

    /**
     * Export weekly fallback data to JSON file for analysis.
     * Returns the file path.
     */
    suspend fun exportWeeklyData(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val oneWeekAgo = System.currentTimeMillis() - SEVEN_DAYS_MS
            val logs = cloudFallbackLogDao.getLogsSince(oneWeekAgo)

            if (logs.isEmpty()) {
                return@withContext ExportResult.NoData
            }

            // Create export directory
            val exportDir = File(filesDir, EXPORT_FOLDER)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Create export file with timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
            val fileName = "fallback_export_${dateFormat.format(Date())}.json"
            val exportFile = File(exportDir, fileName)

            // Create export data structure
            val exportData = ExportData(
                exportDate = Date(),
                periodStart = Date(oneWeekAgo),
                periodEnd = Date(),
                totalEntries = logs.size,
                entries = logs.map { it.toExportEntry() }
            )

            // Write to file
            exportFile.writeText(gson.toJson(exportData))

            // Mark logs as exported
            val logIds = logs.map { it.id }
            cloudFallbackLogDao.markAsExported(logIds)

            Log.d(TAG, "Exported ${logs.size} fallback logs to ${exportFile.absolutePath}")
            ExportResult.Success(exportFile.absolutePath, logs.size)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export fallback data", e)
            ExportResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get unexported logs (for incremental exports).
     */
    suspend fun getUnexportedLogs(): List<CloudFallbackLog> = withContext(Dispatchers.IO) {
        cloudFallbackLogDao.getUnexportedLogs()
    }

    /**
     * Get the most common foods that need cloud fallback.
     * These are the highest priority for adding to training data.
     */
    suspend fun getMostCommonGaps(limit: Int = 20): List<FallbackCount> = withContext(Dispatchers.IO) {
        cloudFallbackLogDao.getMostCommonFallbacks(limit)
    }

    /**
     * Generate a human-readable report.
     */
    suspend fun generateReport(): String = withContext(Dispatchers.IO) {
        val summary = getWeeklySummary()
        val sb = StringBuilder()

        sb.appendLine("=" .repeat(50))
        sb.appendLine("WEEKLY CLOUD FALLBACK REPORT")
        sb.appendLine("=" .repeat(50))
        sb.appendLine()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sb.appendLine("Period: ${dateFormat.format(summary.startDate)} - ${dateFormat.format(summary.endDate)}")
        sb.appendLine()

        sb.appendLine("SUMMARY")
        sb.appendLine("-".repeat(30))
        sb.appendLine("Total fallbacks: ${summary.totalFallbacks}")
        sb.appendLine("Unique foods: ${summary.uniqueFoods}")
        sb.appendLine()

        if (summary.topFoods.isNotEmpty()) {
            sb.appendLine("TOP FOODS NEEDING CLOUD FALLBACK")
            sb.appendLine("-".repeat(30))
            summary.topFoods.forEachIndexed { index, food ->
                sb.appendLine("${index + 1}. ${food.foodName} (${food.count} times)")
            }
            sb.appendLine()
        }

        if (summary.reasonBreakdown.isNotEmpty()) {
            sb.appendLine("FALLBACK REASONS")
            sb.appendLine("-".repeat(30))
            summary.reasonBreakdown.forEach { (reason, count) ->
                val readableReason = when (reason) {
                    "low_confidence" -> "Low confidence"
                    "no_prediction" -> "No prediction"
                    "tflite_error" -> "TFLite error"
                    "tflite_not_initialized" -> "TFLite not ready"
                    else -> reason
                }
                sb.appendLine("$readableReason: $count")
            }
            sb.appendLine()
        }

        sb.appendLine("=" .repeat(50))
        sb.appendLine("Consider adding these foods to training data")
        sb.appendLine("=" .repeat(50))

        sb.toString()
    }

    /**
     * Clean up old exported logs (older than 30 days).
     */
    suspend fun cleanupOldLogs(): Int = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        cloudFallbackLogDao.deleteOldExportedLogs(thirtyDaysAgo)
    }

    // Data classes for export

    data class WeeklySummary(
        val startDate: Date,
        val endDate: Date,
        val totalFallbacks: Int,
        val uniqueFoods: Int,
        val topFoods: List<FallbackCount>,
        val reasonBreakdown: Map<String, Int>
    )

    data class ExportData(
        val exportDate: Date,
        val periodStart: Date,
        val periodEnd: Date,
        val totalEntries: Int,
        val entries: List<ExportEntry>
    )

    data class ExportEntry(
        val timestamp: Long,
        val foodName: String,
        val localPrediction: String?,
        val localConfidence: Float,
        val fallbackReason: String,
        val imageUri: String?
    )

    sealed class ExportResult {
        data class Success(val filePath: String, val count: Int) : ExportResult()
        data class Error(val message: String) : ExportResult()
        object NoData : ExportResult()
    }

    private fun CloudFallbackLog.toExportEntry() = ExportEntry(
        timestamp = timestamp,
        foodName = foodName,
        localPrediction = localPrediction,
        localConfidence = localConfidence,
        fallbackReason = fallbackReason,
        imageUri = imageUri
    )
}
