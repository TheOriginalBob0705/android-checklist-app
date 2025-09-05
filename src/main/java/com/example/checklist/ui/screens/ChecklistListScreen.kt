package com.example.checklist.ui.screens

import androidx.compose.foundation.clickable
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Checklists") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(lists, key = { it.id }) { item -> ChecklistRow(item, onOpen) }
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
}

@Composable
private fun ChecklistRow(item: ChecklistEntity, onOpen: (Long) -> Unit) {
    val createdText = remember(item.createdAt) {
        DateFormat.getDateInstance().format(Date(item.createdAt))
    }
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = { Text("Created • $createdText") },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(item.id) }
    )
    HorizontalDivider()
}