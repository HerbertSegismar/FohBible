package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.SCOPE_CATEGORIES
import com.fountofhopedotorg.fohbible.data.SearchColors
import com.fountofhopedotorg.fohbible.data.SearchScope
import com.fountofhopedotorg.fohbible.data.SearchVerse
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.getBookInfo
import com.fountofhopedotorg.fohbible.data.getScopeConfig
import com.fountofhopedotorg.fohbible.screens.POPULAR_SEARCH_TERMS
import com.fountofhopedotorg.fohbible.screens.getBookColor
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor

@Composable
fun SearchOptionsRow(
    inverseSearch: Boolean,
    exactPhrase: Boolean,
    onInverseChange: (Boolean) -> Unit,
    onExactChange: (Boolean) -> Unit,
    colors: SearchColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Inverse search",
                color = colors.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Checkbox(
                colors = CheckboxDefaults.colors(checkmarkColor = Color.White),
                checked = inverseSearch,
                onCheckedChange = onInverseChange,
                modifier = Modifier.size(18.dp)
            )
        }
        Row(
            modifier = Modifier.padding(end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Exact match",
                color = colors.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Checkbox(
                colors = CheckboxDefaults.colors(checkmarkColor = Color.White),
                checked = exactPhrase,
                onCheckedChange = onExactChange,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ScopeDropdown(
    scope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    isOpen: Boolean,
    onToggle: () -> Unit,
    colors: SearchColors,
    dialogTitle: String = "Select Search Scope"
) {
    val currentConfig = getScopeConfig(scope)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = colors.primary),
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
                modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(colors.primary)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            dialogTitle,
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
                                    modifier = Modifier.fillMaxWidth()
                                        .background(colors.primary.copy(0.8f))
                                        .padding(8.dp),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            items(scopes) { scopeKey ->
                                val config = getScopeConfig(scopeKey)
                                val isSelected = scope == scopeKey
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else colors.card)
                                        .clickable { onScopeChange(scopeKey) }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        config.label,
                                        color = if (isSelected) colors.primary else colors.text,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(config.description, color = colors.muted, fontSize = 12.sp)
                                }
                                HorizontalDivider(color = colors.border)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchState(
    hasSearched: Boolean,
    query: String,
    loading: Boolean,
    onPopularSearch: (String) -> Unit,
    colors: SearchColors,
    inverse: Boolean,
    exactPhrase: Boolean
) {
    if (loading) return
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasSearched && query.isEmpty() || query.isEmpty() && !hasSearched) {
            Text(
                "Enter a word or phrase to find relevant verses",
                fontSize = 14.sp,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Popular Search Terms",
                        color = colors.primary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    PopularSearchTermsRow(onSearch = onPopularSearch, colors = colors)
                }
            }
        } else if (hasSearched && query.isNotEmpty()) {
            val noResultsText = when {
                inverse && exactPhrase -> "No verses without the exact phrase \"$query\" found"
                inverse -> "No verses without \"$query\" found"
                exactPhrase -> "No verses with the exact phrase \"$query\" found"
                else -> "No results found for \"$query\""
            }
            Text(
                noResultsText,
                fontSize = 18.sp,
                color = colors.text,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Try different keywords or check spelling",
                fontSize = 14.sp,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Search tips:",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Try simpler or more common words",
                            "Check for typos",
                            "Search for single words first"
                        ).forEach { tip ->
                            Text(
                                text = "• $tip",
                                color = colors.muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PopularSearchTermsRow(
    onSearch: (String) -> Unit,
    colors: SearchColors
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        POPULAR_SEARCH_TERMS.forEach { term ->
            Card(
                modifier = Modifier.padding(4.dp).clickable { onSearch(term) },
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f))
            ) {
                Text(
                    term,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = colors.primary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    verse: SearchVerse,
    query: String,
    onVersePress: (PassageSelection) -> Unit,
    colors: SearchColors,
    themeColors: ThemeColors,
    verseProcessor: VerseTextProcessor,
    isOldTestament: Boolean
) {
    val longName = verse.bookName ?: getBookInfo(verse.bookNumber)?.name ?: "Unknown Book"
    val bookColorStr = verse.bookColor ?: getBookColor(longName, verse)
    val bookColor = Color(bookColorStr.toColorInt())

    val processed = verseProcessor.processVerse(
        verseText = verse.text,
        baseFontSize = 16.sp,
        themeColors = themeColors,
        highlight = query,
        isOldTestament = isOldTestament
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
            onVersePress(
                PassageSelection(
                    bookNumber = verse.bookNumber,
                    bookName = longName,
                    chapter = verse.chapter,
                    verse = verse.verse
                )
            )
        },
        colors = CardDefaults.cardColors(containerColor = colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "$longName ${verse.chapter}:${verse.verse}",
                color = bookColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(processed.body, color = colors.text, fontSize = 16.sp)
        }
    }
}