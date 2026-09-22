package com.example.checklist.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test.db"
private const val EARLIEST_SHIPPED_VERSION = 1

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    // Fails as soon as the database version is bumped without a matching entry in ALL_MIGRATIONS.
    @Test
    fun migratesFromEarliestShippedVersionToCurrent() {
        helper.createDatabase(TEST_DB, EARLIEST_SHIPPED_VERSION).close()
        helper.runMigrationsAndValidate(TEST_DB, currentVersion(), true, *ALL_MIGRATIONS)
    }

    private fun currentVersion(): Int {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        return try {
            db.openHelper.readableDatabase.version
        } finally {
            db.close()
        }
    }
}
