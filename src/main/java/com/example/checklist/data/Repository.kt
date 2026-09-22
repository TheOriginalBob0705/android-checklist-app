package com.example.checklist.data

import kotlinx.coroutines.flow.*

class Repository(private val db: AppDatabase) {
    val checklists: Flow<List<ChecklistEntity>> = db.checklistDao().observeAll()

    fun checklistFull(id: Long): Flow<ChecklistFull?> = db.aggregateDao().observeChecklist(id)

    suspend fun addChecklist(name: String): Long = db.checklistDao().insert(ChecklistEntity(name = name))

    suspend fun deleteChecklist(id: Long) {
        db.checklistDao().delete(ChecklistEntity(id = id, name = ""))
    }

    suspend fun addSection(checklistId: Long, title: String, orderIndex: Int) : Long =
        db.sectionDao().insert(SectionEntity(checklistId = checklistId, title = title, orderIndex = orderIndex))

    suspend fun addEntry(checklistId: Long, sectionId: Long?, text: String, orderIndex: Int) : Long =
        db.entryDao().insert(EntryEntity(checklistId = checklistId, sectionId = sectionId, text = text, orderIndex = orderIndex))

    suspend fun setEntryChecked(id: Long, checked: Boolean) {
        db.entryDao().setChecked(id, checked)
    }

    suspend fun deleteEntry(id: Long) {
        db.entryDao().delete(EntryEntity(id = id, checklistId = 0, text = "", orderIndex = 0))
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        db.checklistDao().update(checklist)
    }

    suspend fun updateSection(section: SectionEntity) {
        db.sectionDao().update(section)
    }

    suspend fun updateEntry(entry: EntryEntity) {
        db.entryDao().update(entry)
    }
}