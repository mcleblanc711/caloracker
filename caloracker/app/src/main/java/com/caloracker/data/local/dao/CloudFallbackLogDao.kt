package com.caloracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.caloracker.data.local.entity.CloudFallbackLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for cloud fallback logs.
 * Provides methods to track and export foods that required cloud API fallback.
 */
@Dao
interface CloudFallbackLogDao {

    /**
     * Insert a new fallback log entry.
     */
    @Insert
    suspend fun insert(log: CloudFallbackLog): Long

    /**
     * Get all fallback logs (for debugging/admin).
     */
    @Query("SELECT * FROM cloud_fallback_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CloudFallbackLog>>

    /**
     * Get fallback logs within a date range.
     */
    @Query("""
        SELECT * FROM cloud_fallback_logs
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp DESC
    """)
    suspend fun getLogsBetween(startTime: Long, endTime: Long): List<CloudFallbackLog>

    /**
     * Get logs from the last N days.
     */
    @Query("""
        SELECT * FROM cloud_fallback_logs
        WHERE timestamp >= :sinceTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun getLogsSince(sinceTimestamp: Long): List<CloudFallbackLog>

    /**
     * Get unexported logs for training data collection.
     */
    @Query("SELECT * FROM cloud_fallback_logs WHERE exported = 0 ORDER BY timestamp DESC")
    suspend fun getUnexportedLogs(): List<CloudFallbackLog>

    /**
     * Mark logs as exported.
     */
    @Query("UPDATE cloud_fallback_logs SET exported = 1 WHERE id IN (:ids)")
    suspend fun markAsExported(ids: List<Long>)

    /**
     * Get count of fallback logs by food name (to identify most common gaps).
     */
    @Query("""
        SELECT foodName, COUNT(*) as count
        FROM cloud_fallback_logs
        GROUP BY foodName
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getMostCommonFallbacks(limit: Int = 20): List<FallbackCount>

    /**
     * Get count of fallback logs from the last week.
     */
    @Query("SELECT COUNT(*) FROM cloud_fallback_logs WHERE timestamp >= :sinceTimestamp")
    suspend fun getCountSince(sinceTimestamp: Long): Int

    /**
     * Get weekly summary grouped by food name.
     */
    @Query("""
        SELECT foodName, COUNT(*) as count
        FROM cloud_fallback_logs
        WHERE timestamp >= :sinceTimestamp
        GROUP BY foodName
        ORDER BY count DESC
    """)
    suspend fun getWeeklySummary(sinceTimestamp: Long): List<FallbackCount>

    /**
     * Delete old logs (cleanup).
     */
    @Query("DELETE FROM cloud_fallback_logs WHERE timestamp < :beforeTimestamp AND exported = 1")
    suspend fun deleteOldExportedLogs(beforeTimestamp: Long): Int

    /**
     * Delete all logs.
     */
    @Query("DELETE FROM cloud_fallback_logs")
    suspend fun deleteAll()
}

/**
 * Data class for fallback count aggregation.
 */
data class FallbackCount(
    val foodName: String,
    val count: Int
)
