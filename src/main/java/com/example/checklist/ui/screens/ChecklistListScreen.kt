package com.example.checklist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.checklist.data.ChecklistEntity
import com.example.checklist.ui.screens.components.TextFieldDialog
import com.example.checklist.viewmodel.ChecklistViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistListScreen(
    onOpen: (Long) -> Unit,
    vm: ChecklistViewModel
) {
    val lists by vm.checklists.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember {mutableStateOf<ChecklistEntity?>(null) }

    var editChecklist by remember { mutableStateOf<ChecklistEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Checklists") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(lists, key = { it.id }) { item ->
                ChecklistRow(
                    item = item,
                    onOpen = onOpen,
                    onDeleteRequest = { deleteTarget = item },
                    onEditRequest = { editChecklist = item }
                )
            }
        }
    }

    if (showDialog) {
        TextFieldDialog(
            title = "New checklist",
            label = "Name",
            onConfirm = { name -> vm.addChecklist(name); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }

    val deleteCandidate = deleteTarget
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete checklist") },
            text = { Text("Are you sure you want to delete \"${deleteCandidate.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteChecklist(deleteCandidate.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    val editCandidate = editChecklist
    if (editCandidate != null) {
        TextFieldDialog(
            title = "Rename checklist",
            label = "Name",
            initialText = editCandidate.name,
            onConfirm = { text ->
                vm.renameChecklist(editCandidate, text)
                editChecklist = null
            },
            onDismiss = { editChecklist = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChecklistRow(
    item: ChecklistEntity,
    onOpen: (Long) -> Unit,
    onDeleteRequest: () -> Unit,
    onEditRequest: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                item.name,
                modifier = Modifier.clickable { onEditRequest() }
            )
        },
        supportingContent = {
            val createdText = DateFormat.getDateInstance().format(Date(item.createdAt))
            Text("Created • $createdText") },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onOpen(item.id) },
                onLongClick = { onDeleteRequest() }
            )
    )
    HorizontalDivider()
}