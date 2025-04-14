package com.example.foodguard.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Food::class, ScannedItem::class], version = 1) // Specify your entity and database version
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedItemDao(): ScannedItemDao
    abstract fun foodDao(): FoodDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "app_database" // Your database name
                            ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}