package com.example.checklist.ui.screens

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
                    onDeleteRequest = { deleteTarget = item }
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

    val target = deleteTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete checklist") },
            text = { Text("Are you sure you want to delete \"${target.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteChecklist(target.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChecklistRow(
    item: ChecklistEntity,
    onOpen: (Long) -> Unit,
    onDeleteRequest: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.name) },
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