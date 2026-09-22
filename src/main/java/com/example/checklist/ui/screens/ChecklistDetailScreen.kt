package com.example.checklist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.checklist.R
import com.example.checklist.data.*
import com.example.checklist.ui.screens.components.EmptyState
import com.example.checklist.ui.screens.components.TextFieldDialog
import com.example.checklist.viewmodel.*
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// addForSectionId doubles as the ungrouped marker, because null already means "no dialog showing".
private const val UNGROUPED = -1L

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
    var deleteSection by remember { mutableStateOf<SectionEntity?>(null) }
    var editEntry by remember { mutableStateOf<EntryEntity?>(null) }
    var editChecklist by remember { mutableStateOf<ChecklistEntity?>(null) }

    var rows by remember { mutableStateOf<List<DetailRow>>(emptyList()) }
    LaunchedEffect(detail) { rows = detail?.toRows().orEmpty() }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        rows = moveRow(rows, from.index, to.index)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val onDeleteEntry: (EntryEntity) -> Unit = remember(vm) {
        { entry ->
            vm.deleteEntry(entry.id)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.entry_deleted, entry.text),
                    actionLabel = context.getString(R.string.action_undo)
                )
                if (result == SnackbarResult.ActionPerformed) vm.restoreEntry(entry)
            }
        }
    }
    val onToggleEntry = remember(vm) { { entry: EntryEntity -> vm.toggle(entry) } }
    val onEditEntry = remember { { entry: EntryEntity -> editEntry = entry } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
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
        val data = detail
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (data != null) {
                if (data.total > 0) {
                    ProgressHeader(done = data.done, total = data.total)
                }
                if (data.total == 0 && data.sections.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.empty_entries_title),
                            body = stringResource(R.string.empty_entries_body)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        items(rows, key = { it.key }) { row ->
                            when (row) {
                                is DetailRow.Header -> {
                                    val index = data.sections.indexOfFirst { it.section.id == row.section?.id }
                                    SectionHeaderRow(
                                        section = row.section,
                                        canMoveUp = index > 0,
                                        canMoveDown = index >= 0 && index < data.sections.lastIndex,
                                        onRename = { editSection = row.section },
                                        onDelete = { deleteSection = row.section },
                                        onMove = { by -> row.section?.let { vm.moveSection(data, it, by) } }
                                    )
                                }

                                is DetailRow.Entry -> ReorderableItem(reorderState, key = row.key) { _ ->
                                    EntryRow(
                                        entry = row.entry,
                                        onToggle = onToggleEntry,
                                        onEdit = onEditEntry,
                                        onDelete = onDeleteEntry,
                                        dragHandle = {
                                            IconButton(
                                                modifier = Modifier.draggableHandle(
                                                    onDragStopped = { vm.applyRowOrder(rows) }
                                                ),
                                                onClick = {}
                                            ) {
                                                Icon(
                                                    Icons.Default.Menu,
                                                    contentDescription = stringResource(
                                                        R.string.drag_handle,
                                                        row.entry.text
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }

                                is DetailRow.AddEntry -> TextButton(
                                    onClick = { addForSectionId = row.sectionId ?: UNGROUPED }
                                ) {
                                    Text(
                                        if (row.sectionTitle != null) {
                                            stringResource(R.string.add_entry_in_section, row.sectionTitle)
                                        } else {
                                            stringResource(R.string.add_entry)
                                        }
                                    )
                                }

                                DetailRow.BottomSpace -> Spacer(Modifier.height(88.dp))
                            }
                        }
                    }
                }
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

    val targetSectionId = addForSectionId
    val currentDetail = detail
    if (targetSectionId != null && currentDetail != null) {
        val realSectionId = targetSectionId.takeIf { it != UNGROUPED }
        val existing = if (realSectionId == null) {
            currentDetail.ungrouped
        } else {
            currentDetail.sections.firstOrNull { it.section.id == realSectionId }?.entries.orEmpty()
        }
        val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        TextFieldDialog(
            title = stringResource(R.string.new_entry),
            label = stringResource(R.string.label_text),
            onConfirm = { text ->
                vm.addEntry(id, realSectionId, text, nextOrder)
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

    val sectionToDelete = deleteSection
    if (sectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteSection = null },
            title = { Text(stringResource(R.string.delete_heading)) },
            text = { Text(stringResource(R.string.delete_heading_confirm, sectionToDelete.title)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSection(sectionToDelete.id)
                    deleteSection = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteSection = null }) { Text(stringResource(R.string.action_cancel)) }
            }
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
private fun ProgressHeader(done: Int, total: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.progress_count, done, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionHeaderRow(
    section: SectionEntity?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = section?.title ?: stringResource(R.string.ungrouped),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .then(if (section != null) Modifier.clickable { onRename() } else Modifier)
                .padding(vertical = 8.dp)
        )
        if (section != null) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.heading_options, section.title)
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = { menuOpen = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_up)) },
                        enabled = canMoveUp,
                        onClick = { menuOpen = false; onMove(-1) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_down)) },
                        enabled = canMoveDown,
                        onClick = { menuOpen = false; onMove(1) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_heading)) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(
    entry: EntryEntity,
    onToggle: (EntryEntity) -> Unit,
    onEdit: (EntryEntity) -> Unit,
    onDelete: (EntryEntity) -> Unit,
    dragHandle: @Composable () -> Unit
) {
    // Plain remember, not rememberSaveable: LazyColumn keeps saved state per item key, so an
    // undone entry came back already settled as dismissed and deleted itself on the next frame.
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val dismissState = remember {
        SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold)
    }
    var dismissed by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = {
            if (!dismissed) {
                dismissed = true
                onDelete(entry)
            }
        },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        ListItem(
            leadingContent = {
                Checkbox(checked = entry.checked, onCheckedChange = { onToggle(entry) })
            },
            headlineContent = {
                Text(entry.text, modifier = Modifier.clickable { onEdit(entry) })
            },
            trailingContent = dragHandle
        )
    }
    HorizontalDivider()
}
