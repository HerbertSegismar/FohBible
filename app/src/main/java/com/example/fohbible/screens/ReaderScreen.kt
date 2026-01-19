// Modified ReaderScreen.kt
@file:Suppress("AssignedValueIsNeverRead")

package com.example.fohbible.screens

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fohbible.AppViewModel
import com.example.fohbible.MainActivity
import com.example.fohbible.data.BibleBook
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.data.Verse
import com.example.fohbible.modals.CommentaryModal
import com.example.fohbible.modals.DefinitionModal
import com.example.fohbible.modals.StrongsModal
import com.example.fohbible.ui.theme.FohBibleTheme
import com.example.fohbible.utils.ProcessedVerse
import com.example.fohbible.utils.ThemeColors
import com.example.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("AutoboxingStateCreation")
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

    var primaryCurrent by remember { mutableStateOf(passage.copy(verse = 1)) }
    var secondaryCurrent by remember { mutableStateOf(viewModel.secondaryPassage.copy(verse = 1)) }
    var targetVerse by remember { mutableStateOf(passage.verse) }
    var secondaryTargetVerse by remember { mutableStateOf(viewModel.secondaryPassage.verse) }

    LaunchedEffect(passage.bookNumber, passage.chapter, passage.verse) {
        if (passage.bookNumber != primaryCurrent.bookNumber || passage.chapter != primaryCurrent.chapter) {
            primaryCurrent = passage.copy(verse = 1)
            targetVerse = passage.verse
        } else {
            targetVerse = passage.verse
        }
    }

    LaunchedEffect(viewModel.secondaryPassage.bookNumber, viewModel.secondaryPassage.chapter, viewModel.secondaryPassage.verse) {
        if (viewModel.secondaryPassage.bookNumber != secondaryCurrent.bookNumber || viewModel.secondaryPassage.chapter != secondaryCurrent.chapter) {
            secondaryCurrent = viewModel.secondaryPassage.copy(verse = 1)
            secondaryTargetVerse = viewModel.secondaryPassage.verse
        } else {
            secondaryTargetVerse = viewModel.secondaryPassage.verse
        }
    }

    LaunchedEffect(viewModel.scrollSync, viewModel.multiVersion) {
        if (viewModel.multiVersion && viewModel.scrollSync) {
            viewModel.secondaryPassage = viewModel.primaryPassage
            secondaryCurrent = primaryCurrent
        }
    }

    val primaryLoadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<Verse>>() }
    val secondaryLoadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<Verse>>() }

    LaunchedEffect(databaseHelper) {
        primaryLoadedVerses.clear()
    }

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

    LaunchedEffect(secondaryDatabaseHelper) {
        secondaryLoadedVerses.clear()
    }

    val multi = viewModel.multiVersion
    val synced = viewModel.scrollSync && multi && secondaryDatabaseHelper != null

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

    var isButtonVisible by remember { mutableStateOf(true) }
    val buttonAlpha by animateFloatAsState(if (isButtonVisible) 1f else 0.2f, label = "buttonAlpha")
    val scope = rememberCoroutineScope()

    fun scheduleFade() {
        scope.launch {
            delay(5000)
            isButtonVisible = false
        }
    }

    LaunchedEffect(primaryCurrent) {
        isButtonVisible = true
        scheduleFade()
    }

    var dictionaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var strongDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var commentaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var secondaryCommentaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }

    var showWordModal by remember { mutableStateOf(false) }
    var currentWord by remember { mutableStateOf("") }
    var wordDefinition by remember { mutableStateOf("") }

    var showStrongsModal by remember { mutableStateOf(false) }
    var currentStrongNumber by remember { mutableStateOf("") }
    var strongDefinition by remember { mutableStateOf("") }

    var showCommentaryModal by remember { mutableStateOf(false) }
    var commentaryTitle by remember { mutableStateOf("") }
    var commentaryContent by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.selectedDictionary, databaseHelper?.databaseName) {
        dictionaryDbHelper?.close()
        dictionaryDbHelper = DatabaseHelper(context, "${viewModel.selectedDictionary}.dictionary.sqlite3")
        strongDbHelper?.close()
        strongDbHelper = DatabaseHelper(context, "secedictionary.sqlite3")
        commentaryDbHelper?.close()
        val name = databaseHelper?.databaseName ?: return@LaunchedEffect
        val comName = name.replace(".sqlite3", "com.sqlite3")
        commentaryDbHelper = if (comName.isNotEmpty()) DatabaseHelper(context, comName) else null
        if (showWordModal && currentWord.isNotEmpty()) {
            wordDefinition = dictionaryDbHelper?.getWordDefinition(currentWord) ?: "Definition not found."
        }
    }

    LaunchedEffect(viewModel.multiVersion, viewModel.secondaryDbName, secondaryDatabaseHelper?.databaseName) {
        secondaryCommentaryDbHelper?.close()
        val name = secondaryDatabaseHelper?.databaseName ?: return@LaunchedEffect
        val comName = name.replace(".sqlite3", "com.sqlite3")
        secondaryCommentaryDbHelper = if (viewModel.multiVersion && comName.isNotEmpty()) DatabaseHelper(context, comName) else null
    }

    val onWordPress: (String) -> Unit = { word ->
        val trimmed = word.trim()
        val definition = dictionaryDbHelper?.getWordDefinition(trimmed) ?: "Definition not found."
        currentWord = trimmed
        wordDefinition = definition
        showWordModal = true
    }

    val onStrongsPress: (String, Int) -> Unit = { strongNumber, bookNumber ->
        val trimmed = strongNumber.trim()
        val isOldTestament = bookNumber <= 39
        val prefixed = if (trimmed.firstOrNull()?.isLetter() ?: false) {
            trimmed
        } else {
            (if (isOldTestament) "H" else "G") + trimmed
        }
        val definition = strongDbHelper?.getStrongDefinition(prefixed) ?: "Strong's definition not found."
        currentStrongNumber = prefixed
        strongDefinition = definition
        showStrongsModal = true
    }

    val onTagPress: (String, Int, Int, Int, Boolean) -> Unit = { marker, bookNumber, chapter, verseNumber, isPrimary ->
        val dbHelper = if (isPrimary) commentaryDbHelper else secondaryCommentaryDbHelper
        var text = "No commentary found."
        text = try {
            dbHelper?.getCommentary(bookNumber, chapter, verseNumber, marker) ?: text
        } catch (e: Exception) {
            "Error loading commentary: ${e.message}"
        }
        commentaryTitle = "Commentary for ${passage.bookName} $chapter:$verseNumber $marker"
        commentaryContent = text
        showCommentaryModal = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (!multi) {
            SingleVersionReader(
                primaryCurrent = primaryCurrent,
                targetVerse = targetVerse,
                databaseHelper = databaseHelper,
                primaryLoadedVerses = primaryLoadedVerses,
                themeColors = themeColors,
                currentFontFamily = currentFontFamily,
                viewModel = viewModel,
                onPassageChange = { newPassage ->
                    primaryCurrent = newPassage
                    targetVerse = newPassage.verse
                    onPassageChange(newPassage)
                },
                scheduleFade = ::scheduleFade,
                onWordPress = onWordPress,
                onStrongsPress = onStrongsPress,
                onTagPress = onTagPress
            )
        } else if (synced) {
            SyncedMultiVersionReader(
                primaryCurrent = primaryCurrent,
                targetVerse = targetVerse,
                databaseHelper = databaseHelper,
                secondaryDatabaseHelper = secondaryDatabaseHelper,
                primaryLoadedVerses = primaryLoadedVerses,
                secondaryLoadedVerses = secondaryLoadedVerses,
                themeColors = themeColors,
                currentFontFamily = currentFontFamily,
                viewModel = viewModel,
                onPassageChange = { newPassage ->
                    primaryCurrent = newPassage
                    secondaryCurrent = newPassage
                    targetVerse = newPassage.verse
                    onPassageChange(newPassage)
                    viewModel.secondaryPassage = newPassage
                },
                scheduleFade = ::scheduleFade,
                onWordPress = onWordPress,
                onStrongsPress = onStrongsPress,
                onTagPress = onTagPress
            )
        } else {
            IndependentMultiVersionReader(
                primaryCurrent = primaryCurrent,
                secondaryCurrent = secondaryCurrent,
                targetVerse = targetVerse,
                secondaryTargetVerse = secondaryTargetVerse,
                databaseHelper = databaseHelper,
                secondaryDatabaseHelper = secondaryDatabaseHelper,
                primaryLoadedVerses = primaryLoadedVerses,
                secondaryLoadedVerses = secondaryLoadedVerses,
                themeColors = themeColors,
                currentFontFamily = currentFontFamily,
                viewModel = viewModel,
                onPrimaryPassageChange = { newPassage ->
                    primaryCurrent = newPassage
                    targetVerse = newPassage.verse
                    onPassageChange(newPassage)
                },
                onSecondaryPassageChange = { newPassage ->
                    secondaryCurrent = newPassage
                    secondaryTargetVerse = newPassage.verse
                    viewModel.secondaryPassage = newPassage
                },
                scheduleFade = ::scheduleFade,
                onWordPress = onWordPress,
                onStrongsPress = onStrongsPress,
                onTagPress = onTagPress
            )
        }

        FloatingActionButton(
            onClick = {
                viewModel.isReaderFullScreen = !viewModel.isReaderFullScreen
                isButtonVisible = true
                scheduleFade()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(50.dp)
                .alpha(buttonAlpha),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (viewModel.isReaderFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = if (viewModel.isReaderFullScreen) "Exit Fullscreen" else "Enter Fullscreen"
            )
        }

        DefinitionModal(
            show = showWordModal,
            onDismiss = { showWordModal = false },
            word = currentWord,
            definition = wordDefinition,
            selectedDictionary = viewModel.selectedDictionary,
            onSwitch = {
                viewModel.selectedDictionary = if (viewModel.selectedDictionary == "noah") "atsbd" else "noah"
                wordDefinition = dictionaryDbHelper?.getWordDefinition(currentWord) ?: "Definition not found."
            }
        )

        StrongsModal(
            show = showStrongsModal,
            onDismiss = { showStrongsModal = false },
            strongNumber = currentStrongNumber,
            definition = strongDefinition
        )

        CommentaryModal(
            show = showCommentaryModal,
            onDismiss = { showCommentaryModal = false },
            title = commentaryTitle,
            content = commentaryContent
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SingleVersionReader(
    primaryCurrent: PassageSelection,
    targetVerse: Int,
    databaseHelper: DatabaseHelper?,
    primaryLoadedVerses: MutableMap<Pair<Int, Int>, List<Verse>>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    onPassageChange: (PassageSelection) -> Unit,
    scheduleFade: () -> Unit,
    onWordPress: (String) -> Unit,
    onStrongsPress: (String, Int) -> Unit,
    onTagPress: (String, Int, Int, Int, Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBook by remember(primaryCurrent.bookNumber) { derivedStateOf { BibleData.getBookByCustomNumber(primaryCurrent.bookNumber) } }
    val prevPassage by remember(primaryCurrent, currentBook) { derivedStateOf { if (currentBook == null) primaryCurrent else getPreviousPassage(primaryCurrent, currentBook) } }
    val nextPassage by remember(primaryCurrent, currentBook) { derivedStateOf { if (currentBook == null) primaryCurrent else getNextPassage(primaryCurrent, currentBook) } }
    val hasPrev by remember(prevPassage) { derivedStateOf { prevPassage != primaryCurrent } }
    val hasNext by remember(nextPassage) { derivedStateOf { nextPassage != primaryCurrent } }
    val passages by remember(primaryCurrent, prevPassage, nextPassage, hasPrev, hasNext) { derivedStateOf { buildList { if (hasPrev) add(prevPassage); add(primaryCurrent); if (hasNext) add(nextPassage) } } }
    val pageCount by remember(passages) { derivedStateOf { passages.size } }
    val currentOffset by remember(hasPrev) { derivedStateOf { if (hasPrev) 1 else 0 } }
    var pendingPassageChange by remember { mutableStateOf<PassageSelection?>(null) }

    LaunchedEffect(primaryCurrent, hasPrev, hasNext, databaseHelper) {
        val currentKey = primaryCurrent.bookNumber to primaryCurrent.chapter
        if (currentKey !in primaryLoadedVerses) {
            primaryLoadedVerses[currentKey] = databaseHelper?.getVerses(primaryCurrent.bookNumber, primaryCurrent.chapter) ?: emptyList()
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
    }

    val pagerState = rememberPagerState(
        initialPage = currentOffset,
        pageCount = { pageCount }
    )

    var isUserSwiping by remember { mutableStateOf(false) }
    var swipeCompleted by remember { mutableStateOf(false) }

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
            isUserSwiping = false
            val targetPassage = pendingPassageChange
            if (targetPassage != null && !swipeCompleted) {
                swipeCompleted = true
                onPassageChange(targetPassage)
                pendingPassageChange = null
            } else {
                coroutineScope.launch {
                    val offset = if (hasPrev) 1 else 0
                    pagerState.scrollToPage(offset)
                }
            }
        }
    }

    LaunchedEffect(primaryCurrent) {
        if (!isUserSwiping) {
            coroutineScope.launch {
                delay(50)
                val offset = if (hasPrev) 1 else 0
                pagerState.scrollToPage(offset)
                pendingPassageChange = null
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    scheduleFade()
                }
            },
        key = { pageIndex ->
            val passageKey = passages[pageIndex]
            "${passageKey.bookNumber}-${passageKey.chapter}"
        }
    ) { pageIndex ->
        val thisPassage = passages[pageIndex]
        val primaryVerses = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
        val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

        Box(modifier = Modifier.fillMaxSize()) {
            if (primaryVerses.isEmpty()) {
                LoadingIndicator()
            } else {
                val primaryState = rememberScrollState()
                ChapterView(
                    passage = thisPassage,
                    verses = primaryVerses,
                    themeColors = themeColors,
                    currentFontFamily = currentFontFamily,
                    viewModel = viewModel,
                    isCurrentPage = isCurrentPage,
                    targetVerse = targetVerse,
                    versionAbbr = viewModel.currentVersionAbbr,
                    isPrimary = true,
                    state = primaryState,
                    modifier = Modifier.fillMaxSize(),
                    isKjvPlus = databaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                    onWordPress = onWordPress,
                    onStrongsPress = onStrongsPress,
                    onTagPress = onTagPress
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SyncedMultiVersionReader(
    primaryCurrent: PassageSelection,
    targetVerse: Int,
    databaseHelper: DatabaseHelper?,
    secondaryDatabaseHelper: DatabaseHelper?,
    primaryLoadedVerses: MutableMap<Pair<Int, Int>, List<Verse>>,
    secondaryLoadedVerses: MutableMap<Pair<Int, Int>, List<Verse>>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    onPassageChange: (PassageSelection) -> Unit,
    scheduleFade: () -> Unit,
    onWordPress: (String) -> Unit,
    onStrongsPress: (String, Int) -> Unit,
    onTagPress: (String, Int, Int, Int, Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBook by remember(primaryCurrent.bookNumber) { derivedStateOf { BibleData.getBookByCustomNumber(primaryCurrent.bookNumber) } }
    val prevPassage by remember(primaryCurrent, currentBook) { derivedStateOf { if (currentBook == null) primaryCurrent else getPreviousPassage(primaryCurrent, currentBook) } }
    val nextPassage by remember(primaryCurrent, currentBook) { derivedStateOf { if (currentBook == null) primaryCurrent else getNextPassage(primaryCurrent, currentBook) } }
    val hasPrev by remember(prevPassage) { derivedStateOf { prevPassage != primaryCurrent } }
    val hasNext by remember(nextPassage) { derivedStateOf { nextPassage != primaryCurrent } }
    val passages by remember(primaryCurrent, prevPassage, nextPassage, hasPrev, hasNext) { derivedStateOf { buildList { if (hasPrev) add(prevPassage); add(primaryCurrent); if (hasNext) add(nextPassage) } } }
    val pageCount by remember(passages) { derivedStateOf { passages.size } }
    val currentOffset by remember(hasPrev) { derivedStateOf { if (hasPrev) 1 else 0 } }
    var pendingPassageChange by remember { mutableStateOf<PassageSelection?>(null) }

    LaunchedEffect(primaryCurrent, hasPrev, hasNext, databaseHelper, secondaryDatabaseHelper) {
        val currentKey = primaryCurrent.bookNumber to primaryCurrent.chapter
        if (currentKey !in primaryLoadedVerses) {
            primaryLoadedVerses[currentKey] = databaseHelper?.getVerses(primaryCurrent.bookNumber, primaryCurrent.chapter) ?: emptyList()
        }
        if (currentKey !in secondaryLoadedVerses) {
            secondaryLoadedVerses[currentKey] = secondaryDatabaseHelper?.getVerses(primaryCurrent.bookNumber, primaryCurrent.chapter) ?: emptyList()
        }
        if (hasPrev) {
            val prevKey = prevPassage.bookNumber to prevPassage.chapter
            if (prevKey !in primaryLoadedVerses) {
                primaryLoadedVerses[prevKey] = databaseHelper?.getVerses(prevPassage.bookNumber, prevPassage.chapter) ?: emptyList()
            }
            if (prevKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[prevKey] = secondaryDatabaseHelper?.getVerses(prevPassage.bookNumber, prevPassage.chapter) ?: emptyList()
            }
        }
        if (hasNext) {
            val nextKey = nextPassage.bookNumber to nextPassage.chapter
            if (nextKey !in primaryLoadedVerses) {
                primaryLoadedVerses[nextKey] = databaseHelper?.getVerses(nextPassage.bookNumber, nextPassage.chapter) ?: emptyList()
            }
            if (nextKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[nextKey] = secondaryDatabaseHelper?.getVerses(nextPassage.bookNumber, nextPassage.chapter) ?: emptyList()
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = currentOffset,
        pageCount = { pageCount }
    )

    var isUserSwiping by remember { mutableStateOf(false) }
    var swipeCompleted by remember { mutableStateOf(false) }

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
            isUserSwiping = false
            val targetPassage = pendingPassageChange
            if (targetPassage != null && !swipeCompleted) {
                swipeCompleted = true
                onPassageChange(targetPassage)
                pendingPassageChange = null
            } else {
                coroutineScope.launch {
                    val offset = if (hasPrev) 1 else 0
                    pagerState.scrollToPage(offset)
                }
            }
        }
    }

    LaunchedEffect(primaryCurrent) {
        if (!isUserSwiping) {
            coroutineScope.launch {
                delay(50)
                val offset = if (hasPrev) 1 else 0
                pagerState.scrollToPage(offset)
                pendingPassageChange = null
            }
        }
    }

    var suppressSync by remember { mutableStateOf(false) }
    var completedScrolls by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetVerse) {
        suppressSync = true
        completedScrolls = 0
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    scheduleFade()
                }
            },
        key = { pageIndex ->
            val passageKey = passages[pageIndex]
            "${passageKey.bookNumber}-${passageKey.chapter}"
        }
    ) { pageIndex ->
        val thisPassage = passages[pageIndex]
        val primaryVerses = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
        val secondaryVerses = secondaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
        val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

        Box(modifier = Modifier.fillMaxSize()) {
            if (primaryVerses.isEmpty() || secondaryVerses.isEmpty()) {
                LoadingIndicator()
            } else {
                val primaryState = rememberScrollState()
                val secondaryState = rememberScrollState()

                if (viewModel.scrollSync && !suppressSync) {
                    LaunchedEffect(primaryState) {
                        snapshotFlow { primaryState.value }.collect { _ ->
                            val pMax = primaryState.maxValue.coerceAtLeast(1)
                            val sMax = secondaryState.maxValue.coerceAtLeast(1)
                            val fraction = primaryState.value.toFloat() / pMax
                            val targetS = (fraction * sMax).roundToInt()
                            val currentS = secondaryState.value
                            val deltaS = targetS - currentS
                            if (abs(deltaS) > 5) {
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
                            if (abs(deltaP) > 5) {
                                primaryState.scrollBy(deltaP.toFloat())
                            }
                        }
                    }
                }

                if (viewModel.multiViewLayout == "horizontal") {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ChapterView(
                            passage = thisPassage,
                            verses = primaryVerses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = targetVerse,
                            versionAbbr = viewModel.currentVersionAbbr,
                            isPrimary = true,
                            state = primaryState,
                            modifier = Modifier.weight(1f),
                            isKjvPlus = databaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onInitialScrollComplete = {
                                completedScrolls++
                                if (completedScrolls == 2) {
                                    suppressSync = false
                                    completedScrolls = 0
                                }
                            },
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                        ChapterView(
                            passage = thisPassage,
                            verses = secondaryVerses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = targetVerse,
                            versionAbbr = viewModel.secondaryVersionAbbr,
                            isPrimary = false,
                            state = secondaryState,
                            modifier = Modifier.weight(1f),
                            isKjvPlus = secondaryDatabaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onInitialScrollComplete = {
                                completedScrolls++
                                if (completedScrolls == 2) {
                                    suppressSync = false
                                    completedScrolls = 0
                                }
                            },
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChapterView(
                            passage = thisPassage,
                            verses = primaryVerses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = targetVerse,
                            versionAbbr = viewModel.currentVersionAbbr,
                            isPrimary = true,
                            state = primaryState,
                            modifier = Modifier.weight(1f),
                            isKjvPlus = databaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onInitialScrollComplete = {
                                completedScrolls++
                                if (completedScrolls == 2) {
                                    suppressSync = false
                                    completedScrolls = 0
                                }
                            },
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                        ChapterView(
                            passage = thisPassage,
                            verses = secondaryVerses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = targetVerse,
                            versionAbbr = viewModel.secondaryVersionAbbr,
                            isPrimary = false,
                            state = secondaryState,
                            modifier = Modifier.weight(1f),
                            isKjvPlus = secondaryDatabaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onInitialScrollComplete = {
                                completedScrolls++
                                if (completedScrolls == 2) {
                                    suppressSync = false
                                    completedScrolls = 0
                                }
                            },
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IndependentMultiVersionReader(
    primaryCurrent: PassageSelection,
    secondaryCurrent: PassageSelection,
    targetVerse: Int,
    secondaryTargetVerse: Int,
    databaseHelper: DatabaseHelper?,
    secondaryDatabaseHelper: DatabaseHelper?,
    primaryLoadedVerses: MutableMap<Pair<Int, Int>, List<Verse>>,
    secondaryLoadedVerses: MutableMap<Pair<Int, Int>, List<Verse>>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    onPrimaryPassageChange: (PassageSelection) -> Unit,
    onSecondaryPassageChange: (PassageSelection) -> Unit,
    scheduleFade: () -> Unit,
    onWordPress: (String) -> Unit,
    onStrongsPress: (String, Int) -> Unit,
    onTagPress: (String, Int, Int, Int, Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val primaryBook by remember(primaryCurrent.bookNumber) { derivedStateOf { BibleData.getBookByCustomNumber(primaryCurrent.bookNumber) } }
    val primaryPrev by remember(primaryCurrent, primaryBook) { derivedStateOf { if (primaryBook == null) primaryCurrent else getPreviousPassage(primaryCurrent, primaryBook) } }
    val primaryNext by remember(primaryCurrent, primaryBook) { derivedStateOf { if (primaryBook == null) primaryCurrent else getNextPassage(primaryCurrent, primaryBook) } }
    val primaryHasPrev by remember(primaryPrev) { derivedStateOf { primaryPrev != primaryCurrent } }
    val primaryHasNext by remember(primaryNext) { derivedStateOf { primaryNext != primaryCurrent } }
    val primaryPassages by remember(primaryCurrent, primaryPrev, primaryNext, primaryHasPrev, primaryHasNext) { derivedStateOf { buildList { if (primaryHasPrev) add(primaryPrev); add(primaryCurrent); if (primaryHasNext) add(primaryNext) } } }
    val primaryPageCount by remember(primaryPassages) { derivedStateOf { primaryPassages.size } }
    val primaryOffset by remember(primaryHasPrev) { derivedStateOf { if (primaryHasPrev) 1 else 0 } }
    var primaryPendingChange by remember { mutableStateOf<PassageSelection?>(null) }

    val secondaryBook by remember(secondaryCurrent.bookNumber) { derivedStateOf { BibleData.getBookByCustomNumber(secondaryCurrent.bookNumber) } }
    val secondaryPrev by remember(secondaryCurrent, secondaryBook) { derivedStateOf { if (secondaryBook == null) secondaryCurrent else getPreviousPassage(secondaryCurrent, secondaryBook) } }
    val secondaryNext by remember(secondaryCurrent, secondaryBook) { derivedStateOf { if (secondaryBook == null) secondaryCurrent else getNextPassage(secondaryCurrent, secondaryBook) } }
    val secondaryHasPrev by remember(secondaryPrev) { derivedStateOf { secondaryPrev != secondaryCurrent } }
    val secondaryHasNext by remember(secondaryNext) { derivedStateOf { secondaryNext != secondaryCurrent } }
    val secondaryPassages by remember(secondaryCurrent, secondaryPrev, secondaryNext, secondaryHasPrev, secondaryHasNext) { derivedStateOf { buildList { if (secondaryHasPrev) add(secondaryPrev); add(secondaryCurrent); if (secondaryHasNext) add(secondaryNext) } } }
    val secondaryPageCount by remember(secondaryPassages) { derivedStateOf { secondaryPassages.size } }
    val secondaryOffset by remember(secondaryHasPrev) { derivedStateOf { if (secondaryHasPrev) 1 else 0 } }
    var secondaryPendingChange by remember { mutableStateOf<PassageSelection?>(null) }

    LaunchedEffect(primaryCurrent, primaryHasPrev, primaryHasNext, databaseHelper) {
        val currentKey = primaryCurrent.bookNumber to primaryCurrent.chapter
        if (currentKey !in primaryLoadedVerses) {
            primaryLoadedVerses[currentKey] = databaseHelper?.getVerses(primaryCurrent.bookNumber, primaryCurrent.chapter) ?: emptyList()
        }
        if (primaryHasPrev) {
            val prevKey = primaryPrev.bookNumber to primaryPrev.chapter
            if (prevKey !in primaryLoadedVerses) {
                primaryLoadedVerses[prevKey] = databaseHelper?.getVerses(primaryPrev.bookNumber, primaryPrev.chapter) ?: emptyList()
            }
        }
        if (primaryHasNext) {
            val nextKey = primaryNext.bookNumber to primaryNext.chapter
            if (nextKey !in primaryLoadedVerses) {
                primaryLoadedVerses[nextKey] = databaseHelper?.getVerses(primaryNext.bookNumber, primaryNext.chapter) ?: emptyList()
            }
        }
    }

    LaunchedEffect(secondaryCurrent, secondaryHasPrev, secondaryHasNext, secondaryDatabaseHelper) {
        val currentKey = secondaryCurrent.bookNumber to secondaryCurrent.chapter
        if (currentKey !in secondaryLoadedVerses) {
            secondaryLoadedVerses[currentKey] = secondaryDatabaseHelper?.getVerses(secondaryCurrent.bookNumber, secondaryCurrent.chapter) ?: emptyList()
        }
        if (secondaryHasPrev) {
            val prevKey = secondaryPrev.bookNumber to secondaryPrev.chapter
            if (prevKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[prevKey] = secondaryDatabaseHelper?.getVerses(secondaryPrev.bookNumber, secondaryPrev.chapter) ?: emptyList()
            }
        }
        if (secondaryHasNext) {
            val nextKey = secondaryNext.bookNumber to secondaryNext.chapter
            if (nextKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[nextKey] = secondaryDatabaseHelper?.getVerses(secondaryNext.bookNumber, secondaryNext.chapter) ?: emptyList()
            }
        }
    }

    val primaryPagerState = rememberPagerState(
        initialPage = primaryOffset,
        pageCount = { primaryPageCount }
    )

    val secondaryPagerState = rememberPagerState(
        initialPage = secondaryOffset,
        pageCount = { secondaryPageCount }
    )

    var primarySwiping by remember { mutableStateOf(false) }
    var primarySwipeCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(primaryPagerState.currentPage, primaryPagerState.isScrollInProgress) {
        if (primaryPagerState.isScrollInProgress) {
            primarySwiping = true
            primarySwipeCompleted = false
            val offset = if (primaryHasPrev) 1 else 0
            if (primaryPagerState.currentPage < offset) {
                if (primaryHasPrev) primaryPendingChange = primaryPrev
            } else if (primaryPagerState.currentPage > offset) {
                if (primaryHasNext) primaryPendingChange = primaryNext
            }
        } else if (primarySwiping) {
            primarySwiping = false
            val target = primaryPendingChange
            if (target != null && !primarySwipeCompleted) {
                primarySwipeCompleted = true
                onPrimaryPassageChange(target)
                primaryPendingChange = null
            } else {
                coroutineScope.launch {
                    val offset = if (primaryHasPrev) 1 else 0
                    primaryPagerState.scrollToPage(offset)
                }
            }
        }
    }

    LaunchedEffect(primaryCurrent) {
        if (!primarySwiping) {
            coroutineScope.launch {
                delay(50)
                val offset = if (primaryHasPrev) 1 else 0
                primaryPagerState.scrollToPage(offset)
                primaryPendingChange = null
            }
        }
    }

    var secondarySwiping by remember { mutableStateOf(false) }
    var secondarySwipeCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(secondaryPagerState.currentPage, secondaryPagerState.isScrollInProgress) {
        if (secondaryPagerState.isScrollInProgress) {
            secondarySwiping = true
            secondarySwipeCompleted = false
            val offset = if (secondaryHasPrev) 1 else 0
            if (secondaryPagerState.currentPage < offset) {
                if (secondaryHasPrev) secondaryPendingChange = secondaryPrev
            } else if (secondaryPagerState.currentPage > offset) {
                if (secondaryHasNext) secondaryPendingChange = secondaryNext
            }
        } else if (secondarySwiping) {
            secondarySwiping = false
            val target = secondaryPendingChange
            if (target != null && !secondarySwipeCompleted) {
                secondarySwipeCompleted = true
                onSecondaryPassageChange(target)
                secondaryPendingChange = null
            } else {
                coroutineScope.launch {
                    val offset = if (secondaryHasPrev) 1 else 0
                    secondaryPagerState.scrollToPage(offset)
                }
            }
        }
    }

    LaunchedEffect(secondaryCurrent) {
        if (!secondarySwiping) {
            coroutineScope.launch {
                delay(50)
                val offset = if (secondaryHasPrev) 1 else 0
                secondaryPagerState.scrollToPage(offset)
                secondaryPendingChange = null
            }
        }
    }

    val layoutHorizontal = viewModel.multiViewLayout == "horizontal"
    val containerModifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { scheduleFade() }
    }

    if (layoutHorizontal) {
        Row(modifier = containerModifier) {
            HorizontalPager(
                state = primaryPagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex ->
                    val pk = primaryPassages[pageIndex]
                    "${pk.bookNumber}-${pk.chapter}-primary"
                }
            ) { pageIndex ->
                val thisPassage = primaryPassages[pageIndex]
                val verses = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (verses.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberScrollState()
                        ChapterView(
                            passage = thisPassage,
                            verses = verses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = targetVerse,
                            versionAbbr = viewModel.currentVersionAbbr,
                            isPrimary = true,
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            isKjvPlus = databaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                    }
                }
            }
            HorizontalPager(
                state = secondaryPagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex ->
                    val pk = secondaryPassages[pageIndex]
                    "${pk.bookNumber}-${pk.chapter}-secondary"
                }
            ) { pageIndex ->
                val thisPassage = secondaryPassages[pageIndex]
                val verses = secondaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == secondaryCurrent.bookNumber && thisPassage.chapter == secondaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (verses.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberScrollState()
                        ChapterView(
                            passage = thisPassage,
                            verses = verses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = secondaryTargetVerse,
                            versionAbbr = viewModel.secondaryVersionAbbr,
                            isPrimary = false,
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            isKjvPlus = secondaryDatabaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = containerModifier) {
            HorizontalPager(
                state = primaryPagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex ->
                    val pk = primaryPassages[pageIndex]
                    "${pk.bookNumber}-${pk.chapter}-primary"
                }
            ) { pageIndex ->
                val thisPassage = primaryPassages[pageIndex]
                val verses = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (verses.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberScrollState()
                        ChapterView(
                            passage = thisPassage,
                            verses = verses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = targetVerse,
                            versionAbbr = viewModel.currentVersionAbbr,
                            isPrimary = true,
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            isKjvPlus = databaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                    }
                }
            }
            HorizontalPager(
                state = secondaryPagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex ->
                    val pk = secondaryPassages[pageIndex]
                    "${pk.bookNumber}-${pk.chapter}-secondary"
                }
            ) { pageIndex ->
                val thisPassage = secondaryPassages[pageIndex]
                val verses = secondaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == secondaryCurrent.bookNumber && thisPassage.chapter == secondaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (verses.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberScrollState()
                        ChapterView(
                            passage = thisPassage,
                            verses = verses,
                            themeColors = themeColors,
                            currentFontFamily = currentFontFamily,
                            viewModel = viewModel,
                            isCurrentPage = isCurrentPage,
                            targetVerse = secondaryTargetVerse,
                            versionAbbr = viewModel.secondaryVersionAbbr,
                            isPrimary = false,
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            isKjvPlus = secondaryDatabaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false,
                            onWordPress = onWordPress,
                            onStrongsPress = onStrongsPress,
                            onTagPress = onTagPress
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading verses...")
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
    isPrimary: Boolean,
    state: ScrollState,
    modifier: Modifier = Modifier,
    isKjvPlus: Boolean = false,
    onInitialScrollComplete: () -> Unit = {},
    onWordPress: ((String) -> Unit)? = null,
    onStrongsPress: ((String, Int) -> Unit)? = null,
    onTagPress: ((String, Int, Int, Int, Boolean) -> Unit)? = null
) {
    val processor = remember(verses) { VerseTextProcessor() }
    val processedVerses = remember(verses, themeColors, isKjvPlus, onWordPress) {
        val result = mutableMapOf<Int, ProcessedVerse>()
        for (verse in verses) {
            val onStrongsLocal = if (onStrongsPress != null) {
                { strongNumber: String -> onStrongsPress.invoke(strongNumber, passage.bookNumber) }
            } else null
            val onTagLocal = if (onTagPress != null) {
                { marker: String -> onTagPress.invoke(marker, passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary) }
            } else null
            val processed = processor.processVerse(
                verseText = verse.text,
                baseFontSize = viewModel.fontSize.sp,
                themeColors = themeColors,
                textColor = themeColors.textColor,
                onTagPress = onTagLocal,
                onWordPress = onWordPress,
                onStrongsPress = onStrongsLocal,
                isHighlighted = false,
                isKjvPlus = isKjvPlus,
                isOldTestament = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
            )
            result[verse.verseNumber] = processed
        }
        result
    }

    var highlightedVerse by remember { mutableStateOf<Int?>(null) }
    val offsets = remember { mutableStateMapOf<Int, Float>() }

    Box(modifier = modifier) {
        val texture = if (viewModel.bgImageIndex == 0) null else if (viewModel.bgImageIndex == 34 && viewModel.customTextureUri != null) viewModel.customTextureUri else "file:///android_asset/textures/${viewModel.bgImageIndex}.jpg"
        if (texture != null) {
            AsyncImage(
                model = texture,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            val overlayColor = if (viewModel.darkTheme) Color(0xFF301934).copy(alpha = viewModel.overlayOpacity) else Color(0xFFF5F5DC).copy(alpha = viewModel.overlayOpacity)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }

        Column {
            if (viewModel.multiVersion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.primary)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (viewModel.scrollSync || isPrimary) {
                                viewModel.showNavigationModal = true
                            } else {
                                viewModel.showSecondaryNavigationModal = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = passage.bookName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = passage.chapter.let { " $it" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (isPrimary) {
                                viewModel.showPrimaryVersionDropdown = true
                            } else {
                                viewModel.showSecondaryVersionDropdown = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = versionAbbr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(state)
                    .padding(12.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                verses.forEach { verse ->
                    val processedVerse = processedVerses[verse.verseNumber]
                    val isHighlighted = verse.verseNumber == highlightedVerse
                    if (processedVerse != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .then(if (isHighlighted) Modifier.background(themeColors.searchHighlightBg) else Modifier)
                                .onGloballyPositioned { coords ->
                                    offsets[verse.verseNumber] = coords.positionInParent().y
                                }
                        ) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val annotatedString = buildAnnotatedString {
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
                                }
                                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                Text(
                                    text = annotatedString,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset: Offset ->
                                                textLayoutResult?.let { layout ->
                                                    val position = layout.getOffsetForPosition(offset)
                                                    val annotations = annotatedString.getStringAnnotations(start = position, end = position)
                                                    annotations.forEach { annotation ->
                                                        when (annotation.tag) {
                                                            "word" -> onWordPress?.invoke(annotation.item)
                                                            "strong" -> onStrongsPress?.invoke(annotation.item, passage.bookNumber)
                                                            "tag" -> onTagPress?.invoke(annotation.item, passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    fontSize = viewModel.fontSize.sp,
                                    lineHeight = (viewModel.fontSize * 1.333f).sp,
                                    fontFamily = currentFontFamily,
                                    onTextLayout = { textLayoutResult = it }
                                )
                            }
                        }
                    } else {
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
                if (isCurrentPage) {
                    LaunchedEffect(targetVerse, verses) {
                        targetVerse?.let { v ->
                            if (verses.isNotEmpty() && v > 0) {
                                delay(200)
                                val offset = offsets[v] ?: return@let
                                state.animateScrollTo(offset.toInt())
                                highlightedVerse = targetVerse
                                delay(2000)
                                highlightedVerse = null
                                onInitialScrollComplete()
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
        current.copy(chapter = currentBook.chapters, verse = 1)
    } else {
        current.copy(chapter = current.chapter - 1, verse = 1)
    }
}

fun getNextPassage(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters <= 2 && current.chapter == currentBook.chapters) return current
    return if (current.chapter == currentBook.chapters) {
        current.copy(chapter = 1, verse = 1)
    } else {
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