package com.fountofhopedotorg.fohbible.notes

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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