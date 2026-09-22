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
) {
    val total: Int = ungrouped.size + sections.sumOf { it.entries.size }
    val done: Int = ungrouped.count { it.checked } + sections.sumOf { g -> g.entries.count { it.checked } }
}

// One flat list so drag indices, which the list reports as absolute positions, line up with the model.
@Immutable
sealed interface DetailRow {
    val key: String

    @Immutable
    data class Header(val section: SectionEntity?) : DetailRow {
        override val key get() = "header-${section?.id ?: 0L}"
    }

    @Immutable
    data class Entry(val entry: EntryEntity) : DetailRow {
        override val key get() = "entry-${entry.id}"
    }

    @Immutable
    data class AddEntry(val sectionId: Long?, val sectionTitle: String?) : DetailRow {
        override val key get() = "add-${sectionId ?: 0L}"
    }

    @Immutable
    data object BottomSpace : DetailRow {
        override val key get() = "bottom-space"
    }
}

fun ChecklistDetail.toRows(): List<DetailRow> = buildList {
    if (sections.isNotEmpty() || ungrouped.isNotEmpty()) {
        if (sections.isNotEmpty()) add(DetailRow.Header(null))
        ungrouped.forEach { add(DetailRow.Entry(it)) }
        if (sections.isNotEmpty()) add(DetailRow.AddEntry(null, null))
    }
    sections.forEach { group ->
        add(DetailRow.Header(group.section))
        group.entries.forEach { add(DetailRow.Entry(it)) }
        add(DetailRow.AddEntry(group.section.id, group.section.title))
    }
    add(DetailRow.BottomSpace)
}

/** Returns [rows] unchanged when the move is not a legal one, so callers can apply it blindly. */
fun moveRow(rows: List<DetailRow>, from: Int, to: Int): List<DetailRow> {
    if (from !in rows.indices || to !in rows.indices || from == to) return rows
    if (rows[from] !is DetailRow.Entry) return rows
    if (rows[to] is DetailRow.BottomSpace) return rows
    if (to == 0 && rows.first() is DetailRow.Header) return rows
    return rows.toMutableList().apply { add(to, removeAt(from)) }
}

/** Re-derives each entry's section and position from where it now sits in the flat list. */
fun List<DetailRow>.toPlacements(): List<EntryPlacement> {
    val placements = mutableListOf<EntryPlacement>()
    var sectionId: Long? = null
    var order = 0
    forEach { row ->
        when (row) {
            is DetailRow.Header -> {
                sectionId = row.section?.id
                order = 0
            }
            is DetailRow.Entry -> {
                placements += EntryPlacement(row.entry.id, sectionId, order)
                order++
            }
            else -> Unit
        }
    }
    return placements
}

class ChecklistViewModel(private val repo: Repository) : ViewModel() {
    val summaries = repo.summaries.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun deleteChecklist(id: Long) { viewModelScope.launch { repo.deleteChecklist(id) } }

    fun deleteSection(id: Long) { viewModelScope.launch { repo.deleteSection(id) } }

    fun deleteEntry(id: Long) { viewModelScope.launch { repo.deleteEntry(id) } }

    fun restoreEntry(entry: EntryEntity) { viewModelScope.launch { repo.restoreEntry(entry) } }

    fun renameChecklist(entity: ChecklistEntity, newName: String) {
        viewModelScope.launch { repo.updateChecklist(entity.copy(name = newName)) }
    }

    fun renameSection(section: SectionEntity, newTitle: String) {
        viewModelScope.launch { repo.updateSection(section.copy(title = newTitle)) }
    }

    fun renameEntry(entry: EntryEntity, newText: String) {
        viewModelScope.launch { repo.updateEntry(entry.copy(text = newText)) }
    }

    fun applyRowOrder(rows: List<DetailRow>) {
        viewModelScope.launch { repo.applyEntryPlacements(rows.toPlacements()) }
    }

    fun moveSection(detail: ChecklistDetail, section: SectionEntity, by: Int) {
        val ids = detail.sections.map { it.section.id }.toMutableList()
        val from = ids.indexOf(section.id)
        val to = from + by
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        viewModelScope.launch { repo.applySectionOrder(ids) }
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
