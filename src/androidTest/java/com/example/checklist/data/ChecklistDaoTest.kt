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
    fun summaries_countCheckedEntriesPerChecklist() = runTest {
        val listId = repo.addChecklist("Trip")
        val a = repo.addEntry(listId, null, "a", 0)
        repo.addEntry(listId, null, "b", 1)
        repo.setEntryChecked(a, true)
        repo.addChecklist("Empty")

        val summaries = repo.summaries.first().associateBy { it.checklist.name }

        assertEquals(2, summaries.getValue("Trip").total)
        assertEquals(1, summaries.getValue("Trip").done)
        assertEquals(0, summaries.getValue("Empty").total)
        assertEquals(0, summaries.getValue("Empty").done)
    }

    @Test
    fun applyEntryPlacements_movesAnEntryIntoAnotherSection() = runTest {
        val listId = repo.addChecklist("Trip")
        val bag = repo.addSection(listId, "Bag", 0)
        val loose = repo.addEntry(listId, null, "loose", 0)
        val socks = repo.addEntry(listId, bag, "socks", 0)

        repo.applyEntryPlacements(
            listOf(
                EntryPlacement(socks, bag, 0),
                EntryPlacement(loose, bag, 1)
            )
        )

        val full = repo.checklistFull(listId).first()!!
        val moved = full.entries.single { it.id == loose }
        assertEquals(bag, moved.sectionId)
        assertEquals(1, moved.orderIndex)
    }

    @Test
    fun deleteSection_removesOnlyItsOwnEntries() = runTest {
        val listId = repo.addChecklist("Trip")
        val bag = repo.addSection(listId, "Bag", 0)
        val docs = repo.addSection(listId, "Docs", 1)
        repo.addEntry(listId, bag, "socks", 0)
        repo.addEntry(listId, docs, "passport", 0)
        repo.addEntry(listId, null, "loose", 0)

        repo.deleteSection(bag)

        val full = repo.checklistFull(listId).first()!!
        assertEquals(listOf("Docs"), full.sections.map { it.title })
        assertEquals(setOf("passport", "loose"), full.entries.map { it.text }.toSet())
    }

    @Test
    fun restoreEntry_bringsBackTheSameRow() = runTest {
        val listId = repo.addChecklist("Trip")
        val entryId = repo.addEntry(listId, null, "socks", 3)
        repo.setEntryChecked(entryId, true)
        val original = repo.checklistFull(listId).first()!!.entries.single()

        repo.deleteEntry(entryId)
        assertEquals(0, repo.checklistFull(listId).first()!!.entries.size)

        repo.restoreEntry(original)

        val restored = repo.checklistFull(listId).first()!!.entries.single()
        assertEquals(original, restored)
    }

    @Test
    fun applySectionOrder_renumbersSections() = runTest {
        val listId = repo.addChecklist("Trip")
        val bag = repo.addSection(listId, "Bag", 0)
        val docs = repo.addSection(listId, "Docs", 1)

        repo.applySectionOrder(listOf(docs, bag))

        val full = repo.checklistFull(listId).first()!!
        assertEquals(
            listOf("Docs", "Bag"),
            full.sections.sortedBy { it.orderIndex }.map { it.title }
        )
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
