package com.example.checklist.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.checklist.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class SectionGroup(
    val section: SectionEntity,
    val entries: List<EntryEntity>
)

@Immutable
data class ChecklistDetail(
    val checklist: ChecklistEntity,
    val ungrouped: List<EntryEntity>,
    val sections: List<SectionGroup>
)

internal fun ChecklistFull.toDetail(): ChecklistDetail {
    val bySection = entries.groupBy { it.sectionId }
    fun ordered(list: List<EntryEntity>?) = list.orEmpty().sortedBy { it.orderIndex }
    return ChecklistDetail(
        checklist = checklist,
        ungrouped = ordered(bySection[null]),
        sections = sections
            .sortedBy { it.orderIndex }
            .map { section -> SectionGroup(section, ordered(bySection[section.id])) }
    )
}

class ChecklistViewModel(private val repo: Repository) : ViewModel() {
    val checklists = repo.checklists.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val details = mutableMapOf<Long, StateFlow<ChecklistDetail?>>()

    // Cached per id so recomposition reuses the subscription instead of restarting the query.
    fun checklistDetail(id: Long): StateFlow<ChecklistDetail?> = details.getOrPut(id) {
        repo.checklistFull(id)
            .map { it?.toDetail() }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, detailSharing, null)
    }

    fun addChecklist(name: String) { viewModelScope.launch { repo.addChecklist(name) } }

    fun addSection(checklistId: Long, title: String, orderIndex: Int) { viewModelScope.launch { repo.addSection(checklistId, title, orderIndex) } }

    fun addEntry(checklistId: Long, sectionId: Long?, text: String, orderIndex: Int) { viewModelScope.launch { repo.addEntry(checklistId, sectionId, text, orderIndex) } }

    fun toggle(entry: EntryEntity) { viewModelScope.launch { repo.setEntryChecked(entry.id, !entry.checked) } }

    fun deleteChecklist(id: Long) {
        viewModelScope.launch { repo.deleteChecklist(id) }
    }
    fun deleteEntry(id: Long) { viewModelScope.launch { repo.deleteEntry(id) } }

    fun renameChecklist(entity: ChecklistEntity, newName: String) {
        viewModelScope.launch { repo.updateChecklist(entity.copy(name = newName)) }
    }

    fun renameSection(section: SectionEntity, newTitle: String) {
        viewModelScope.launch { repo.updateSection(section.copy(title = newTitle)) }
    }

    fun renameEntry(entry: EntryEntity, newText: String) {
        viewModelScope.launch { repo.updateEntry(entry.copy(text = newText)) }
    }

    companion object {
        // Survives a rotation, then drops the cached rows so visited checklists don't accumulate.
        private val detailSharing = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = 5_000,
            replayExpirationMillis = 30_000
        )

        fun factory(db: AppDatabase) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChecklistViewModel(Repository(db)) as T
            }
        }
    }
}
