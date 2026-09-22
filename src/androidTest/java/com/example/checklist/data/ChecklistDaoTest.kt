package com.example.checklist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChecklistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repo = Repository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun setEntryChecked_leavesOtherColumnsAlone() = runTest {
        val listId = repo.addChecklist("Trip")
        val entryId = repo.addEntry(listId, null, "original", 0)

        repo.updateEntry(
            EntryEntity(id = entryId, checklistId = listId, text = "renamed", orderIndex = 0)
        )
        repo.setEntryChecked(entryId, true)

        val entry = repo.checklistFull(listId).first()!!.entries.single()
        assertEquals("renamed", entry.text)
        assertTrue(entry.checked)
        assertEquals(0, entry.orderIndex)
    }

    @Test
    fun checklistFull_returnsEveryEntryExactlyOnce() = runTest {
        val listId = repo.addChecklist("Trip")
        val bag = repo.addSection(listId, "Bag", 0)
        repo.addEntry(listId, bag, "socks", 0)
        repo.addEntry(listId, bag, "shirt", 1)
        repo.addEntry(listId, null, "loose", 0)

        val full = repo.checklistFull(listId).first()!!

        assertEquals(1, full.sections.size)
        assertEquals(3, full.entries.size)
        assertEquals(setOf("socks", "shirt", "loose"), full.entries.map { it.text }.toSet())
    }

    @Test
    fun deletingChecklist_cascadesToSectionsAndEntries() = runTest {
        val listId = repo.addChecklist("Trip")
        val bag = repo.addSection(listId, "Bag", 0)
        repo.addEntry(listId, bag, "socks", 0)
        repo.addEntry(listId, null, "loose", 0)

        repo.deleteChecklist(listId)

        assertNull(repo.checklistFull(listId).first())
        assertEquals(0, db.sectionDao().observe(listId).first().size)
        assertEquals(0, db.entryDao().observe(listId).first().size)
    }

    @Test
    fun deletingSection_cascadesToItsEntriesOnly() = runTest {
        val listId = repo.addChecklist("Trip")
        val bag = repo.addSection(listId, "Bag", 0)
        repo.addEntry(listId, bag, "socks", 0)
        repo.addEntry(listId, null, "loose", 0)

        db.sectionDao().delete(SectionEntity(id = bag, checklistId = listId, title = "Bag", orderIndex = 0))

        val full = repo.checklistFull(listId).first()!!
        assertEquals(0, full.sections.size)
        assertEquals(listOf("loose"), full.entries.map { it.text })
    }

    @Test
    fun observeAll_returnsNewestFirst() = runTest {
        val older = ChecklistEntity(name = "older", createdAt = 1_000)
        val newer = ChecklistEntity(name = "newer", createdAt = 2_000)
        db.checklistDao().insert(older)
        db.checklistDao().insert(newer)

        assertEquals(listOf("newer", "older"), db.checklistDao().observeAll().first().map { it.name })
    }
}
