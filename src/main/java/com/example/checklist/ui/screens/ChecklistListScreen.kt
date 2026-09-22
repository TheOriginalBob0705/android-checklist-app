package com.example.checklist.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.checklist.R
import com.example.checklist.data.ChecklistEntity
import com.example.checklist.data.ChecklistSummary
import com.example.checklist.ui.screens.components.EmptyState
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
    val lists by vm.summaries.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChecklistEntity?>(null) }
    var editChecklist by remember { mutableStateOf<ChecklistEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.checklists_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_checklist))
            }
        }
    ) { padding ->
        if (lists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.empty_checklists_title),
                    body = stringResource(R.string.empty_checklists_body)
                )
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(lists, key = { it.checklist.id }) { item ->
                    ChecklistRow(
                        item = item,
                        onOpen = onOpen,
                        onDeleteRequest = { deleteTarget = item.checklist },
                        onEditRequest = { editChecklist = item.checklist }
                    )
                }
            }
        }
    }

    if (showDialog) {
        TextFieldDialog(
            title = stringResource(R.string.new_checklist),
            label = stringResource(R.string.label_name),
            onConfirm = { name -> vm.addChecklist(name); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }

    val deleteCandidate = deleteTarget
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_checklist)) },
            text = { Text(stringResource(R.string.delete_checklist_confirm, deleteCandidate.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteChecklist(deleteCandidate.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    val editCandidate = editChecklist
    if (editCandidate != null) {
        TextFieldDialog(
            title = stringResource(R.string.rename_checklist),
            label = stringResource(R.string.label_name),
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
    item: ChecklistSummary,
    onOpen: (Long) -> Unit,
    onDeleteRequest: () -> Unit,
    onEditRequest: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                item.checklist.name,
                modifier = Modifier.clickable { onEditRequest() }
            )
        },
        supportingContent = {
            Column {
                val createdText = DateFormat.getDateInstance().format(Date(item.checklist.createdAt))
                Text(stringResource(R.string.created_on, createdText))
                if (item.total > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.progress_count, item.done, item.total))
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.done.toFloat() / item.total },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onOpen(item.checklist.id) },
                onLongClick = { onDeleteRequest() }
            )
    )
    HorizontalDivider()
}
