package com.fountofhopedotorg.fohbible.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.SCOPE_RANGES
import com.fountofhopedotorg.fohbible.data.SearchColors
import com.fountofhopedotorg.fohbible.data.SearchOptions
import com.fountofhopedotorg.fohbible.data.SearchVerse
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.getBookInfo
import com.fountofhopedotorg.fohbible.data.getBookNumberFromScope
import com.fountofhopedotorg.fohbible.data.getScopeConfig
import com.fountofhopedotorg.fohbible.data.isBookScope
import com.fountofhopedotorg.fohbible.data.scopeColors
import com.fountofhopedotorg.fohbible.modals.VersionSelectionModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.text.iterator

val POPULAR_SEARCH_TERMS = listOf(
    "faith", "love", "hope", "grace", "peace", "joy", "forgiveness", "salvation", "redemption", "righteousness",
    "sanctification", "holiness", "sin", "creation", "God", "Jesus", "Christ", "Savior", "Holy Spirit", "sacrifice"
)

fun getScopeForBookNumber(bookNumber: Int): String? {
    for ((scope, range) in SCOPE_RANGES) {
        if (range != null && bookNumber >= range.start && bookNumber <= range.end &&
            scope != "whole" && scope != "old-testament" && scope != "new-testament"
        ) {
            return scope
        }
    }
    return null
}

fun generateColorFromString(str: String): String {
    var hash = 0
    for (char in str) {
        hash = char.code + ((hash shl 5) - hash)
    }
    val colors = listOf(
        "#3B82F6", "#EF4444", "#10B981", "#F59E0B", "#8B5CF6",
        "#EC4899", "#06B6D4", "#84CC16", "#F97316", "#6366F1"
    )
    return colors[abs(hash) % colors.size]
}

fun getBookColor(bookName: String, verse: SearchVerse? = null): String {
    verse?.bookColor?.let { return it }
    val bookNumber = verse?.bookNumber ?: return generateColorFromString(bookName)
    val scope = getScopeForBookNumber(bookNumber)
    return scope?.let { scopeColors[it] }
        ?: if (getBookInfo(bookNumber)?.testament == Testament.OLD) "#DC2626" else "#059669"
}

suspend fun enhanceSearchResultsWithColors(
    results: List<SearchVerse>,
    dbHelper: DatabaseHelper?
): List<SearchVerse> {
    if (results.isEmpty() || dbHelper == null) return results
    val uniqueBookNumbers = results.map { it.bookNumber }.toSet()
    val colorMap = mutableMapOf<Int, String>()
    withContext(Dispatchers.IO) {
        uniqueBookNumbers.forEach { bookNumber ->
            try {
                val book = getBookInfo(bookNumber)
                val bookName = book?.name ?: bookNumber.toString()
                val color = getBookColor(bookName)
                colorMap[bookNumber] = color
            } catch (_: Exception) {
                colorMap[bookNumber] = generateColorFromString(bookNumber.toString())
            }
        }
    }
    return results.map { result ->
        result.copy(bookColor = colorMap[result.bookNumber])
    }
}

@Composable
fun SearchScreen(
    databaseHelper: DatabaseHelper?,
    onPassageSelected: (PassageSelection) -> Unit,
    currentVersionKey: String,
    onVersionChange: (String) -> Unit
) {
    val viewModel: AppViewModel = viewModel()
    val theme = LocalAppTheme.current
    val isDark = theme.darkTheme
    val primaryColor = theme.primaryColor

    val colors = SearchColors(
        primary = primaryColor,
        background = Color.Transparent,
        text = if (isDark) Color.White else Color.Black,
        muted = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
        card = if (isDark) Color(0xFF1E293B) else Color.White,
        border = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
    )

    var hasSearched by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<SearchVerse>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scope by remember { mutableStateOf("whole") }
    var showScopeDropdown by remember { mutableStateOf(false) }
    var showResultsStats by remember { mutableStateOf(false) }
    var inverseSearch by remember { mutableStateOf(false) }
    var exactPhrase by remember { mutableStateOf(false) }
    var showVersionDropdown by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showBackToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemScrollOffset > 300 || listState.firstVisibleItemIndex > 0
        }
    }

    val currentVersionDisplay = remember(currentVersionKey) {
        BibleVersionUtils.versionMap[currentVersionKey] ?: "Bible"
    }
    val scopeConfig = getScopeConfig(scope)
    val verseProcessor = remember { VerseTextProcessor() }
    val themeColors = ThemeColors(
        textColor = colors.text,
        verseNumber = colors.primary,
        primary = colors.primary,
        tagColor = colors.muted,
        tagBg = colors.card,
        wordsOfJesus = viewModel.wordsOfJesus,
        searchHighlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        highlightIcon = colors.primary
    )
    val handleVersionChange: (String) -> Unit = { newVersionKey ->
        onVersionChange(newVersionKey)
        showVersionDropdown = false
        results.clear()
        hasSearched = false
        showResultsStats = false
        query = ""
        error = null
    }
    val handleSearch: suspend (String?) -> Unit = handleSearch@{ searchQuery ->
        val actualQuery = searchQuery ?: query
        hasSearched = true
        showResultsStats = false
        if (actualQuery.trim().isEmpty()) {
            results.clear()
            return@handleSearch
        }
        try {
            loading = true
            error = null
            val searchOptions = SearchOptions(
                bookRange = if (isBookScope(scope)) {
                    getBookNumberFromScope(scope)?.let { Pair(it, it) }
                } else {
                    SCOPE_RANGES[scope]?.let { Pair(it.start, it.end) }
                }
            )
            val searchResults = withContext(Dispatchers.IO) {
                databaseHelper?.searchVerses(actualQuery, searchOptions, inverseSearch, exactPhrase)
                    ?: emptyList()
            }
            val enhancedResults = enhanceSearchResultsWithColors(searchResults, databaseHelper)
            results.clear()
            results.addAll(enhancedResults)
            showResultsStats = true
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        } catch (_: Exception) {
            error = "Failed to search. Please try again."
        } finally {
            loading = false
        }
    }

    val clearSearch: () -> Unit = {
        query = ""
        results.clear()
        hasSearched = false
        showResultsStats = false
        inverseSearch = false
        exactPhrase = false
    }

    val getResultStats: () -> String = {
        if (!hasSearched || loading || !showResultsStats) {
            val mode = when {
                exactPhrase && inverseSearch -> " (exact phrase & opposite)"
                exactPhrase -> " (exact phrase)"
                inverseSearch -> " (opposite)"
                else -> ""
            }
            "Search ${scopeConfig.label}$mode"
        } else if (results.isEmpty()) {
            val desc = when {
                inverseSearch && exactPhrase -> "without exact term \"$query\""
                inverseSearch -> "without \"$query\""
                exactPhrase -> "with exact term \"$query\""
                else -> "for \"$query\""
            }
            "No results found $desc in ${scopeConfig.label}"
        } else {
            val bookCount = results.map { it.bookNumber }.toSet().size
            val foundStr = "Found ${results.size} result${if (results.size != 1) "s" else ""}"
            val termStr = when {
                inverseSearch && exactPhrase -> " without exact term \"$query\""
                inverseSearch -> " without \"$query\""
                exactPhrase -> " with exact term \"$query\""
                else -> " for \"$query\""
            }
            val scopeStr = if (isBookScope(scope)) {
                " in ${scopeConfig.label}"
            } else {
                " in $bookCount book${if (bookCount != 1) "s" else ""}"
            }
            val queryStr = if (inverseSearch || exactPhrase || !isBookScope(scope)) termStr else ""
            "$foundStr$queryStr$scopeStr"
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = colors.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Searching ${scopeConfig.label}...", color = colors.text, fontSize = 18.sp)
            }
        }
        return
    }

    if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error!!, color = colors.primary, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { coroutineScope.launch { handleSearch(query) } }) {
                    Text("Try Again")
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                ScopeDropdown(
                    scope = scope,
                    onScopeChange = { newScope ->
                        scope = newScope
                        showScopeDropdown = false
                        results.clear()
                        hasSearched = false
                        showResultsStats = false
                    },
                    isOpen = showScopeDropdown,
                    onToggle = { showScopeDropdown = !showScopeDropdown },
                    colors = colors
                )
            }
            item {
                SearchInputRow(
                    query = query,
                    onQueryChange = { query = it; showResultsStats = false },
                    onClear = clearSearch,
                    currentVersionDisplay = currentVersionDisplay,
                    onVersionInfoClick = {
                        viewModel.showVersionInfoDialog = true
                        viewModel.versionInfoForDialog = viewModel.currentDbName
                    },
                    onVersionSelectorClick = { showVersionDropdown = true },
                    colors = colors
                )
            }
            item {
                SearchOptionsRow(
                    inverseSearch = inverseSearch,
                    exactPhrase = exactPhrase,
                    onInverseChange = { newValue ->
                        inverseSearch = newValue
                        if (hasSearched && query.trim().isNotEmpty() && !loading) {
                            coroutineScope.launch { handleSearch(null) }
                        }
                    },
                    onExactChange = { newValue ->
                        exactPhrase = newValue
                        if (hasSearched && query.trim().isNotEmpty() && !loading) {
                            coroutineScope.launch { handleSearch(null) }
                        }
                    },
                    colors = colors
                )
            }
            item {
                Button(
                    onClick = { coroutineScope.launch { handleSearch(query) } },
                    enabled = query.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            getResultStats(),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            if (results.isEmpty()) {
                item {
                    EmptySearchState(
                        hasSearched = hasSearched,
                        query = query,
                        loading = loading,
                        onPopularSearch = { term ->
                            query = term
                            coroutineScope.launch { handleSearch(term) }
                        },
                        colors = colors,
                        inverse = inverseSearch,
                        exactPhrase = exactPhrase
                    )
                }
            } else {
                items(results, key = { "${it.bookNumber}-${it.chapter}-${it.verse}" }) { verse ->
                    SearchResultItem(
                        verse = verse,
                        query = query,
                        onVersePress = onPassageSelected,
                        colors = colors,
                        themeColors = themeColors,
                        verseProcessor = verseProcessor,
                        isOldTestament = viewModel.isOldTestament
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showBackToTop && results.size > 10,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            FloatingActionButton(
                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                containerColor = colors.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.ArrowUpward, null, tint = Color.White)
            }
        }
        if (showVersionDropdown) {
            VersionSelectionModal(
                currentVersionKey = currentVersionKey,
                isSecondary = false,
                onVersionSelected = handleVersionChange,
                onDismiss = { showVersionDropdown = false },
                colors = mapOf(
                    "primary" to colors.primary,
                    "card" to colors.card,
                    "text" to colors.text,
                    "muted" to colors.muted,
                    "border" to colors.border
                )
            )
        }
    }
}
@Composable
private fun SearchInputRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    currentVersionDisplay: String,
    onVersionInfoClick: () -> Unit,
    onVersionSelectorClick: () -> Unit,
    colors: SearchColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search...") },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = onClear) { Icon(Icons.Default.Clear, null) } }
            } else null,
            leadingIcon = {
                VersionSelectorChip(
                    displayName = currentVersionDisplay,
                    onInfoClick = onVersionInfoClick,
                    onSelectorClick = onVersionSelectorClick,
                    colors = colors
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VersionSelectorChip(
    displayName: String,
    onInfoClick: () -> Unit,
    onSelectorClick: () -> Unit,
    colors: SearchColors
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onSelectorClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = displayName,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.size(5.dp))
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Version info",
                    tint = colors.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}