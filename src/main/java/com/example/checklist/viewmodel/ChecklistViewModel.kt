package com.example.checklist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklist.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChecklistViewModel(private val repo: Repository) : ViewModel() {
    val checklists = repo.checklists.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun checklistFull(id: Long) = repo.checklistFull(id)

    fun addChecklist(name: String) { viewModelScope.launch { repo.addChecklist(name) } }

    fun addSection(checklistId: Long, title: String, orderIndex: Int) { viewModelScope.launch { repo.addSection(checklistId, title, orderIndex) } }

    fun addEntry(checklistId: Long, sectionId: Long?, text: String, orderIndex: Int) { viewModelScope.launch { repo.addEntry(checklistId, sectionId, text, orderIndex) } }

    fun toggle(entry: EntryEntity) { viewModelScope.launch { repo.toggleEntry(entry.id, !entry.checked, entry.checklistId, entry.sectionId, entry.text, entry.orderIndex) } }

    fun deleteChecklist(id: Long) {
        viewModelScope.launch { repo.deleteChecklist(id) }
    }
    fun deleteEntry(id: Long) { viewModelScope.launch { repo.deleteEntry(id) } }
}