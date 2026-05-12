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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.modals.VersionSelectionModal
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateToReader: (PassageSelection) -> Unit
) {
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    var bookmarkedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var selectedDbName by remember { mutableStateOf(viewModel.currentDbName) }
    var selectedVersionAbbr by remember { mutableStateOf(viewModel.currentVersionAbbr) }
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedVerses = remember { mutableStateListOf<Verse>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_ADDED) }
    var showVersionModal by remember { mutableStateOf(false) }

    val dbHelper = remember(selectedDbName) { DatabaseHelper(context as MainActivity, selectedDbName) }

    LaunchedEffect(selectedDbName, multiSelectMode) {
        if (!multiSelectMode) {
            bookmarkedVerses = sortVerses(loadBookmarks(dbHelper), sortOrder)
        }
    }
    LaunchedEffect(sortOrder) {
        if (!multiSelectMode) {
            bookmarkedVerses = sortVerses(bookmarkedVerses, sortOrder)
        }
    }

    val filteredVerses = remember(bookmarkedVerses, searchQuery) {
        if (searchQuery.isEmpty()) bookmarkedVerses
        else bookmarkedVerses.filter { verse ->
            verse.text.contains(searchQuery, ignoreCase = true) ||
                    verse.bookName?.contains(searchQuery, ignoreCase = true) == true ||
                    verse.verseNumber.toString().contains(searchQuery) ||
                    verse.chapter?.toString()?.contains(searchQuery) == true
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (multiSelectMode) {
                MultiSelectTopBar(
                    selectedCount = selectedVerses.size,
                    onCancel = {
                        multiSelectMode = false
                        selectedVerses.clear()
                    },
                    onDelete = { showDeleteConfirmation = true },
                    onShare = { shareVerses(context, selectedVerses) }
                )
            } else {
                NormalTopBar(
                    bookmarksCount = bookmarkedVerses.size,
                    onSearch = { searchActive = true },
                    onSort = { showSortOptions = true },
                    showSortBadge = sortOrder != SortOrder.DATE_ADDED,
                    onMore = { multiSelectMode = true }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedVerses.isNotEmpty() && multiSelectMode,
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                            placeholder = { Text("Search in bookmarks...") },
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {}
            }

            if (!searchActive) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showVersionModal = true },
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
                                    "Bible Version",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "$selectedVersionAbbr - ${BibleVersionUtils.descriptionMap[selectedDbName] ?: "Bible translation"}",
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
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filteredVerses.isEmpty()) {
                EmptyBookmarksScreen(searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredVerses, key = { "${it.bookName}-${it.chapter}-${it.verseNumber}" }) { verse ->
                        BookmarkItem(
                            verse = verse,
                            isSelected = selectedVerses.contains(verse),
                            multiSelectMode = multiSelectMode,
                            onToggleSelect = {
                                if (multiSelectMode) {
                                    if (selectedVerses.contains(verse)) selectedVerses.remove(verse)
                                    else selectedVerses.add(verse)
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    deleteBookmarksWithUndo(
                                        bookmarksToDelete = listOf(verse),
                                        dbHelper = dbHelper,
                                        snackbarHostState = snackbarHostState,
                                        onDeleted = { deleted ->
                                            bookmarkedVerses = bookmarkedVerses.filter { it !in deleted }
                                        },
                                        onRestore = { restored ->
                                            bookmarkedVerses = sortVerses(bookmarkedVerses + restored, sortOrder)
                                        }
                                    )
                                }
                            },
                            onNavigate = {
                                if (!multiSelectMode) {
                                    viewModel.currentDbName = selectedDbName
                                    viewModel.currentVersionAbbr = selectedVersionAbbr
                                    val bookNumber = BibleData.getBookByName(verse.bookName ?: "")?.customNumber ?: 1
                                    onNavigateToReader(
                                        PassageSelection(
                                            bookNumber = bookNumber,
                                            bookName = verse.bookName ?: "Genesis",
                                            chapter = verse.chapter ?: 1,
                                            verse = verse.verseNumber
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
            SortOptionsDialog(
                currentSortOrder = sortOrder,
                onSortOrderSelected = { sortOrder = it; showSortOptions = false },
                onDismiss = { showSortOptions = false }
            )
        }
        if (showDeleteConfirmation) {
            val bookmarksToDelete = selectedVerses.toList()
            DeleteConfirmationDialog(
                count = bookmarksToDelete.size,
                onConfirm = {
                    scope.launch {
                        deleteBookmarksWithUndo(
                            bookmarksToDelete = bookmarksToDelete,
                            dbHelper = dbHelper,
                            snackbarHostState = snackbarHostState,
                            onDeleted = { deleted ->
                                bookmarkedVerses = bookmarkedVerses.filter { it !in deleted }
                                selectedVerses.clear()
                                multiSelectMode = false
                            },
                            onRestore = { restored ->
                                bookmarkedVerses = sortVerses(bookmarkedVerses + restored, sortOrder)
                                selectedVerses.clear()
                                multiSelectMode = false
                            }
                        )
                    }
                    showDeleteConfirmation = false
                },
                onDismiss = { showDeleteConfirmation = false }
            )
        }
        if (showVersionModal) {
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
                    "primary" to MaterialTheme.colorScheme.primary,
                    "card" to if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor,
                    "text" to MaterialTheme.colorScheme.onSurface,
                    "muted" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    "border" to MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
private suspend fun deleteBookmarksWithUndo(
    bookmarksToDelete: List<Verse>,
    dbHelper: DatabaseHelper,
    snackbarHostState: SnackbarHostState,
    onDeleted: (List<Verse>) -> Unit,
    onRestore: (List<Verse>) -> Unit
) {
    withContext(Dispatchers.IO) {
        bookmarksToDelete.forEach { dbHelper.removeBookmark(it) }
    }
    onDeleted(bookmarksToDelete)

    val result = snackbarHostState.showSnackbar(
        message = "${bookmarksToDelete.size} bookmark${if (bookmarksToDelete.size != 1) "s" else ""} removed",
        actionLabel = "Undo",
        duration = SnackbarDuration.Short
    )
    if (result == SnackbarResult.ActionPerformed) {
        withContext(Dispatchers.IO) {
            bookmarksToDelete.forEach { dbHelper.addBookmark(it) }
        }
        onRestore(bookmarksToDelete)
    }
}
private suspend fun loadBookmarks(dbHelper: DatabaseHelper): List<Verse> =
    withContext(Dispatchers.IO) { dbHelper.getBookmarks() }

private fun shareVerses(context: Context, verses: List<Verse>) {
    val text = verses.joinToString("\n\n") { verse ->
        "${verse.bookName} ${verse.chapter}:${verse.verseNumber}\n${SimpleVerseProcessor.stripXmlTags(verse.text)}"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun sortVerses(verses: List<Verse>, sortOrder: SortOrder): List<Verse> = when (sortOrder) {
    SortOrder.BOOK -> verses.sortedWith(
        compareBy<Verse> { BibleData.getBookByName(it.bookName ?: "")?.customNumber ?: 0 }
            .thenBy { it.chapter ?: 0 }
            .thenBy { it.verseNumber }
    )
    SortOrder.CHAPTER -> verses.sortedWith(
        compareBy<Verse> { it.chapter ?: 0 }
            .thenBy { BibleData.getBookByName(it.bookName ?: "")?.customNumber ?: 0 }
            .thenBy { it.verseNumber }
    )
    SortOrder.DATE_ADDED -> verses
}

enum class SortOrder(val displayName: String) {
    BOOK("By Book"),
    CHAPTER("By Chapter"),
    DATE_ADDED("Recently Added")
}

@Composable
fun NormalTopBar(
    bookmarksCount: Int,
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
            Text("Bookmarks ($bookmarksCount)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
fun BookmarkItem(
    verse: Verse,
    isSelected: Boolean,
    multiSelectMode: Boolean,
    onToggleSelect: () -> Unit,
    onRemove: () -> Unit,
    onNavigate: () -> Unit
) {
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
                        text = "${verse.bookName} ${verse.chapter}:${verse.verseNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!multiSelectMode) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                    append("${verse.verseNumber} ")
                }
                append(SimpleVerseProcessor.stripXmlTags(verse.text))
            }
            Text(annotatedText, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp, textAlign = TextAlign.Justify, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.clip(CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(verse.bookName?.take(3) ?: "GEN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.clip(CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Ch ${verse.chapter}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}

@Composable
fun SortOptionsDialog(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sort Bookmarks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                SortOrder.entries.forEach { order ->
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
fun DeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(if (count == 1) "Delete Bookmark?" else "Delete $count Bookmarks?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(if (count == 1) "This bookmark will be permanently removed." else "These bookmarks will be permanently removed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
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
fun EmptyBookmarksScreen(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.size(90.dp))
            Spacer(Modifier.height(12.dp))
            Text(if (isSearching) "No matching bookmarks" else "No bookmarks yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(if (isSearching) "Try a different search term" else "Bookmark verses to see them here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        }
    }
}