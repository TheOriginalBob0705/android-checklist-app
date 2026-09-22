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
import androidx.compose.ui.res.stringResource
import com.example.checklist.R
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.checklists_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text(stringResource(R.string.action_add)) }
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
            Text(stringResource(R.string.created_on, createdText)) },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onOpen(item.id) },
                onLongClick = { onDeleteRequest() }
            )
    )
    HorizontalDivider()
}