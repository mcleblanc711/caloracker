package com.caloracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.caloracker.data.local.dao.FoodLogDao
import com.caloracker.data.local.entity.FoodLog

/**
 * Room database for Caloracker app.
 * Contains all entities and provides DAOs.
 */
@Database(
    entities = [FoodLog::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get the FoodLog DAO.
     */
    abstract fun foodLogDao(): FoodLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "caloracker.db"

        /**
         * Get the singleton database instance.
         * Thread-safe singleton pattern using double-checked locking.
         *
         * @param context Application context
         * @return The database instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing: get an in-memory database instance.
         * Data is cleared when the process is killed.
         *
         * @param context Context
         * @return In-memory database instance
         */
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries() // Only for testing
                .build()
        }
    }
}
