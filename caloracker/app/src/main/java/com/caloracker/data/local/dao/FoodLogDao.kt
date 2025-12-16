package com.caloracker.data.local.dao

import androidx.room.*
import com.caloracker.data.local.entity.FoodLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FoodLog entity.
 * Provides methods to interact with the food_logs table.
 */
@Dao
interface FoodLogDao {

    /**
     * Insert a new food log entry.
     * @param foodLog The food log to insert
     * @return The row ID of the inserted food log
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodLog: FoodLog): Long

    /**
     * Insert multiple food log entries.
     * @param foodLogs List of food logs to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foodLogs: List<FoodLog>)

    /**
     * Update an existing food log entry.
     * @param foodLog The food log to update
     */
    @Update
    suspend fun update(foodLog: FoodLog)

    /**
     * Delete a food log entry.
     * @param foodLog The food log to delete
     */
    @Delete
    suspend fun delete(foodLog: FoodLog)

    /**
     * Delete a food log by its ID.
     * @param id The ID of the food log to delete
     */
    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Get a food log by its ID.
     * @param id The ID of the food log
     * @return The food log, or null if not found
     */
    @Query("SELECT * FROM food_logs WHERE id = :id")
    suspend fun getById(id: Long): FoodLog?

    /**
     * Get all food logs for a specific date.
     * Returns a Flow that emits whenever the data changes.
     * @param startOfDay Timestamp for start of day (00:00:00)
     * @param endOfDay Timestamp for end of day (23:59:59)
     * @return Flow of list of food logs for the specified date
     */
    @Query("SELECT * FROM food_logs WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date DESC")
    fun getLogsForDate(startOfDay: Long, endOfDay: Long): Flow<List<FoodLog>>

    /**
     * Get all food logs.
     * Returns a Flow that emits whenever the data changes.
     * @return Flow of list of all food logs, ordered by date descending
     */
    @Query("SELECT * FROM food_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<FoodLog>>

    /**
     * Get total calories for a specific date.
     * @param startOfDay Timestamp for start of day
     * @param endOfDay Timestamp for end of day
     * @return Total calories consumed on the date, or 0 if no logs
     */
    @Query("SELECT COALESCE(SUM(calories), 0) FROM food_logs WHERE date >= :startOfDay AND date <= :endOfDay")
    suspend fun getTotalCaloriesForDate(startOfDay: Long, endOfDay: Long): Double

    /**
     * Get total macros (protein, carbs, fat) for a specific date.
     * @param startOfDay Timestamp for start of day
     * @param endOfDay Timestamp for end of day
     * @return Map with "protein", "carbs", "fat" keys and their totals
     */
    @Query("SELECT COALESCE(SUM(protein), 0) as protein, COALESCE(SUM(carbs), 0) as carbs, COALESCE(SUM(fat), 0) as fat FROM food_logs WHERE date >= :startOfDay AND date <= :endOfDay")
    suspend fun getTotalMacrosForDate(startOfDay: Long, endOfDay: Long): MacroTotals

    /**
     * Get list of distinct dates that have food logs.
     * @return List of date timestamps (start of day)
     */
    @Query("SELECT DISTINCT date FROM food_logs ORDER BY date DESC")
    suspend fun getDistinctDates(): List<Long>

    /**
     * Delete all food logs (for testing purposes).
     */
    @Query("DELETE FROM food_logs")
    suspend fun deleteAll()
}

/**
 * Data class to hold macro totals.
 */
data class MacroTotals(
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
