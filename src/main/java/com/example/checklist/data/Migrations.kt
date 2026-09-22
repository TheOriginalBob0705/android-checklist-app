package com.example.checklist.data

import androidx.room.migration.Migration

/**
 * Every schema change needs an entry here and a bump of [AppDatabase]'s version.
 * Without one, existing installs crash on launch.
 *
 * val MIGRATION_1_2 = Migration(1, 2) { db ->
 *     db.execSQL("ALTER TABLE entries ADD COLUMN note TEXT DEFAULT NULL")
 * }
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf()
