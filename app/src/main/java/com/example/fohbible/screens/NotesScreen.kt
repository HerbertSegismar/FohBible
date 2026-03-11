package com.example.fohbible.screens

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.MainActivity
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.Note
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.data.Verse
import com.example.fohbible.modals.NotesModal
import com.example.fohbible.models.AppViewModel
import com.example.fohbible.utils.BibleVersionUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onNavigateToReader: (PassageSelection) -> Unit
) {
    val context = LocalContext.current
    val appViewModel: AppViewModel = viewModel()
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var selectedDbName by remember { mutableStateOf(appViewModel.currentDbName) }
    var selectedVersionAbbr by remember { mutableStateOf(appViewModel.currentVersionAbbr) }
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedNotes = remember { mutableStateListOf<Note>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(NoteSortOrder.DATE_NEWEST) }
    var showFilterOptions by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<Note?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val dbHelper = remember(selectedDbName) { DatabaseHelper(context as MainActivity, selectedDbName) }

    LaunchedEffect(selectedDbName, multiSelectMode) {
        if (!multiSelectMode) {
            loadNotes(dbHelper) { loadedNotes ->
                notes = sortNotes(loadedNotes, sortOrder)
            }
        }
    }

    LaunchedEffect(sortOrder) {
        if (!multiSelectMode) {
            notes = sortNotes(notes, sortOrder)
        }
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isEmpty()) {
            notes
        } else {
            notes.filter { note ->
                note.note.contains(searchQuery, ignoreCase = true) ||
                        note.bookName.contains(searchQuery, ignoreCase = true) ||
                        note.chapter.toString().contains(searchQuery) ||
                        note.startVerse.toString().contains(searchQuery) ||
                        note.endVerse.toString().contains(searchQuery)
            }
        }
    }

    Scaffold(
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
                    onFilter = { showFilterOptions = true },
                    onMore = { multiSelectMode = true }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedNotes.isNotEmpty() && multiSelectMode,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
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
                ) { }
            }

            if (!searchActive) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    BibleVersionSelector(
                        title = "Bible Version",
                        currentAbbr = selectedVersionAbbr,
                        description = BibleVersionUtils.descriptionMap[selectedDbName] ?: "Bible translation",
                        onVersionSelected = { file, abbr ->
                            selectedDbName = file
                            selectedVersionAbbr = abbr
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredNotes.isEmpty()) {
                EmptyNotesScreen(isSearching = searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { note -> "${note.bookName}-${note.chapter}-${note.startVerse}-${note.endVerse}" }) { note ->
                        NoteItem(
                            note = note,
                            isSelected = selectedNotes.contains(note),
                            multiSelectMode = multiSelectMode,
                            onToggleSelect = {
                                if (multiSelectMode) {
                                    if (selectedNotes.contains(note)) {
                                        selectedNotes.remove(note)
                                    } else {
                                        selectedNotes.add(note)
                                    }
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    removeNote(note, dbHelper)
                                    notes = notes.filter { it != note }
                                    snackbarHostState.showSnackbar(
                                        "Note removed",
                                        actionLabel = "Undo",
                                        duration = androidx.compose.material3.SnackbarDuration.Short
                                    ).also { action ->
                                        if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            dbHelper.addOrUpdateNote(note.bookName, note.chapter, note.startVerse, note.endVerse, note.note)
                                            notes = notes + note
                                        }
                                    }
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
                                    // Navigate to reader at the passage (first verse of the range)
                                    val bookNumber = BibleData.getBookByName(note.bookName)?.customNumber ?: 1
                                    val passage = PassageSelection(
                                        bookNumber = bookNumber,
                                        bookName = note.bookName,
                                        chapter = note.chapter,
                                        verse = note.startVerse
                                    )
                                    onNavigateToReader(passage)
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
                onSortOrderSelected = {
                    sortOrder = it
                    showSortOptions = false
                },
                onDismiss = { showSortOptions = false }
            )
        }

        if (showFilterOptions) {
            NoteFilterOptionsDialog(
                onDismiss = { showFilterOptions = false }
            )
        }

        if (showDeleteConfirmation) {
            NoteDeleteConfirmationDialog(
                count = selectedNotes.size,
                onConfirm = {
                    selectedNotes.forEach { note ->
                        removeNote(note, dbHelper)
                    }
                    notes = notes.filter { it !in selectedNotes }
                    selectedNotes.clear()
                    multiSelectMode = false
                    showDeleteConfirmation = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "${selectedNotes.size} notes removed",
                            actionLabel = "Undo",
                            duration = androidx.compose.material3.SnackbarDuration.Short
                        ).also { action ->
                            if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                selectedNotes.forEach { note ->
                                    dbHelper.addOrUpdateNote(note.bookName, note.chapter, note.startVerse, note.endVerse, note.note)
                                }
                                notes = notes + selectedNotes
                            }
                        }
                    }
                },
                onDismiss = { showDeleteConfirmation = false }
            )
        }
        selectedNoteForEdit?.let { note ->
            NotesModal(
                show = showNotesModal,
                onDismiss = {
                    showNotesModal = false
                    selectedNoteForEdit = null
                },
                verses = listOf(
                    Verse(
                        verseNumber = note.startVerse,
                        text = "",
                        bookName = note.bookName,
                        chapter = note.chapter
                    )
                ),
                passage = PassageSelection(
                    bookNumber = BibleData.getBookByName(note.bookName)?.customNumber ?: 1,
                    bookName = note.bookName,
                    chapter = note.chapter,
                    verse = note.startVerse
                ),
                databaseHelper = dbHelper,
                onSave = {
                    // Refresh notes
                    loadNotes(dbHelper) { loadedNotes ->
                        notes = sortNotes(loadedNotes, sortOrder)
                    }
                }
            )
        }
    }
}

@Composable
fun NormalTopBar(
    title: String,
    count: Int,
    onSearch: () -> Unit,
    onSort: () -> Unit,
    showSortBadge: Boolean,
    onFilter: () -> Unit,
    onMore: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp)
            .height(64.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$title ($count)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearch) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(onClick = onSort) {
                        BadgedBox(
                            badge = {
                                if (showSortBadge) {
                                    Badge(
                                        modifier = Modifier.size(8.dp),
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(onClick = onFilter) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onMore) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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
                onClick = {
                    if (multiSelectMode) {
                        onToggleSelect()
                    } else {
                        onNavigate()
                    }
                },
                onLongClick = {
                    if (!multiSelectMode) {
                        onToggleSelect()
                    }
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = multiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
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
                        IconButton(
                            onClick = { onEdit() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Note,
                                contentDescription = "Edit Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).rotate(90f)
                            )
                        }
                        IconButton(
                            onClick = { onRemove() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.note,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = note.bookName.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Ch ${note.chapter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                Text(
                    text = "$formattedDate at $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp)
            .height(64.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row {
                IconButton(
                    onClick = onShare,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteSortOptionsDialog(
    currentSortOrder: NoteSortOrder,
    onSortOrderSelected: (NoteSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Sort Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                NoteSortOrder.entries.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderSelected(order) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOrder == order,
                            onClick = { onSortOrderSelected(order) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = order.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun NoteDeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (count == 1) "Delete Note?" else "Delete $count Notes?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (count == 1) "This note will be permanently removed." else "These notes will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesScreen(isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Note,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(120.dp).rotate(90f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isSearching) "No matching notes" else "No notes yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearching) "Try a different search term" else "Add notes to verses to see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NoteFilterOptionsDialog(
    onDismiss: () -> Unit
) {
    // Placeholder for future filtering options (e.g., by book)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Filter options coming soon")
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

private fun shareNotes(context: Context, notes: List<Note>) {
    val text = notes.joinToString("\n\n") { note ->
        val range = if (note.startVerse == note.endVerse) "${note.startVerse}" else "${note.startVerse}-${note.endVerse}"
        "${note.bookName} ${note.chapter}:$range\n${note.note}"
    }
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

private fun loadNotes(dbHelper: DatabaseHelper, onComplete: (List<Note>) -> Unit) {
    Thread {
        val notes = dbHelper.getAllNotes()
        Handler(Looper.getMainLooper()).post {
            onComplete(notes)
        }
    }.start()
}

private fun removeNote(note: Note, dbHelper: DatabaseHelper) {
    Thread {
        dbHelper.deleteNote(note.bookName, note.chapter, note.startVerse, note.endVerse)
    }.start()
}

private fun sortNotes(notes: List<Note>, sortOrder: NoteSortOrder): List<Note> {
    return when (sortOrder) {
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
}

enum class NoteSortOrder(val displayName: String) {
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First"),
    BOOK("By Book"),
    CHAPTER("By Chapter")
}

