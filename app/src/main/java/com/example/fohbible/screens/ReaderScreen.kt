@file:Suppress("AssignedValueIsNeverRead")

package com.example.fohbible.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.AppViewModel
import com.example.fohbible.MainActivity
import com.example.fohbible.data.BibleBook
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.data.Verse
import com.example.fohbible.ui.theme.FohBibleTheme
import com.example.fohbible.utils.ProcessedVerse
import com.example.fohbible.utils.ThemeColors
import com.example.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    passage: PassageSelection,
    databaseHelper: DatabaseHelper?,
    onPassageChange: (PassageSelection) -> Unit = {}
) {
    val themeColors = ThemeColors(
        textColor = MaterialTheme.colorScheme.onBackground,
        verseNumber = MaterialTheme.colorScheme.primary,
        primary = MaterialTheme.colorScheme.primary,
        tagColor = MaterialTheme.colorScheme.secondary,
        tagBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        wordsOfJesus = Color(0xFFCB531D),
        searchHighlightBg = Color.Yellow.copy(alpha = 0.3f),
        highlightIcon = MaterialTheme.colorScheme.primary
    )

    val viewModel = viewModel<AppViewModel>()
    val coroutineScope = rememberCoroutineScope()

    // Track current passage - use LaunchedEffect to sync with parent
    var currentPassage by remember { mutableStateOf(passage.copy(verse = 1)) }

    // Sync with parent passage changes using LaunchedEffect
    LaunchedEffect(passage.bookNumber, passage.chapter) {
        if (passage.bookNumber != currentPassage.bookNumber || passage.chapter != currentPassage.chapter) {
            currentPassage = passage.copy(verse = 1)
        }
    }

    // Get current book info
    val currentBook by remember(currentPassage.bookNumber) {
        derivedStateOf { BibleData.getBookByCustomNumber(currentPassage.bookNumber) }
    }

    // Calculate previous and next passages
    val prevPassage by remember(currentPassage, currentBook) {
        derivedStateOf {
            if (currentBook == null) currentPassage else getPreviousPassage(currentPassage, currentBook)
        }
    }
    val nextPassage by remember(currentPassage, currentBook) {
        derivedStateOf {
            if (currentBook == null) currentPassage else getNextPassage(currentPassage, currentBook)
        }
    }
    val hasPrev by remember(prevPassage) { derivedStateOf { prevPassage != currentPassage } }
    val hasNext by remember(nextPassage) { derivedStateOf { nextPassage != currentPassage } }

    // Build list of passages for pager
    val passages by remember(currentPassage, prevPassage, nextPassage, hasPrev, hasNext) {
        derivedStateOf {
            buildList {
                if (hasPrev) add(prevPassage)
                add(currentPassage)
                if (hasNext) add(nextPassage)
            }
        }
    }

    val pageCount by remember(passages) { derivedStateOf { passages.size } }
    val currentOffset by remember(hasPrev) { derivedStateOf { if (hasPrev) 1 else 0 } }

    // Track target passage for swipe completion
    var pendingPassageChange by remember { mutableStateOf<PassageSelection?>(null) }

    // Track loaded verses - use snapshotFlow to clear when databaseHelper changes
    val loadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<Verse>>() }

    // Clear loaded verses when databaseHelper changes
    LaunchedEffect(databaseHelper) {
        loadedVerses.clear()
    }

    // Load verses for current, previous, and next passages
    LaunchedEffect(currentPassage, hasPrev, hasNext, databaseHelper) {
        // Load current passage
        val currentKey = currentPassage.bookNumber to currentPassage.chapter
        if (currentKey !in loadedVerses) {
            loadedVerses[currentKey] = databaseHelper?.getVerses(currentPassage.bookNumber, currentPassage.chapter) ?: emptyList()
        }

        // Load previous passage if available
        if (hasPrev) {
            val prevKey = prevPassage.bookNumber to prevPassage.chapter
            if (prevKey !in loadedVerses) {
                loadedVerses[prevKey] = databaseHelper?.getVerses(prevPassage.bookNumber, prevPassage.chapter) ?: emptyList()
            }
        }

        // Load next passage if available
        if (hasNext) {
            val nextKey = nextPassage.bookNumber to nextPassage.chapter
            if (nextKey !in loadedVerses) {
                loadedVerses[nextKey] = databaseHelper?.getVerses(nextPassage.bookNumber, nextPassage.chapter) ?: emptyList()
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = currentOffset,
        pageCount = { pageCount }
    )

    // Track swipe state
    var isUserSwiping by remember { mutableStateOf(false) }
    var swipeCompleted by remember { mutableStateOf(false) }

    // Handle page changes during swipe
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            isUserSwiping = true
            swipeCompleted = false
            val offset = if (hasPrev) 1 else 0
            if (pagerState.currentPage < offset) {
                if (hasPrev) pendingPassageChange = prevPassage
            } else if (pagerState.currentPage > offset) {
                if (hasNext) pendingPassageChange = nextPassage
            }
        } else if (isUserSwiping) {
            // Swipe just ended
            isUserSwiping = false
            // Process pending passage change if any
            val targetPassage = pendingPassageChange
            if (targetPassage != null && !swipeCompleted) {
                swipeCompleted = true
                currentPassage = targetPassage
                onPassageChange(targetPassage)
                pendingPassageChange = null
                // Reset will be handled by the other LaunchedEffect
            } else {
                // No change (edge swipe), reset to center
                coroutineScope.launch {
                    val offset = if (hasPrev) 1 else 0
                    pagerState.scrollToPage(offset)
                }
            }
        }
    }

    // Reset pager to center when passage changes (non-user initiated)
    LaunchedEffect(currentPassage) {
        if (!isUserSwiping) {
            coroutineScope.launch {
                // Small delay to ensure any ongoing animations complete
                delay(50)
                val offset = if (hasPrev) 1 else 0
                pagerState.scrollToPage(offset)
                pendingPassageChange = null
            }
        }
    }

    val context = LocalContext.current
    val systemFont = FontFamily.Default
    val oswaldFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Oswald.ttf")) }
    val poppinsFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Poppins.ttf")) }
    val rubikGlitchFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/RubikGlitch.ttf")) }

    val currentFontFamily = when (viewModel.selectedFontFamily) {
        "system" -> systemFont
        "oswald" -> oswaldFont
        "rubik-glitch" -> rubikGlitchFont
        "poppins" -> poppinsFont
        else -> systemFont
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { pageIndex ->
                val passageKey = passages[pageIndex]
                "${passageKey.bookNumber}-${passageKey.chapter}"
            }
        ) { pageIndex ->
            val thisPassage = passages[pageIndex]
            val thisVerses = loadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()

            val processor = remember(thisVerses) { VerseTextProcessor() }
            val processedVerses = remember(thisVerses, themeColors) {
                val result = mutableMapOf<Int, ProcessedVerse>()
                for (verse in thisVerses) {
                    val processed = processor.processVerse(
                        verseText = verse.text,
                        baseFontSize = viewModel.fontSize.sp,
                        themeColors = themeColors,
                        textColor = themeColors.textColor
                    )
                    result[verse.verseNumber] = processed
                }
                result
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (thisVerses.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading verses...")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            // Display chapter header
                            Text(
                                text = "${thisPassage.bookName} ${thisPassage.chapter}",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = (viewModel.fontSize + 4f).sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                textAlign = TextAlign.Center,
                                fontFamily = currentFontFamily
                            )
                        }
                        // Display verses with processed text
                        items(thisVerses) { verse ->
                            val processedVerse = processedVerses[verse.verseNumber]
                            if (processedVerse != null) {
                                // Display the processed verse with header and body
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    // Display header if exists
                                    processedVerse.header?.let { header ->
                                        if (header.text.isNotEmpty()) {
                                            Text(
                                                text = header,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = themeColors.tagColor,
                                                modifier = Modifier.padding(bottom = 4.dp),
                                                fontFamily = currentFontFamily
                                            )
                                        }
                                    }
                                    // Display verse number and body
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(
                                                    style = SpanStyle(
                                                        fontWeight = FontWeight.Bold,
                                                        color = themeColors.verseNumber,
                                                        fontSize = (viewModel.fontSize * 0.778f).sp
                                                    )
                                                ) {
                                                    append("${verse.verseNumber} ")
                                                }
                                                append(processedVerse.body)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = viewModel.fontSize.sp,
                                            lineHeight = (viewModel.fontSize * 1.333f).sp,
                                            fontFamily = currentFontFamily
                                        )
                                    }
                                }
                            } else {
                                // Fallback: display original verse text
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${verse.verseNumber}.",
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.verseNumber,
                                        fontSize = (viewModel.fontSize * 0.778f).sp,
                                        fontFamily = currentFontFamily
                                    )
                                    Text(
                                        text = verse.text,
                                        modifier = Modifier.weight(1f),
                                        fontSize = viewModel.fontSize.sp,
                                        lineHeight = (viewModel.fontSize * 1.333f).sp,
                                        fontFamily = currentFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getPreviousPassage(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters <= 2 && current.chapter == 1) return current
    return if (current.chapter == 1) {
        // If at first chapter, find previous book's last chapter
        val prevBook = BibleData.getBookByCustomNumber(current.bookNumber - 1)
        if (prevBook != null) {
            current.copy(
                bookNumber = prevBook.customNumber,
                bookName = prevBook.name,
                chapter = prevBook.chapters,
                verse = 1
            )
        } else {
            // If no previous book, wrap to last chapter of current book
            current.copy(chapter = currentBook.chapters, verse = 1)
        }
    } else {
        // Normal previous chapter within same book
        current.copy(chapter = current.chapter - 1, verse = 1)
    }
}

fun getNextPassage(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters <= 2 && current.chapter == currentBook.chapters) return current
    return if (current.chapter == currentBook.chapters) {
        // If at last chapter, find next book's first chapter
        val nextBook = BibleData.getBookByCustomNumber(current.bookNumber + 1)
        if (nextBook != null) {
            current.copy(
                bookNumber = nextBook.customNumber,
                bookName = nextBook.name,
                chapter = 1,
                verse = 1
            )
        } else {
            // If no next book, wrap to first chapter of current book
            current.copy(chapter = 1, verse = 1)
        }
    } else {
        // Normal next chapter within same book
        current.copy(chapter = current.chapter + 1, verse = 1)
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    val context = LocalContext.current
    FohBibleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ReaderScreen(
                passage = PassageSelection(
                    bookNumber = 10,
                    bookName = "Genesis",
                    chapter = 1,
                    verse = 1
                ),
                databaseHelper = DatabaseHelper(context as MainActivity, "kj2.sqlite3")
            )
        }
    }
}