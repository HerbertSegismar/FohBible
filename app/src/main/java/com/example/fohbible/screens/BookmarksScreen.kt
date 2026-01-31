package com.example.fohbible.screens

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.BookmarkRemove
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.fohbible.MainActivity
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.data.Verse
import com.example.fohbible.models.AppViewModel
import com.example.fohbible.utils.BibleVersionUtils
import com.example.fohbible.utils.SimpleVerseProcessor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BookmarksScreen(
    databaseHelper: DatabaseHelper? = null,
    onNavigateToReader: (PassageSelection) -> Unit
) {
    val context = LocalContext.current
    val appViewModel: AppViewModel = viewModel()
    var bookmarkedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var selectedDbName by remember { mutableStateOf(appViewModel.currentDbName) }
    var selectedVersionAbbr by remember { mutableStateOf(appViewModel.currentVersionAbbr) }

    // Interactive states
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedVerses = remember { mutableStateListOf<Verse>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_ADDED) }
    var showFilterOptions by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val dbHelper = remember(selectedDbName) { DatabaseHelper(context as MainActivity, selectedDbName) }

    LaunchedEffect(selectedDbName, multiSelectMode) {
        if (!multiSelectMode) {
            loadBookmarks(context, databaseHelper, selectedDbName) { verses ->
                bookmarkedVerses = sortVerses(verses, sortOrder)
            }
        }
    }

    LaunchedEffect(sortOrder) {
        if (!multiSelectMode) {
            bookmarkedVerses = sortVerses(bookmarkedVerses, sortOrder)
        }
    }

    // Filter verses based on search query
    val filteredVerses = remember(bookmarkedVerses, searchQuery) {
        if (searchQuery.isEmpty()) {
            bookmarkedVerses
        } else {
            bookmarkedVerses.filter { verse ->
                verse.text.contains(searchQuery, ignoreCase = true) ||
                        verse.bookName?.contains(searchQuery, ignoreCase = true) == true ||
                        verse.verseNumber.toString().contains(searchQuery) ||
                        verse.chapter?.toString()?.contains(searchQuery) == true
            }
        }
    }

    Scaffold(
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
                TopAppBar(
                    title = {
                        Text(
                            "Bookmarks (${bookmarkedVerses.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortOptions = true }) {
                                BadgedBox(
                                    badge = {
                                        if (sortOrder != SortOrder.DATE_ADDED) {
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
                        IconButton(onClick = { showFilterOptions = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { multiSelectMode = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedVerses.isNotEmpty() && multiSelectMode,
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
            // Search Bar
            AnimatedVisibility(
                visible = searchActive,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Search in bookmarks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search suggestions could be added here
                }
            }

            // Version Selector
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

            if (filteredVerses.isEmpty()) {
                EmptyBookmarksScreen(searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredVerses, key = { verse -> "${verse.bookName}-${verse.chapter}-${verse.verseNumber}" }) { verse ->
                        SwipeToDeleteBookmarkItem(
                            verse = verse,
                            isSelected = selectedVerses.contains(verse),
                            multiSelectMode = multiSelectMode,
                            onToggleSelect = {
                                if (multiSelectMode) {
                                    if (selectedVerses.contains(verse)) {
                                        selectedVerses.remove(verse)
                                    } else {
                                        selectedVerses.add(verse)
                                    }
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    removeBookmark(verse, dbHelper)
                                    bookmarkedVerses = bookmarkedVerses.filter { it != verse }
                                    snackbarHostState.showSnackbar(
                                        "Bookmark removed",
                                        actionLabel = "Undo",
                                        duration = androidx.compose.material3.SnackbarDuration.Short
                                    ).also { action ->
                                        if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            dbHelper.addBookmark(verse)
                                            bookmarkedVerses = bookmarkedVerses + verse
                                        }
                                    }
                                }
                            },
                            onNavigate = {
                                if (!multiSelectMode) {
                                    appViewModel.currentDbName = selectedDbName
                                    appViewModel.currentVersionAbbr = selectedVersionAbbr
                                    val bookNumber = BibleData.getBookByName(verse.bookName ?: "")?.customNumber ?: 1
                                    val passage = PassageSelection(
                                        bookNumber = bookNumber,
                                        bookName = verse.bookName ?: "Genesis",
                                        chapter = verse.chapter ?: 1,
                                        verse = verse.verseNumber
                                    )
                                    onNavigateToReader(passage)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Sort Options Dialog
        if (showSortOptions) {
            SortOptionsDialog(
                currentSortOrder = sortOrder,
                onSortOrderSelected = { sortOrder = it
                    showSortOptions = false },
                onDismiss = { showSortOptions = false }
            )
        }

        // Filter Options Dialog
        if (showFilterOptions) {
            FilterOptionsDialog(
                onDismiss = { showFilterOptions = false }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmation) {
            DeleteConfirmationDialog(
                count = selectedVerses.size,
                onConfirm = {
                    selectedVerses.forEach { verse ->
                        removeBookmark(verse, dbHelper)
                    }
                    bookmarkedVerses = bookmarkedVerses.filter { it !in selectedVerses }
                    selectedVerses.clear()
                    multiSelectMode = false
                    showDeleteConfirmation = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "${selectedVerses.size} bookmarks removed",
                            actionLabel = "Undo",
                            duration = androidx.compose.material3.SnackbarDuration.Short
                        ).also { action ->
                            if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                selectedVerses.forEach { verse ->
                                    dbHelper.addBookmark(verse)
                                }
                                bookmarkedVerses = bookmarkedVerses + selectedVerses
                            }
                        }
                    }
                },
                onDismiss = { showDeleteConfirmation = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteBookmarkItem(
    verse: Verse,
    isSelected: Boolean,
    multiSelectMode: Boolean,
    onToggleSelect: () -> Unit,
    onRemove: () -> Unit,
    onNavigate: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }

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
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale *= zoom
                    scale = scale.coerceIn(0.8f, 1.2f)
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
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
                        text = "${verse.bookName} ${verse.chapter}:${verse.verseNumber}",
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
                            onClick = { onRemove() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.BookmarkRemove,
                                contentDescription = "Remove Bookmark",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val annotatedText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                ) {
                    append("${verse.verseNumber} ")
                }
                append(SimpleVerseProcessor.stripXmlTags(verse.text))
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                textAlign = TextAlign.Justify,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            // Tags/Categories could be added here
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = verse.bookName?.take(3) ?: "GEN",
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
                        text = "Ch ${verse.chapter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
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
            .shadow(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
fun SortOptionsDialog(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
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
                    text = "Sort Bookmarks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SortOrder.entries.forEach { order ->
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
fun DeleteConfirmationDialog(
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
                    text = if (count == 1) "Delete Bookmark?" else "Delete $count Bookmarks?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (count == 1) "This bookmark will be permanently removed." else "These bookmarks will be permanently removed.",
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
fun EmptyBookmarksScreen(isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isSearching) "No matching bookmarks" else "No bookmarks yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearching) "Try a different search term" else "Bookmark verses to see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FilterOptionsDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Implement book filtering options here
                // This would require storing book information with bookmarks
            }
        }
    }
}

private fun shareVerses(context: Context, verses: List<Verse>) {
    val text = verses.joinToString("\n\n") { verse ->
        "${verse.bookName} ${verse.chapter}:${verse.verseNumber}\n${SimpleVerseProcessor.stripXmlTags(verse.text)}"
    }
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

private fun sortVerses(verses: List<Verse>, sortOrder: SortOrder): List<Verse> {
    return when (sortOrder) {
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
        SortOrder.DATE_ADDED -> verses // Assuming original order is by addition date
    }
}

enum class SortOrder(val displayName: String) {
    BOOK("By Book"),
    CHAPTER("By Chapter"),
    DATE_ADDED("Recently Added")
}

// Add these extension functions for better user experience
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// Helper functions remain the same
private fun loadBookmarks(
    context: Context,
    databaseHelper: DatabaseHelper?,
    currentDbName: String,
    onComplete: (List<Verse>) -> Unit
) {
    if (databaseHelper != null) {
        Thread {
            val verses = databaseHelper.getBookmarks()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    } else {
        Thread {
            val dbHelper = DatabaseHelper(
                context as MainActivity,
                databaseName = currentDbName
            )
            val verses = dbHelper.getBookmarks()
            dbHelper.close()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    }
}

private fun removeBookmark(verse: Verse, databaseHelper: DatabaseHelper) {
    Thread {
        databaseHelper.removeBookmark(verse)
    }.start()
}