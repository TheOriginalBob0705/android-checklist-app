package com.example.checklist.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.*

class Repository(private val db: AppDatabase) {
    val checklists: Flow<List<ChecklistEntity>> = db.checklistDao().observeAll()

    val summaries: Flow<List<ChecklistSummary>> = db.checklistDao().observeSummaries()

    fun checklistFull(id: Long): Flow<ChecklistFull?> = db.aggregateDao().observeChecklist(id)

    suspend fun addChecklist(name: String): Long = db.checklistDao().insert(ChecklistEntity(name = name))

    suspend fun deleteChecklist(id: Long) {
        db.checklistDao().delete(ChecklistEntity(id = id, name = ""))
    }

    suspend fun addSection(checklistId: Long, title: String, orderIndex: Int) : Long =
        db.sectionDao().insert(SectionEntity(checklistId = checklistId, title = title, orderIndex = orderIndex))

    suspend fun deleteSection(id: Long) {
        db.sectionDao().deleteById(id)
    }

    suspend fun addEntry(checklistId: Long, sectionId: Long?, text: String, orderIndex: Int) : Long =
        db.entryDao().insert(EntryEntity(checklistId = checklistId, sectionId = sectionId, text = text, orderIndex = orderIndex))

    suspend fun setEntryChecked(id: Long, checked: Boolean) {
        db.entryDao().setChecked(id, checked)
    }

    suspend fun deleteEntry(id: Long) {
        db.entryDao().deleteById(id)
    }

    // Keeps the original id so an undo restores the row exactly where it was.
    suspend fun restoreEntry(entry: EntryEntity) {
        db.entryDao().restore(entry)
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        db.checklistDao().update(checklist)
    }

    suspend fun updateSection(section: SectionEntity) {
        db.sectionDao().update(section)
    }

    suspend fun updateEntry(entry: EntryEntity) {
        db.entryDao().updateEntry(entry)
    }

    suspend fun applyEntryPlacements(placements: List<EntryPlacement>) = db.withTransaction {
        placements.forEach { db.entryDao().setPlacement(it.id, it.sectionId, it.orderIndex) }
    }

    suspend fun applySectionOrder(idsInOrder: List<Long>) = db.withTransaction {
        idsInOrder.forEachIndexed { index, id -> db.sectionDao().setOrder(id, index) }
    }
}

data class EntryPlacement(val id: Long, val sectionId: Long?, val orderIndex: Int)
