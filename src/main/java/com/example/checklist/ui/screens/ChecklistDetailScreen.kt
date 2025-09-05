package com.example.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val full by vm.checklistFull(id).collectAsState(initial = null)

    var showAddSection by remember { mutableStateOf(false) }
    var showAddUngrouped by remember { mutableStateOf(false) }
    var addForSectionId by remember { mutableStateOf<Long?>(null) } // <-- new

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(full?.checklist?.name ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        },
        bottomBar = {
            BottomAppBar(actions = {
                TextButton(onClick = { showAddSection = true }) { Text("Add heading") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showAddUngrouped = true }) { Text("Add entry") }
            })
        }
    ) { padding ->
        full?.let { data ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                if (data.ungrouped.isNotEmpty()) {
                    item { SectionHeader("Ungrouped") }
                    items(data.ungrouped.sortedBy { it.orderIndex }, key = { it.id }) { entry ->
                        EntryRow(entry, vm)
                    }
                }

                val sections = data.sections.sortedBy { it.section.orderIndex }
                sections.forEach { sw ->
                    item { SectionHeader(sw.section.title) }
                    items(sw.entries.sortedBy { it.orderIndex }, key = { it.id }) { entry ->
                        EntryRow(entry, vm)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        TextButton(onClick = { addForSectionId = sw.section.id }) {
                            Text("Add entry in \"${sw.section.title}\"")
                        }
                    }
                }

                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showAddSection) {
        val order = (full?.sections?.maxOfOrNull { it.section.orderIndex } ?: -1) + 1
        TextFieldDialog(
            title = "New heading",
            label = "Title",
            onConfirm = { title -> vm.addSection(id, title, order); showAddSection = false },
            onDismiss = { showAddSection = false }
        )
    }

    if (showAddUngrouped) {
        val order = (full?.ungrouped?.maxOfOrNull { it.orderIndex } ?: -1) + 1
        TextFieldDialog(
            title = "New entry",
            label = "Text",
            onConfirm = { text -> vm.addEntry(id, null, text, order); showAddUngrouped = false },
            onDismiss = { showAddUngrouped = false }
        )
    }

    // Inline dialog for adding inside a section
    val targetSectionId = addForSectionId
    if (targetSectionId != null && full != null) {
        val sw = full!!.sections.firstOrNull { it.section.id == targetSectionId }
        val nextOrder = ((sw?.entries?.maxOfOrNull { it.orderIndex }) ?: -1) + 1
        TextFieldDialog(
            title = "New entry",
            label = "Text",
            onConfirm = { text ->
                vm.addEntry(id, targetSectionId, text, nextOrder)
                addForSectionId = null
            },
            onDismiss = { addForSectionId = null }
        )
    }
}

@Composable
private fun EntryRow(entry: EntryEntity, vm: ChecklistViewModel) {
    ListItem(
        leadingContent = { Checkbox(checked = entry.checked, onCheckedChange = { vm.toggle(entry) }) },
        headlineContent = { Text(entry.text) },
        trailingContent = { TextButton(onClick = { vm.deleteEntry(entry.id) }) { Text("Delete") } }
    )
    HorizontalDivider()
}