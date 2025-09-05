package com.example.checklist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChecklistEntity::class, SectionEntity::class, EntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checklistDao(): ChecklistDao
    abstract fun sectionDao(): SectionDao
    abstract fun entryDao(): EntryDao
    abstract fun aggregateDao(): AggregateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "checklist.db"
            ).build().also { INSTANCE = it }
        }
    }
}