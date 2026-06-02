package com.example.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReportHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
