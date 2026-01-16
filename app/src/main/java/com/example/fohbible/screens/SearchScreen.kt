@file:Suppress("UnusedImport", "VariableNaming", "FunctionName", "LocalVariableName", "UnusedParameter")

package com.example.fohbible.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.Testament
import com.example.fohbible.data.getBookInfo
import com.example.fohbible.data.getScopeConfig
import com.example.fohbible.data.isBookScope
import com.example.fohbible.data.getBookNumberFromScope
import com.example.fohbible.data.SCOPE_CATEGORIES
import com.example.fohbible.data.SCOPE_RANGES
import com.example.fohbible.data.SearchScope
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.ui.theme.LocalAppTheme
import com.example.fohbible.utils.ThemeColors
import com.example.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.AppViewModel
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.scopeColors
import kotlin.Boolean

fun getScopeForBookNumber(bookNumber: Int): String? {
    for ((scope, range) in SCOPE_RANGES) {
        if (range != null && bookNumber >= range.start && bookNumber <= range.end && scope != "whole" && scope != "old-testament" && scope != "new-testament") {
            return scope
        }
    }
    return null
}

data class SearchOptions(val bookRange: Pair<Int, Int>? = null)

// Add to DatabaseHelper
fun DatabaseHelper.searchVerses(query: String, options: SearchOptions? = null): List<Verse> {
    val verses = mutableListOf<Verse>()
    try {
        if (database == null || !database!!.isOpen) return verses
        var whereClause = ""
        val args = mutableListOf<String>()
        query.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.forEach { word ->
            if (whereClause.isNotEmpty()) whereClause += " AND "
            whereClause += "text LIKE ?"
            args.add("%$word%")
        }
        if (options?.bookRange != null) {
            if (whereClause.isNotEmpty()) whereClause += " AND "
            whereClause += "book_number BETWEEN ? AND ?"
            args.add(options.bookRange.first.toString())
            args.add(options.bookRange.second.toString())
        }
        val cursor = database?.query(
            "verses",
            arrayOf("book_number", "chapter", "verse", "text"),
            whereClause.ifEmpty { null },
            if (args.isEmpty()) null else args.toTypedArray(),
            null,
            null,
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val bookNumber = it.getInt(it.getColumnIndexOrThrow("book_number"))
                val chapter = it.getInt(it.getColumnIndexOrThrow("chapter"))
                val verseNum = it.getInt(it.getColumnIndexOrThrow("verse"))
                val text = it.getString(it.getColumnIndexOrThrow("text"))
                val bookName = getBookInfo(bookNumber)?.name
                verses.add(Verse(verseNum, text, bookNumber, chapter, bookName))
            }
        }
    } catch (e: Exception) {
        Log.e("DatabaseHelper", "Error in searchVerses: ${e.message}")
    }
    return verses
}

data class Verse(
    val verse: Int,
    val text: String?,
    val bookNumber: Int = 0,
    val chapter: Int = 0,
    val bookName: String? = null,
    val bookColor: String? = null
)

val BOOK_COLORS: Map<String, String> = mapOf()

fun generateColorFromString(str: String): String {
    var hash = 0
    for (char in str) {
        hash = char.code + ((hash shl 5) - hash)
    }
    val colors = listOf(
        "#3B82F6", "#EF4444", "#10B981", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4", "#84CC16", "#F97316", "#6366F1"
    )
    return colors[abs(hash) % colors.size]
}

fun getBookColor(bookName: String, verse: Verse? = null): String {
    verse?.bookColor?.let { return it }
    val normalizedBookName = bookName.lowercase().trim()
    BOOK_COLORS[normalizedBookName]?.let { return it }
    val bookNumber = verse?.bookNumber ?: return generateColorFromString(bookName)
    val scope = getScopeForBookNumber(bookNumber)
    val hex = scope?.let { scopeColors[it] } ?: if (getBookInfo(bookNumber)?.testament == Testament.OLD) "#DC2626" else "#059669"
    return hex
}

suspend fun enhanceSearchResultsWithColors(
    results: List<Verse>,
    dbHelper: DatabaseHelper?
): List<Verse> {
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
            } catch (error: Exception) {
                Log.e("SearchScreen", "Error fetching book $bookNumber:", error)
                colorMap[bookNumber] = generateColorFromString(bookNumber.toString())
            }
        }
    }
    return results.map { result -> result.copy(bookColor = colorMap[result.bookNumber]) }
}

@Composable
fun SearchScreen(
    databaseHelper: DatabaseHelper?,
    onPassageSelected: (PassageSelection) -> Unit
) {
    val theme = LocalAppTheme.current
    val isDark = theme.darkTheme
    val primaryColor = theme.primaryColor
    val colors = mapOf(
        "primary" to primaryColor,
        "background" to if (isDark) Color(0xFF0f172a) else Color(0xFFF8FAFC),
        "text" to if (isDark) Color.White else Color.Black,
        "muted" to if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
        "card" to if (isDark) Color(0xFF1E293B) else Color.White,
        "border" to if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB),
    )
    var hasSearched by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<Verse>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scope by remember { mutableStateOf("whole") }
    var showScopeDropdown by remember { mutableStateOf(false) }
    var showResultsStats by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemScrollOffset > 300 || listState.firstVisibleItemIndex > 0 } }

    val handleQueryChange: (String) -> Unit = { text ->
        query = text
        showResultsStats = false
    }

    val handleSearch: suspend (String?) -> Unit = Unit@{ searchQuery ->
        val actualQuery = searchQuery ?: query
        hasSearched = true
        showResultsStats = false
        if (actualQuery.trim().isEmpty()) {
            results.clear()
            return@Unit
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
                databaseHelper?.searchVerses(actualQuery, searchOptions) ?: emptyList()
            }
            val enhancedResults = enhanceSearchResultsWithColors(searchResults, databaseHelper)
            results.clear()
            results.addAll(enhancedResults)
            showResultsStats = true
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        } catch (err: Exception) {
            Log.e("SearchScreen", "Search error", err)
            error = "Failed to search. Please try again."
        } finally {
            loading = false
        }
    }

    val handlePopularSearch: (String) -> Unit = { term ->
        query = term
        coroutineScope.launch { handleSearch(term) }
    }

    val handleScopeChange: (SearchScope) -> Unit = { newScope ->
        scope = newScope
        showScopeDropdown = false
        results.clear()
        hasSearched = false
        showResultsStats = false
    }

    val handleVersePress: (Verse) -> Unit = { verse ->
        val bookName = getBookInfo(verse.bookNumber)?.name ?: verse.bookName ?: "Unknown Book"
        onPassageSelected(
            PassageSelection(
                bookNumber = verse.bookNumber,
                bookName = bookName,
                chapter = verse.chapter,
                verse = verse.verse
            )
        )
    }

    val clearSearch: () -> Unit = {
        query = ""
        results.clear()
        hasSearched = false
        showResultsStats = false
    }

    val getResultStats: () -> String = {
        if (!hasSearched || loading || !showResultsStats) {
            val config = getScopeConfig(scope)
            "Search ${config.label}"
        } else if (results.isEmpty()) {
            val config = getScopeConfig(scope)
            "No results found for \"$query\" in ${config.label}"
        } else {
            val bookCount = results.map { it.bookNumber }.toSet().size
            val config = getScopeConfig(scope)
            if (isBookScope(scope)) {
                "Found ${results.size} result${if (results.size != 1) "s" else ""} in ${config.label}"
            } else {
                "Found ${results.size} result${if (results.size != 1) "s" else ""} in $bookCount book${if (bookCount != 1) "s" else ""} for \"$query\""
            }
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = colors["primary"] as Color)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Searching ${getScopeConfig(scope).label}...", color = colors["text"] as Color, fontSize = 18.sp)
            }
        }
        return
    }

    if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error!!, color = colors["primary"] as Color, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { coroutineScope.launch { handleSearch(query) } }) {
                    Text("Try Again")
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(colors["background"] as Color)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                ScopeDropdown(
                    scope = scope,
                    onScopeChange = handleScopeChange,
                    isOpen = showScopeDropdown,
                    onToggle = { showScopeDropdown = !showScopeDropdown },
                    colors = colors
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = handleQueryChange,
                        placeholder = { Text("Search ${getScopeConfig(scope).label.lowercase()}...") },
                        trailingIcon = if (query.isNotEmpty()) {
                            { IconButton(onClick = clearSearch) { Icon(Icons.Default.Clear, null) } }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Button(
                    onClick = { coroutineScope.launch { handleSearch(query) } },
                    enabled = query.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors["primary"] as Color)
                ) {
                    Text(getResultStats(), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            if (results.isEmpty()) {
                item {
                    EmptyStates(
                        hasSearched = hasSearched,
                        query = query,
                        loading = loading,
                        onPopularSearch = handlePopularSearch,
                        colors = colors
                    )
                }
            } else {
                items(results, key = { "${it.bookNumber}-${it.chapter}-${it.verse}" }) { verse ->
                    SearchResultItem(verse = verse, query = query, onVersePress = handleVersePress, colors = colors)
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
                containerColor = colors["primary"] as Color,
                shape = CircleShape
            ) {
                Icon(Icons.Default.ArrowUpward, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun ScopeDropdown(
    scope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    isOpen: Boolean,
    onToggle: () -> Unit,
    colors: Map<String, Color>
) {
    val currentConfig = getScopeConfig(scope)
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = colors["primary"] as Color),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(currentConfig.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(currentConfig.description, color = Color.White, fontSize = 12.sp)
            }
            Text(if (isOpen) "↑" else "↓", color = Color.White, fontSize = 20.sp)
        }
    }
    if (isOpen) {
        Dialog(onDismissRequest = onToggle) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(colors["primary"] as Color).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Select Search Scope",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onToggle) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                    LazyColumn(modifier = Modifier.height(500.dp)) {
                        SCOPE_CATEGORIES.forEach { (category, scopes) ->
                            item {
                                Text(
                                    category,
                                    modifier = Modifier.fillMaxWidth().background(colors["primary"] as Color).padding(8.dp),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            items(scopes) { scopeKey ->
                                val config = getScopeConfig(scopeKey)
                                val isSelected = scope == scopeKey
                                Column(
                                    modifier = Modifier.fillMaxWidth().background(if (isSelected) (colors["primary"] as Color).copy(alpha = 0.1f) else colors["card"] as Color).clickable { onScopeChange(scopeKey) }.padding(16.dp)
                                ) {
                                    Text(
                                        config.label,
                                        color = if (isSelected) colors["primary"] as Color else colors["text"] as Color,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(config.description, color = colors["muted"] as Color, fontSize = 12.sp)
                                }
                                HorizontalDivider(color = colors["border"] as Color)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PopularSearchTerms(
    onSearch: (String) -> Unit,
    colors: Map<String, Color>
) {
    val terms = listOf("faith", "love", "hope", "grace", "peace", "joy", "forgiveness", "salvation")
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth().wrapContentHeight()) {
        terms.forEach { term ->
            Card(
                modifier = Modifier.padding(4.dp).clickable { onSearch(term) },
                colors = CardDefaults.cardColors(containerColor = colors["card"] as Color),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.dp, (colors["primary"] as Color).copy(alpha = 0.2f))
            ) {
                Text(
                    term,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = colors["primary"] as Color,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyStates(
    hasSearched: Boolean,
    query: String,
    loading: Boolean,
    onPopularSearch: (String) -> Unit,
    colors: Map<String, Color>
) {
    if (loading) return
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasSearched && query.isEmpty() || query.isEmpty() && !hasSearched) {
            Text("Search the Bible", fontSize = 18.sp, color = colors["text"] as Color, modifier = Modifier.padding(bottom = 8.dp))
            Text("Enter a word or phrase to find relevant verses", fontSize = 14.sp, color = colors["muted"] as Color, modifier = Modifier.padding(bottom = 24.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = (colors["primary"] as Color).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Popular Search Terms", color = colors["primary"] as Color, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
                    PopularSearchTerms(onSearch = onPopularSearch, colors = colors)
                }
            }
        } else if (hasSearched && query.isNotEmpty()) {
            Text("No results found for \"$query\"", fontSize = 18.sp, color = colors["text"] as Color, modifier = Modifier.padding(bottom = 8.dp))
            Text("Try different keywords or check spelling", fontSize = 14.sp, color = colors["muted"] as Color, modifier = Modifier.padding(bottom = 24.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colors["card"] as Color),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search tips:", color = colors["text"] as Color, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
                    Text("• Try simpler or more common words\n• Check for typos\n• Search for single words first", color = colors["muted"] as Color, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    verse: Verse,
    query: String,
    onVersePress: (Verse) -> Unit,
    colors: Map<String, Color>
) {
    val longName = getBookInfo(verse.bookNumber)?.name ?: verse.bookName ?: "Unknown Book"
    val bookColorStr = verse.bookColor ?: getBookColor(longName, verse)
    val bookColor = Color(bookColorStr.toColorInt())
    val processor = VerseTextProcessor()
    val viewModel = viewModel<AppViewModel>()
    val themeColors = ThemeColors(
        textColor = colors["text"] as Color,
        verseNumber = bookColor,
        primary = colors["primary"] as Color,
        tagColor = colors["muted"] as Color,
        tagBg = colors["card"] as Color,
        wordsOfJesus = Color(0xFFDA4227),
        searchHighlightBg = if (viewModel.darkTheme) Color(0xFF81D4FA).copy(alpha = 0.3f) else Color.Yellow.copy(alpha = 0.3f),
        highlightIcon = colors["primary"] as Color
    )
    val processed = processor.processVerse(
        verseText = verse.text,
        baseFontSize = 16.sp,
        themeColors = themeColors,
        highlight = query,
        isOldTestament = viewModel.isOldTestament
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onVersePress(verse) },
        colors = CardDefaults.cardColors(containerColor = colors["card"] as Color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "$longName ${verse.chapter}:${verse.verse}",
                color = bookColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(processed.body, color = colors["text"] as Color, fontSize = 16.sp)
        }
    }
}