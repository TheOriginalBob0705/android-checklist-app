package com.example.checklist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.checklist.R
import com.example.checklist.data.*
import com.example.checklist.ui.screens.components.SectionHeader
import com.example.checklist.ui.screens.components.TextFieldDialog
import com.example.checklist.viewmodel.ChecklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    id: Long,
    onBack: () -> Unit,
    vm: ChecklistViewModel
) {
    val detail by remember(vm, id) { vm.checklistDetail(id) }.collectAsState()

    var showAddSection by remember { mutableStateOf(false) }
    var showAddUngrouped by remember { mutableStateOf(false) }
    var addForSectionId by remember { mutableStateOf<Long?>(null) }

    var editSection by remember { mutableStateOf<SectionEntity?>(null) }
    var editEntry by remember { mutableStateOf<EntryEntity?>(null) }

    var editChecklist by remember { mutableStateOf<ChecklistEntity?>(null) }

    // Stable instances so rows can skip recomposition.
    val onToggleEntry = remember(vm) { { entry: EntryEntity -> vm.toggle(entry) } }
    val onDeleteEntry = remember(vm) { { entry: EntryEntity -> vm.deleteEntry(entry.id) } }
    val onEditEntry = remember { { entry: EntryEntity -> editEntry = entry } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        detail?.checklist?.name ?: "",
                        modifier = Modifier.clickable {
                            detail?.checklist?.let { editChecklist = it }
                        }
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }
            )
        },
        bottomBar = {
            BottomAppBar(actions = {
                TextButton(onClick = { showAddSection = true }) { Text(stringResource(R.string.add_heading)) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showAddUngrouped = true }) { Text(stringResource(R.string.add_entry)) }
            })
        }
    ) { padding ->
        detail?.let { data ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                if (data.ungrouped.isNotEmpty()) {
                    item(key = "header-ungrouped") { SectionHeader(stringResource(R.string.ungrouped)) }
                    items(data.ungrouped, key = { it.id }) { entry ->
                        EntryRow(entry, onToggleEntry, onEditEntry, onDeleteEntry)
                    }
                }

                data.sections.forEach { group ->
                    item(key = "header-${group.section.id}") {
                        SectionHeader(
                            group.section.title,
                            onClick = { editSection = group.section }
                        )
                    }
                    items(group.entries, key = { it.id }) { entry ->
                        EntryRow(entry, onToggleEntry, onEditEntry, onDeleteEntry)
                    }
                    item(key = "add-${group.section.id}") {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { addForSectionId = group.section.id }) {
                            Text(stringResource(R.string.add_entry_in_section, group.section.title))
                        }
                    }
                }

                item(key = "bottom-spacer") { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showAddSection) {
        val order = (detail?.sections?.maxOfOrNull { it.section.orderIndex } ?: -1) + 1
        TextFieldDialog(
            title = stringResource(R.string.new_heading),
            label = stringResource(R.string.label_title),
            onConfirm = { title -> vm.addSection(id, title, order); showAddSection = false },
            onDismiss = { showAddSection = false }
        )
    }

    if (showAddUngrouped) {
        val order = (detail?.ungrouped?.maxOfOrNull { it.orderIndex } ?: -1) + 1
        TextFieldDialog(
            title = stringResource(R.string.new_entry),
            label = stringResource(R.string.label_text),
            onConfirm = { text -> vm.addEntry(id, null, text, order); showAddUngrouped = false },
            onDismiss = { showAddUngrouped = false }
        )
    }

    // Inline dialog for adding inside a section
    val targetSectionId = addForSectionId
    val currentDetail = detail
    if (targetSectionId != null && currentDetail != null) {
        val group = currentDetail.sections.firstOrNull { it.section.id == targetSectionId }
        val nextOrder = ((group?.entries?.maxOfOrNull { it.orderIndex }) ?: -1) + 1
        TextFieldDialog(
            title = stringResource(R.string.new_entry),
            label = stringResource(R.string.label_text),
            onConfirm = { text ->
                vm.addEntry(id, targetSectionId, text, nextOrder)
                addForSectionId = null
            },
            onDismiss = { addForSectionId = null }
        )
    }

    val targetSection = editSection
    if (targetSection != null) {
        TextFieldDialog(
            title = stringResource(R.string.rename_heading),
            label = stringResource(R.string.label_title),
            initialText = targetSection.title,
            onConfirm = { text ->
                vm.renameSection(targetSection, text)
                editSection = null
            },
            onDismiss = { editSection = null }
        )
    }

    val targetEntry = editEntry
    if (targetEntry != null) {
        TextFieldDialog(
            title = stringResource(R.string.edit_entry),
            label = stringResource(R.string.label_text),
            initialText = targetEntry.text,
            onConfirm = { text ->
                vm.renameEntry(targetEntry, text)
                editEntry = null
            },
            onDismiss = { editEntry = null }
        )
    }

    val targetChecklist = editChecklist
    if (targetChecklist != null) {
        TextFieldDialog(
            title = stringResource(R.string.rename_checklist),
            label = stringResource(R.string.label_name),
            initialText = targetChecklist.name,
            onConfirm = { text ->
                vm.renameChecklist(targetChecklist, text)
                editChecklist = null
            },
            onDismiss = { editChecklist = null }
        )
    }
}

@Composable
private fun EntryRow(
    entry: EntryEntity,
    onToggle: (EntryEntity) -> Unit,
    onEdit: (EntryEntity) -> Unit,
    onDelete: (EntryEntity) -> Unit
) {
    ListItem(
        leadingContent = { Checkbox(checked = entry.checked, onCheckedChange = { onToggle(entry) }) },
        headlineContent = { Text(entry.text, modifier = Modifier.clickable { onEdit(entry) }) },
        trailingContent = { TextButton(onClick = { onDelete(entry) }) { Text(stringResource(R.string.action_delete)) } }
    )
    HorizontalDivider()
}
