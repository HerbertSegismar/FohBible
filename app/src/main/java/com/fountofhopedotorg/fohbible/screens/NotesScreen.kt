package com.fountofhopedotorg.fohbible.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.data.*
import com.fountofhopedotorg.fohbible.modals.NotesModal
import com.fountofhopedotorg.fohbible.modals.VersionSelectionModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class VersionModalColors(
    val primary: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val border: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onNavigateToReader: (PassageSelection) -> Unit) {
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var selectedDbName by remember { mutableStateOf(viewModel.currentDbName) }
    var selectedVersionAbbr by remember { mutableStateOf(viewModel.currentVersionAbbr) }
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedNotes = remember { mutableStateListOf<Note>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(NoteSortOrder.DATE_NEWEST) }
    var showNotesModal by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<Note?>(null) }
    var showVersionModal by remember { mutableStateOf(false) }
    var editVerses by remember { mutableStateOf<List<Verse>?>(null) }

    val dbHelper = remember(selectedDbName) { DatabaseHelper(context as MainActivity, selectedDbName) }

    LaunchedEffect(selectedDbName, multiSelectMode) {
        if (!multiSelectMode) {
            notes = sortNotes(loadNotes(dbHelper), sortOrder)
        }
    }

    LaunchedEffect(sortOrder) {
        if (!multiSelectMode) {
            notes = sortNotes(notes, sortOrder)
        }
    }

    LaunchedEffect(selectedNoteForEdit) {
        editVerses = if (selectedNoteForEdit != null) {
            val note = selectedNoteForEdit!!
            val bookNumber = BibleData.getBookByName(note.bookName)?.customNumber ?: return@LaunchedEffect
            withContext(Dispatchers.IO) {
                dbHelper.getVerses(bookNumber, note.chapter)
                    .filter { it.verseNumber in note.startVerse..note.endVerse }
            }
        } else null
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isEmpty()) notes
        else notes.filter { note ->
            note.note.contains(searchQuery, ignoreCase = true) ||
                    note.bookName.contains(searchQuery, ignoreCase = true) ||
                    note.chapter.toString().contains(searchQuery) ||
                    note.startVerse.toString().contains(searchQuery) ||
                    note.endVerse.toString().contains(searchQuery)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (multiSelectMode) {
                MultiSelectTopBar(
                    selectedCount = selectedNotes.size,
                    onCancel = {
                        multiSelectMode = false
                        selectedNotes.clear()
                    },
                    onDelete = { showDeleteConfirmation = true },
                    onShare = { shareNotes(context, selectedNotes) }
                )
            } else {
                NormalTopBar(
                    title = "Notes",
                    count = notes.size,
                    onSearch = { searchActive = true },
                    onSort = { showSortOptions = true },
                    showSortBadge = sortOrder != NoteSortOrder.DATE_NEWEST,
                    onMore = { multiSelectMode = true }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedNotes.isNotEmpty() && multiSelectMode,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                FloatingActionButton(
                    onClick = { showDeleteConfirmation = true },
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, "Delete Selected")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = searchActive,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { searchActive = false },
                            expanded = searchActive,
                            onExpandedChange = { searchActive = it },
                            placeholder = { Text("Search in notes...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, "Clear")
                                    }
                                }
                            }
                        )
                    },
                    expanded = searchActive,
                    onExpandedChange = { searchActive = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {}
            }

            if (!searchActive) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVersionModal = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Bible Version",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$selectedVersionAbbr - ${BibleVersionUtils.descriptionMap[selectedDbName] ?: "Bible translation"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.showVersionInfoDialog = true
                                    viewModel.versionInfoForDialog = viewModel.currentDbName
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filteredNotes.isEmpty()) {
                EmptyNotesScreen(isSearching = searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { "${it.bookName}-${it.chapter}-${it.startVerse}-${it.endVerse}" }) { note ->
                        NoteItem(
                            note = note,
                            isSelected = selectedNotes.contains(note),
                            multiSelectMode = multiSelectMode,
                            onToggleSelect = {
                                if (multiSelectMode) {
                                    if (selectedNotes.contains(note)) selectedNotes.remove(note)
                                    else selectedNotes.add(note)
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    deleteNotesWithUndo(
                                        notesToDelete = listOf(note),
                                        dbHelper = dbHelper,
                                        snackbarHostState = snackbarHostState,
                                        onNotesDeleted = { deleted ->
                                            notes = notes.filter { it !in deleted }
                                        },
                                        onRestore = { restored ->
                                            notes = sortNotes(notes + restored, sortOrder)
                                        }
                                    )
                                }
                            },
                            onEdit = {
                                if (!multiSelectMode) {
                                    selectedNoteForEdit = note
                                    showNotesModal = true
                                }
                            },
                            onNavigate = {
                                if (!multiSelectMode) {
                                    val bookNumber = BibleData.getBookByName(note.bookName)?.customNumber ?: 1
                                    onNavigateToReader(
                                        PassageSelection(
                                            bookNumber = bookNumber,
                                            bookName = note.bookName,
                                            chapter = note.chapter,
                                            verse = note.startVerse
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showSortOptions) {
            NoteSortOptionsDialog(
                currentSortOrder = sortOrder,
                onSortOrderSelected = { sortOrder = it; showSortOptions = false },
                onDismiss = { showSortOptions = false }
            )
        }

        if (showDeleteConfirmation) {
            val notesToDelete = selectedNotes.toList()
            NoteDeleteConfirmationDialog(
                count = notesToDelete.size,
                onConfirm = {
                    scope.launch {
                        deleteNotesWithUndo(
                            notesToDelete = notesToDelete,
                            dbHelper = dbHelper,
                            snackbarHostState = snackbarHostState,
                            onNotesDeleted = { deleted ->
                                notes = notes.filter { it !in deleted }
                                selectedNotes.clear()
                                multiSelectMode = false
                            },
                            onRestore = { restored ->
                                notes = sortNotes(notes + restored, sortOrder)
                                selectedNotes.clear()
                                multiSelectMode = false
                            }
                        )
                    }
                    showDeleteConfirmation = false
                },
                onDismiss = { showDeleteConfirmation = false }
            )
        }

        if (showNotesModal && selectedNoteForEdit != null && editVerses != null) {
            NotesModal(
                show = true,
                onDismiss = {
                    showNotesModal = false
                    selectedNoteForEdit = null
                    editVerses = null
                },
                verses = editVerses!!,
                passage = PassageSelection(
                    bookNumber = BibleData.getBookByName(selectedNoteForEdit!!.bookName)?.customNumber ?: 1,
                    bookName = selectedNoteForEdit!!.bookName,
                    chapter = selectedNoteForEdit!!.chapter,
                    verse = selectedNoteForEdit!!.startVerse
                ),
                databaseHelper = dbHelper,
                onSave = {
                    scope.launch {
                        notes = sortNotes(loadNotes(dbHelper), sortOrder)
                    }
                },
                appViewModel = viewModel
            )
        }

        if (showVersionModal) {
            val modalColors = VersionModalColors(
                primary = MaterialTheme.colorScheme.primary,
                card = if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor,
                text = MaterialTheme.colorScheme.onSurface,
                muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                border = MaterialTheme.colorScheme.surfaceVariant
            )
            VersionSelectionModal(
                currentVersionKey = viewModel.currentDbName,
                isSecondary = false,
                onVersionSelected = { file ->
                    selectedDbName = file
                    selectedVersionAbbr = BibleVersionUtils.versionMap[file] ?: "Bible"
                    viewModel.currentDbName = file
                    viewModel.currentVersionAbbr = selectedVersionAbbr
                    showVersionModal = false
                },
                onDismiss = { showVersionModal = false },
                colors = mapOf(
                    "primary" to modalColors.primary,
                    "card" to modalColors.card,
                    "text" to modalColors.text,
                    "muted" to modalColors.muted,
                    "border" to modalColors.border
                )
            )
        }
    }
}

private suspend fun deleteNotesWithUndo(
    notesToDelete: List<Note>,
    dbHelper: DatabaseHelper,
    snackbarHostState: SnackbarHostState,
    onNotesDeleted: (List<Note>) -> Unit,
    onRestore: (List<Note>) -> Unit
) {
    withContext(Dispatchers.IO) {
        notesToDelete.forEach { note ->
            dbHelper.deleteNote(note.bookName, note.chapter, note.startVerse, note.endVerse)
        }
    }
    onNotesDeleted(notesToDelete)

    val result = snackbarHostState.showSnackbar(
        message = "${notesToDelete.size} note${if (notesToDelete.size != 1) "s" else ""} removed",
        actionLabel = "Undo",
        duration = SnackbarDuration.Short
    )
    if (result == SnackbarResult.ActionPerformed) {
        withContext(Dispatchers.IO) {
            notesToDelete.forEach { note ->
                dbHelper.addOrUpdateNote(note.bookName, note.chapter, note.startVerse, note.endVerse, note.note)
            }
        }
        onRestore(notesToDelete)
    }
}
private suspend fun loadNotes(dbHelper: DatabaseHelper): List<Note> =
    withContext(Dispatchers.IO) {
        dbHelper.getAllNotes()
    }

private fun shareNotes(context: Context, notes: List<Note>) {
    val text = notes.joinToString("\n\n") { note ->
        val range = if (note.startVerse == note.endVerse) "${note.startVerse}" else "${note.startVerse}-${note.endVerse}"
        "${note.bookName} ${note.chapter}:$range\n${note.note}"
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}

private fun sortNotes(notes: List<Note>, sortOrder: NoteSortOrder): List<Note> = when (sortOrder) {
    NoteSortOrder.DATE_NEWEST -> notes.sortedByDescending { it.timestamp }
    NoteSortOrder.DATE_OLDEST -> notes.sortedBy { it.timestamp }
    NoteSortOrder.BOOK -> notes.sortedWith(
        compareBy<Note> { BibleData.getBookByName(it.bookName)?.customNumber ?: 0 }
            .thenBy { it.chapter }
            .thenBy { it.startVerse }
    )
    NoteSortOrder.CHAPTER -> notes.sortedWith(
        compareBy<Note> { it.chapter }
            .thenBy { BibleData.getBookByName(it.bookName)?.customNumber ?: 0 }
            .thenBy { it.startVerse }
    )
}

enum class NoteSortOrder(val displayName: String) {
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First"),
    BOOK("By Book"),
    CHAPTER("By Chapter")
}

@Composable
fun NormalTopBar(
    title: String,
    count: Int,
    onSearch: () -> Unit,
    onSort: () -> Unit,
    showSortBadge: Boolean,
    onMore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp).height(64.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$title ($count)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }
                Box {
                    IconButton(onClick = onSort) {
                        BadgedBox(badge = { if (showSortBadge) Badge(modifier = Modifier.size(8.dp), containerColor = MaterialTheme.colorScheme.primary) }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
fun NoteItem(
    note: Note,
    isSelected: Boolean,
    multiSelectMode: Boolean,
    onToggleSelect: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    onNavigate: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(note.timestamp * 1000))
    val formattedTime = timeFormat.format(Date(note.timestamp * 1000))
    val rangeString = if (note.startVerse == note.endVerse) "${note.startVerse}" else "${note.startVerse}-${note.endVerse}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (multiSelectMode) onToggleSelect() else onNavigate() },
                onLongClick = { if (!multiSelectMode) onToggleSelect() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = multiSelectMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() }, modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(
                        text = "${note.bookName} ${note.chapter}:$rangeString",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!multiSelectMode) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Note, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).rotate(90f))
                        }
                        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(note.note, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.clip(CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(note.bookName.take(3).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.clip(CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Ch ${note.chapter}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Text("$formattedDate at $formattedTime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
fun MultiSelectTopBar(selectedCount: Int, onCancel: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().shadow(4.dp).height(64.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Cancel", color = MaterialTheme.colorScheme.onPrimaryContainer) }
            Text("$selectedCount selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row {
                IconButton(onClick = onShare, enabled = selectedCount > 0) {
                    Icon(Icons.Default.Share, null, tint = if (selectedCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                }
                IconButton(onClick = onDelete, enabled = selectedCount > 0) {
                    Icon(Icons.Default.Delete, null, tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.38f))
                }
            }
        }
    }
}

@Composable
fun NoteSortOptionsDialog(currentSortOrder: NoteSortOrder, onSortOrderSelected: (NoteSortOrder) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sort Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                NoteSortOrder.entries.forEach { order ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSortOrderSelected(order) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentSortOrder == order, onClick = { onSortOrderSelected(order) })
                        Spacer(Modifier.width(12.dp))
                        Text(order.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

@Composable
fun NoteDeleteConfirmationDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(if (count == 1) "Delete Note?" else "Delete $count Notes?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(if (count == 1) "This note will be permanently removed." else "These notes will be permanently removed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Spacer(Modifier.width(16.dp))
                    TextButton(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesScreen(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.AutoMirrored.Filled.Note, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.size(90.dp).rotate(90f))
            Spacer(Modifier.height(12.dp))
            Text(if (isSearching) "No matching notes" else "No notes yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(if (isSearching) "Try a different search term" else "Add notes to verses to see them here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        }
    }
}