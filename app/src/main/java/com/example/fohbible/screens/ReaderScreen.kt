@file:Suppress("AssignedValueIsNeverRead")

package com.example.fohbible.screens

import android.graphics.Typeface
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    passage: PassageSelection,
    databaseHelper: DatabaseHelper?,
    onPassageChange: (PassageSelection) -> Unit = {}
) {
    val viewModel = viewModel<AppViewModel>()
    val themeColors = ThemeColors(
        textColor = MaterialTheme.colorScheme.onBackground,
        verseNumber = MaterialTheme.colorScheme.primary,
        primary = MaterialTheme.colorScheme.primary,
        tagColor = MaterialTheme.colorScheme.secondary,
        tagBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        wordsOfJesus = Color(0xFFDA4227),
        searchHighlightBg = if (viewModel.darkTheme) Color(0xFF81D4FA).copy(alpha = 0.3f) else Color.Yellow.copy(alpha = 0.3f),
        highlightIcon = MaterialTheme.colorScheme.primary
    )
    val coroutineScope = rememberCoroutineScope()

    // Track current passage - use LaunchedEffect to sync with parent
    var currentPassage by remember { mutableStateOf(passage.copy(verse = 1)) }
    var targetVerse by remember { mutableStateOf(passage.verse) }

    // Sync with parent passage changes using LaunchedEffect
    LaunchedEffect(passage.bookNumber, passage.chapter, passage.verse) {
        if (passage.bookNumber != currentPassage.bookNumber || passage.chapter != currentPassage.chapter) {
            currentPassage = passage.copy(verse = 1)
            targetVerse = passage.verse
        } else {
            targetVerse = passage.verse
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

    // Track loaded verses for primary and secondary
    val primaryLoadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<Verse>>() }
    val secondaryLoadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<Verse>>() }

    // Clear loaded verses when databaseHelper changes
    LaunchedEffect(databaseHelper) { primaryLoadedVerses.clear() }

    // Secondary database helper
    val context = LocalContext.current
    var secondaryDatabaseHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    LaunchedEffect(viewModel.multiVersion, viewModel.secondaryDbName) {
        secondaryDatabaseHelper?.close()
        secondaryDatabaseHelper = if (viewModel.multiVersion && viewModel.secondaryDbName.isNotEmpty()) {
            DatabaseHelper(context as MainActivity, viewModel.secondaryDbName)
        } else {
            null
        }
    }
    LaunchedEffect(secondaryDatabaseHelper) { secondaryLoadedVerses.clear() }

    // Load verses for current, previous, and next passages for primary and secondary if multi-version
    LaunchedEffect(currentPassage, hasPrev, hasNext, databaseHelper, secondaryDatabaseHelper, viewModel.multiVersion) {
        // Load primary
        val currentKey = currentPassage.bookNumber to currentPassage.chapter
        if (currentKey !in primaryLoadedVerses) {
            primaryLoadedVerses[currentKey] = databaseHelper?.getVerses(currentPassage.bookNumber, currentPassage.chapter) ?: emptyList()
        }
        if (hasPrev) {
            val prevKey = prevPassage.bookNumber to prevPassage.chapter
            if (prevKey !in primaryLoadedVerses) {
                primaryLoadedVerses[prevKey] = databaseHelper?.getVerses(prevPassage.bookNumber, prevPassage.chapter) ?: emptyList()
            }
        }
        if (hasNext) {
            val nextKey = nextPassage.bookNumber to nextPassage.chapter
            if (nextKey !in primaryLoadedVerses) {
                primaryLoadedVerses[nextKey] = databaseHelper?.getVerses(nextPassage.bookNumber, nextPassage.chapter) ?: emptyList()
            }
        }

        // Load secondary if multi-version enabled and helper exists
        if (viewModel.multiVersion && secondaryDatabaseHelper != null) {
            if (currentKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[currentKey] = secondaryDatabaseHelper?.getVerses(currentPassage.bookNumber, currentPassage.chapter) ?: emptyList()
            }
            if (hasPrev) {
                val prevKey = prevPassage.bookNumber to prevPassage.chapter
                if (prevKey !in secondaryLoadedVerses) {
                    secondaryLoadedVerses[prevKey] = secondaryDatabaseHelper?.getVerses(prevPassage.bookNumber, prevPassage.chapter) ?: emptyList()
                }
            }
            if (hasNext) {
                val nextKey = nextPassage.bookNumber to nextPassage.chapter
                if (nextKey !in secondaryLoadedVerses) {
                    secondaryLoadedVerses[nextKey] = secondaryDatabaseHelper?.getVerses(nextPassage.bookNumber, nextPassage.chapter) ?: emptyList()
                }
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
                targetVerse = targetPassage.verse
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

    val contextFont = LocalContext.current
    val systemFont = FontFamily.Default
    val oswaldFont = remember { FontFamily(Typeface.createFromAsset(contextFont.assets, "fonts/Oswald.ttf")) }
    val poppinsFont = remember { FontFamily(Typeface.createFromAsset(contextFont.assets, "fonts/Poppins.ttf")) }
    val rubikGlitchFont = remember { FontFamily(Typeface.createFromAsset(contextFont.assets, "fonts/RubikGlitch.ttf")) }
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
            val primaryVerses = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
            val effectiveMultiVersion = viewModel.multiVersion && secondaryDatabaseHelper != null
            val secondaryVerses = if (effectiveMultiVersion) secondaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList() else emptyList()
            val isCurrentPage = thisPassage.bookNumber == currentPassage.bookNumber && thisPassage.chapter == currentPassage.chapter

            Box(modifier = Modifier.fillMaxSize()) {
                if (primaryVerses.isEmpty()) {
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
                    val primaryState = rememberScrollState()
                    val secondaryState = if (effectiveMultiVersion) rememberScrollState() else null

                    if (effectiveMultiVersion) {
                        // Sync logic using snapshotFlow
                        if (viewModel.scrollSync && secondaryState != null) {
                            LaunchedEffect(primaryState) {
                                snapshotFlow { primaryState.value }.collect { _ ->
                                    val pMax = primaryState.maxValue.coerceAtLeast(1)
                                    val sMax = secondaryState.maxValue.coerceAtLeast(1)
                                    val fraction = primaryState.value.toFloat() / pMax
                                    val targetS = (fraction * sMax).roundToInt()
                                    val currentS = secondaryState.value
                                    val deltaS = targetS - currentS
                                    if (abs(deltaS) > 5) { // Epsilon to avoid loops and minor discrepancies
                                        secondaryState.scrollBy(deltaS.toFloat())
                                    }
                                }
                            }
                            LaunchedEffect(secondaryState) {
                                snapshotFlow { secondaryState.value }.collect { _ ->
                                    val pMax = primaryState.maxValue.coerceAtLeast(1)
                                    val sMax = secondaryState.maxValue.coerceAtLeast(1)
                                    val fraction = secondaryState.value.toFloat() / sMax
                                    val targetP = (fraction * pMax).roundToInt()
                                    val currentP = primaryState.value
                                    val deltaP = targetP - currentP
                                    if (abs(deltaP) > 5) { // Epsilon to avoid loops and minor discrepancies
                                        primaryState.scrollBy(deltaP.toFloat())
                                    }
                                }
                            }
                        }

                        if (viewModel.multiViewLayout == "horizontal") {
                            Row(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                ChapterView(
                                    passage = thisPassage,
                                    verses = primaryVerses,
                                    themeColors = themeColors,
                                    currentFontFamily = currentFontFamily,
                                    viewModel = viewModel,
                                    isCurrentPage = isCurrentPage,
                                    targetVerse = targetVerse,
                                    versionAbbr = viewModel.currentVersionAbbr,
                                    state = primaryState,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    if (secondaryVerses.isEmpty()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Loading secondary verses...")
                                        }
                                    } else {
                                        ChapterView(
                                            passage = thisPassage,
                                            verses = secondaryVerses,
                                            themeColors = themeColors,
                                            currentFontFamily = currentFontFamily,
                                            viewModel = viewModel,
                                            isCurrentPage = isCurrentPage,
                                            targetVerse = targetVerse,
                                            versionAbbr = viewModel.secondaryVersionAbbr,
                                            state = secondaryState!!,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                ChapterView(
                                    passage = thisPassage,
                                    verses = primaryVerses,
                                    themeColors = themeColors,
                                    currentFontFamily = currentFontFamily,
                                    viewModel = viewModel,
                                    isCurrentPage = isCurrentPage,
                                    targetVerse = targetVerse,
                                    versionAbbr = viewModel.currentVersionAbbr,
                                    state = primaryState,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    if (secondaryVerses.isEmpty()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Loading secondary verses...")
                                        }
                                    } else {
                                        ChapterView(
                                            passage = thisPassage,
                                            verses = secondaryVerses,
                                            themeColors = themeColors,
                                            currentFontFamily = currentFontFamily,
                                            viewModel = viewModel,
                                            isCurrentPage = isCurrentPage,
                                            targetVerse = targetVerse,
                                            versionAbbr = viewModel.secondaryVersionAbbr,
                                            state = secondaryState!!,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        ChapterView(
                            passage = thisPassage,
                            verses = primaryVerses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = targetVerse,
                            versionAbbr = viewModel.currentVersionAbbr,
                            state = primaryState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterView(
    passage: PassageSelection,
    verses: List<Verse>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    isCurrentPage: Boolean,
    targetVerse: Int?,
    versionAbbr: String,
    state: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val processor = remember(verses) { VerseTextProcessor() }
    val processedVerses = remember(verses, themeColors) {
        val result = mutableMapOf<Int, ProcessedVerse>()
        for (verse in verses) {
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
    var highlightedVerse by remember { mutableStateOf<Int?>(null) }
    val offsets = remember { mutableStateMapOf<Int, Float>() }

    Column(
        modifier = modifier
            .verticalScroll(state)
            .padding(16.dp)
    ) {
        // Display chapter header
        Text(
            text = "${passage.bookName} ${passage.chapter} $versionAbbr",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(themeColors.tagBg)
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
            fontFamily = currentFontFamily
        )

        // Display verses with processed text
        verses.forEach { verse ->
            val processedVerse = processedVerses[verse.verseNumber]
            val isHighlighted = verse.verseNumber == highlightedVerse
            if (processedVerse != null) {
                // Display the processed verse with header and body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .then(if (isHighlighted) Modifier.background(themeColors.searchHighlightBg) else Modifier)
                        .onGloballyPositioned { coords ->
                            offsets[verse.verseNumber] = coords.positionInParent().y
                        }
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
                        .padding(vertical = 8.dp)
                        .then(if (isHighlighted) Modifier.background(themeColors.searchHighlightBg) else Modifier)
                        .onGloballyPositioned { coords ->
                            offsets[verse.verseNumber] = coords.positionInParent().y
                        },
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

    if (isCurrentPage) {
        LaunchedEffect(targetVerse, verses) {
            targetVerse?.let { v ->
                if (verses.isNotEmpty() && v > 0) {
                    delay(200) // Wait for layout
                    val offset = offsets[v] ?: return@let
                    state.animateScrollTo(offset.toInt())
                    highlightedVerse = targetVerse
                    delay(2000)
                    highlightedVerse = null
                }
            }
        }
    }
}

fun getPreviousPassage(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters <= 2 && current.chapter == 1) return current
    return if (current.chapter == 1) {
        current.copy(chapter = currentBook.chapters, verse = null)
    } else {
        current.copy(chapter = current.chapter - 1, verse = null)
    }
}

fun getNextPassage(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters <= 2 && current.chapter == currentBook.chapters) return current
    return if (current.chapter == currentBook.chapters) {
        current.copy(chapter = 1, verse = null)
    } else {
        current.copy(chapter = current.chapter + 1, verse = null)
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