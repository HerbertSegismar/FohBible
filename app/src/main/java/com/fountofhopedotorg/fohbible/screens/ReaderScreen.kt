package com.fountofhopedotorg.fohbible.screens

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.composables.IndependentMultiVersionReader
import com.fountofhopedotorg.fohbible.composables.SingleVersionReader
import com.fountofhopedotorg.fohbible.composables.SyncedMultiVersionReader
import com.fountofhopedotorg.fohbible.core.createCommentaryHelperIfExists
import com.fountofhopedotorg.fohbible.data.*
import com.fountofhopedotorg.fohbible.modals.InteractiveModal
import com.fountofhopedotorg.fohbible.modals.NotesModal
import com.fountofhopedotorg.fohbible.modals.VerseOptionsModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class ReaderModalState {
    object None : ReaderModalState()
    data class Word(
        val word: String,
        val definition: String,
        val dbHelper: DatabaseHelper?,
        val isOldTestament: Boolean,
        val useSecondaryDictionary: Boolean
    ) : ReaderModalState()
    data class Strong(
        val strongNumber: String,
        val definition: String,
        val dbHelper: DatabaseHelper?,
        val isOldTestament: Boolean
    ) : ReaderModalState()
    data class Commentary(
        val title: String,
        val content: String,
        val dbHelper: DatabaseHelper?,
        val isOldTestament: Boolean
    ) : ReaderModalState()
    data class CrossRef(
        val source: String,
        val content: String,
        val dbHelper: DatabaseHelper?,
        val book: Int,
        val chapter: Int,
        val verse: Int,
        val isOldTestament: Boolean
    ) : ReaderModalState()
    data class VerseCommentary(
        val book: Int,
        val chapter: Int,
        val verse: Int,
        val dbHelper: DatabaseHelper?,
        val isOldTestament: Boolean
    ) : ReaderModalState()
}

@Composable
fun ReaderScreen(
    passage: PassageSelection,
    databaseHelper: DatabaseHelper?,
    onPassageChange: (PassageSelection) -> Unit = {}
) {
    var verseCommentaryBook by remember { mutableIntStateOf(0) }
    var verseCommentaryChapter by remember { mutableIntStateOf(0) }
    var verseCommentaryVerse by remember { mutableIntStateOf(0) }
    var secondaryDictionaryModal by remember { mutableStateOf(false) }
    val viewModel = viewModel<AppViewModel>()
    val colorScheme = MaterialTheme.colorScheme
    val themeColors = remember(colorScheme, viewModel.darkTheme) {
        ThemeColors(
            textColor = colorScheme.onBackground,
            verseNumber = colorScheme.primary.copy(0.8f),
            primary = colorScheme.primary,
            tagColor = colorScheme.secondary,
            tagBg = colorScheme.secondary.copy(alpha = 0.1f),
            wordsOfJesus = viewModel.wordsOfJesus.copy(0.8f),
            searchHighlightBg = colorScheme.primary.copy(alpha = 0.3f),
            highlightIcon = colorScheme.primary
        )
    }
    var primaryCurrent by remember { mutableStateOf(passage.copy(verse = 1)) }
    var secondaryCurrent by remember { mutableStateOf(viewModel.secondaryPassage.copy(verse = 1)) }
    var targetVerse by remember { mutableStateOf(passage.verse) }
    var secondaryTargetVerse by remember { mutableStateOf(viewModel.secondaryPassage.verse) }
    var currentModalIsOldTestament by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }
    var selectedVersesForNote by remember { mutableStateOf(emptyList<Verse>()) }
    var crossRefHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    val contextFont = LocalContext.current
    LaunchedEffect(viewModel.selectedCrossReferenceDatabase) {
        crossRefHelper?.close()
        crossRefHelper = DatabaseHelper(contextFont, "${viewModel.selectedCrossReferenceDatabase}.crossreferences.sqlite3")
    }
    DisposableEffect(Unit) {
        onDispose { crossRefHelper?.close() }
    }
    var crossRefBook by remember { mutableIntStateOf(0) }
    var crossRefChapter by remember { mutableIntStateOf(0) }
    var crossRefVerse by remember { mutableIntStateOf(0) }
    LaunchedEffect(passage.bookNumber, passage.chapter, passage.verse) {
        if (passage.bookNumber != primaryCurrent.bookNumber || passage.chapter != primaryCurrent.chapter) {
            primaryCurrent = passage.copy(verse = 1)
            targetVerse = passage.verse
        }
        else {
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
    LaunchedEffect(viewModel.scrollSyncAction) {
        if (viewModel.scrollSyncAction) {
            delay(300)
            viewModel.scrollSyncAction = false
        }
    }

    val primaryLoadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<VerseContent>>() }
    val secondaryLoadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<VerseContent>>() }
    LaunchedEffect(databaseHelper) {
        primaryLoadedVerses.clear()
    }
    var secondaryDatabaseHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    LaunchedEffect(viewModel.multiVersion, viewModel.secondaryDbName) {
        secondaryDatabaseHelper?.close()
        secondaryDatabaseHelper = if (viewModel.multiVersion && viewModel.secondaryDbName.isNotEmpty()) {
            DatabaseHelper(contextFont as MainActivity, viewModel.secondaryDbName)
        } else { null }
    }
    LaunchedEffect(secondaryDatabaseHelper) {
        secondaryLoadedVerses.clear()
    }
    val multi = viewModel.multiVersion
    val synced = viewModel.scrollSync && multi && secondaryDatabaseHelper != null
    val currentFontFamily = getFontFamily(viewModel.selectedFontFamily)
    var isButtonVisible by remember { mutableStateOf(true) }
    val buttonAlpha by animateFloatAsState(if (isButtonVisible) 1f else 0.2f, label = "buttonAlpha")
    val scope = rememberCoroutineScope()
    var fadeJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleFade() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            delay(5000)
            isButtonVisible = false
        }
    }
    DisposableEffect(Unit) {
        onDispose { fadeJob?.cancel() }
    }

    LaunchedEffect(primaryCurrent) {
        isButtonVisible = true
        scheduleFade()
    }

    var isWordHighlightMode by remember { mutableStateOf(false) }
    var highlightJob by remember { mutableStateOf<Job?>(null) }

    fun activateWordHighlightMode() {
        isWordHighlightMode = true
        highlightJob?.cancel()
        highlightJob = scope.launch {
            delay(5000L)
            isWordHighlightMode = false
        }
    }
    var primaryDictionaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var secondaryDictionaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var strongDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var commentaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var secondaryCommentaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var modalState by remember { mutableStateOf<ReaderModalState>(ReaderModalState.None) }
    LaunchedEffect(viewModel.selectedPrimaryDictionary, databaseHelper?.databaseName, viewModel.selectedSecondaryDictionary) {
        primaryDictionaryDbHelper?.close()
        primaryDictionaryDbHelper = if (viewModel.selectedPrimaryDictionary.isNotBlank()) {
            DatabaseHelper(contextFont, "${viewModel.selectedPrimaryDictionary}.dictionary.sqlite3")
        } else null

        secondaryDictionaryDbHelper?.close()
        secondaryDictionaryDbHelper = if (viewModel.selectedSecondaryDictionary.isNotBlank()) {
            DatabaseHelper(contextFont, "${viewModel.selectedSecondaryDictionary}.dictionary.sqlite3")
        } else null

        strongDbHelper?.close()
        strongDbHelper = DatabaseHelper(contextFont, "secedictionary.sqlite3")

        commentaryDbHelper?.close()
        commentaryDbHelper = createCommentaryHelperIfExists(contextFont, databaseHelper?.databaseName)
    }

    LaunchedEffect(viewModel.multiVersion, viewModel.secondaryDbName, secondaryDatabaseHelper?.databaseName) {
        secondaryCommentaryDbHelper?.close()
        secondaryCommentaryDbHelper = createCommentaryHelperIfExists(contextFont, secondaryDatabaseHelper?.databaseName)
    }

    val onPrimaryWordPress: (String) -> Unit = { word ->
        secondaryDictionaryModal = false
        currentModalIsOldTestament = viewModel.isOldTestament
        val trimmed = word.trim()
        val definition = primaryDictionaryDbHelper?.getWordDefinition(trimmed)
            ?: "Definition not found."

        modalState = ReaderModalState.Word(
            word = trimmed,
            definition = definition,
            dbHelper = databaseHelper,
            isOldTestament = currentModalIsOldTestament,
            useSecondaryDictionary = secondaryDictionaryModal
        )
    }

    val onSecondaryWordPress: (String) -> Unit = { word ->
        secondaryDictionaryModal = true
        currentModalIsOldTestament = viewModel.isSecondaryOldTestament
        val trimmed = word.trim()
        val definition = secondaryDictionaryDbHelper?.getWordDefinition(trimmed)
            ?: primaryDictionaryDbHelper?.getWordDefinition(trimmed)
            ?: "Definition not found."

        modalState = ReaderModalState.Word(
            word = trimmed,
            definition = definition,
            dbHelper = secondaryDatabaseHelper ?: databaseHelper,
            isOldTestament = currentModalIsOldTestament,
            useSecondaryDictionary = secondaryDictionaryModal
        )
    }

    val onStrongsPress: (String, Int, Boolean) -> Unit = { strongNumber, _, isPrimary ->
        val isOldTestamentForVersion = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
        currentModalIsOldTestament = isOldTestamentForVersion
        val trimmed = strongNumber.trim()
        val prefixed = if (trimmed.firstOrNull()?.isLetter() ?: false) trimmed else (if (isOldTestamentForVersion) "H" else "G") + trimmed
        val definition = strongDbHelper?.getStrongDefinition(prefixed) ?: "Strong's definition not found."
        modalState = ReaderModalState.Strong(
            strongNumber = prefixed,
            definition = definition,
            dbHelper = if (isPrimary) databaseHelper else secondaryDatabaseHelper,
            isOldTestament = currentModalIsOldTestament
        )
    }

    val onTagPress: (String, Int, Int, Int, Boolean) -> Unit = { marker, bookNumber, chapter, verseNumber, isPrimary ->
        currentModalIsOldTestament = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
        val dbHelper = if (isPrimary) commentaryDbHelper else secondaryCommentaryDbHelper
        val text = dbHelper?.getCommentary(bookNumber, chapter, verseNumber, marker) ?: "No commentary found."
        val title = "Notes on ${BibleData.getBookByCustomNumber(bookNumber)?.name ?: ""} $chapter:$verseNumber$marker"
        modalState = ReaderModalState.Commentary(
            title = title,
            content = text,
            dbHelper = if (isPrimary) databaseHelper else secondaryDatabaseHelper,
            isOldTestament = currentModalIsOldTestament
        )
    }

    val onVerseCommentaryClick: (Int, Int, Int) -> Unit = { book, chap, verseNum ->
        verseCommentaryBook = book
        verseCommentaryChapter = chap
        verseCommentaryVerse = verseNum
        currentModalIsOldTestament = viewModel.isOldTestament
        modalState = ReaderModalState.VerseCommentary(
            book = book,
            chapter = chap,
            verse = verseNum,
            dbHelper = databaseHelper,
            isOldTestament = currentModalIsOldTestament
        )
    }

    var showMenu by remember { mutableStateOf(false) }
    var showBgModal by remember { mutableStateOf(false) }
    var showVerseOptions by remember { mutableStateOf(false) }
    var selectedVerse by remember { mutableStateOf<Verse?>(null) }
    var selectedPassage by remember { mutableStateOf<PassageSelection?>(null) }
    var selectedIsPrimary by remember { mutableStateOf(false) }

    val onVerseLongPress: (Verse, PassageSelection, Boolean) -> Unit = { verse, currentPassage, isPrimary ->
        selectedVerse = verse
        selectedPassage = currentPassage
        selectedIsPrimary = isPrimary
        showVerseOptions = true
    }

    var refreshKey by remember { mutableIntStateOf(0) }
    val subheadingsDbHelper = remember { DatabaseHelper(contextFont, "kjvsubheadings.sqlite3") }

    val onCrossRefClick: (Int, Int, Int, Boolean) -> Unit = { book, chapter, verse, isPrimary ->
        val dbForVerses = if (isPrimary) databaseHelper else secondaryDatabaseHelper
        val refs = crossRefHelper?.getCrossReferences(book, chapter, verse) ?: emptyList()
        val bookName = BibleData.getBookByCustomNumber(book)?.name ?: book.toString()
        val source = "References for $bookName $chapter:$verse"
        val htmlItems = refs.joinToString("<br>") { ref ->
            val toBook = BibleData.getBookByCustomNumber(ref.bookTo)?.name ?: ref.bookTo.toString()
            val verseRange = if (ref.verseToStart == ref.verseToEnd) {
                ref.verseToStart.toString()
            } else {
                "${ref.verseToStart}-${ref.verseToEnd}"
            }
            val href = "B:${ref.bookTo} ${ref.chapterTo}:$verseRange"
            "<a href=\"$href\">$toBook ${ref.chapterTo}:$verseRange</a>"
        }
        crossRefBook = book
        crossRefChapter = chapter
        crossRefVerse = verse
        currentModalIsOldTestament = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
        modalState = ReaderModalState.CrossRef(
            source = source,
            content = htmlItems,
            dbHelper = dbForVerses,
            book = book,
            chapter = chapter,
            verse = verse,
            isOldTestament = currentModalIsOldTestament
        )
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
                subheadingsDbHelper = subheadingsDbHelper,
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
                onWordPress = onPrimaryWordPress,
                onStrongsPress = onStrongsPress,
                onTagPress = onTagPress,
                onVerseLongPress = onVerseLongPress,
                onCrossRefClick = onCrossRefClick,
                crossRefHelper = crossRefHelper,
                refreshKey = refreshKey,
                onVerseCommentaryClick = onVerseCommentaryClick,
                markerColor = viewModel.wordMarkerColor,
                onWordHighlightAction = { activateWordHighlightMode() }
            )
        } else if (synced) {
            SyncedMultiVersionReader(
                primaryCurrent = primaryCurrent,
                targetVerse = targetVerse,
                databaseHelper = databaseHelper,
                secondaryDatabaseHelper = secondaryDatabaseHelper,
                subheadingsDbHelper = subheadingsDbHelper,
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
                onPrimaryWordPress = onPrimaryWordPress,
                onSecondaryWordPress = onSecondaryWordPress,
                onStrongsPress = onStrongsPress,
                onTagPress = onTagPress,
                onVerseLongPress = onVerseLongPress,
                onCrossRefClick = onCrossRefClick,
                crossRefHelper = crossRefHelper,
                refreshKey = refreshKey,
                onVerseCommentaryClick = onVerseCommentaryClick,
                markerColor = viewModel.wordMarkerColor,
                onWordHighlightAction = { activateWordHighlightMode() }
            )
        } else {
            IndependentMultiVersionReader(
                primaryCurrent = primaryCurrent,
                secondaryCurrent = secondaryCurrent,
                targetVerse = targetVerse,
                secondaryTargetVerse = secondaryTargetVerse,
                databaseHelper = databaseHelper,
                secondaryDatabaseHelper = secondaryDatabaseHelper,
                subheadingsDbHelper = subheadingsDbHelper,
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
                onPrimaryWordPress = onPrimaryWordPress,
                onSecondaryWordPress = onSecondaryWordPress,
                onStrongsPress = onStrongsPress,
                onTagPress = onTagPress,
                onVerseLongPress = onVerseLongPress,
                onCrossRefClick = onCrossRefClick,
                crossRefHelper = crossRefHelper,
                refreshKey = refreshKey,
                onVerseCommentaryClick = onVerseCommentaryClick,
                markerColor = viewModel.wordMarkerColor,
                onWordHighlightAction = { activateWordHighlightMode() }
            )
        }
        val fabModifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp)
        if (isWordHighlightMode) {
            Row(
                modifier = fabModifier
                    .background(
                        themeColors.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                viewModel.predefinedHighlightColors.forEach { color ->
                    val isSelected = color == viewModel.wordMarkerColor
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                viewModel.wordMarkerColor = color
                                activateWordHighlightMode()
                            }
                            .background(color, CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else Modifier
                            )
                    )
                }
            }
        } else {
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
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.align(Alignment.TopEnd),
            containerColor = if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor
        ) {
            DropdownMenuItem(
                text = { Text("Background Texture") },
                onClick = { showBgModal = true; showMenu = false }
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Overlay Opacity")
                Slider(
                    value = viewModel.overlayOpacity,
                    onValueChange = { viewModel.overlayOpacity = it },
                    valueRange = 0f..1f
                )
            }
        }

        when (val state = modalState) {
            is ReaderModalState.Word -> InteractiveModal(
                show = true,
                onDismiss = { modalState = ReaderModalState.None },
                onNavigateToReader = { p ->
                    viewModel.primaryPassage = p
                    viewModel.navigateTo(Screen.Reader(p))
                },
                databaseHelper = state.dbHelper,
                initialType = "definition",
                word = state.word,
                definition = state.definition,
                isOldTestament = state.isOldTestament,
                useSecondaryDictionary = state.useSecondaryDictionary
            )
            is ReaderModalState.Strong -> InteractiveModal(
                show = true,
                onDismiss = { modalState = ReaderModalState.None },
                onNavigateToReader = { p ->
                    viewModel.primaryPassage = p
                    viewModel.navigateTo(Screen.Reader(p))
                },
                databaseHelper = state.dbHelper,
                initialType = "strong",
                strongNumber = state.strongNumber,
                strongDefinition = state.definition,
                isOldTestament = state.isOldTestament
            )
            is ReaderModalState.Commentary -> InteractiveModal(
                show = true,
                onDismiss = { modalState = ReaderModalState.None },
                onNavigateToReader = { p ->
                    viewModel.primaryPassage = p
                    viewModel.navigateTo(Screen.Reader(p))
                },
                databaseHelper = state.dbHelper,
                initialType = "commentary",
                initialTitle = state.title,
                initialContent = state.content,
                isOldTestament = state.isOldTestament
            )
            is ReaderModalState.CrossRef -> InteractiveModal(
                show = true,
                onDismiss = { modalState = ReaderModalState.None },
                onNavigateToReader = { p ->
                    viewModel.primaryPassage = p
                    viewModel.navigateTo(Screen.Reader(p))
                },
                databaseHelper = state.dbHelper,
                initialType = "crossreference",
                initialTitle = state.source,
                initialContent = state.content,
                bookNumber = state.book,
                chapter = state.chapter,
                verse = state.verse,
                isOldTestament = state.isOldTestament
            )
            is ReaderModalState.VerseCommentary -> InteractiveModal(
                show = true,
                onDismiss = { modalState = ReaderModalState.None },
                onNavigateToReader = { p ->
                    viewModel.primaryPassage = p
                    viewModel.navigateTo(Screen.Reader(p))
                },
                databaseHelper = state.dbHelper,
                initialType = "versecommentary",
                bookNumber = state.book,
                chapter = state.chapter,
                verse = state.verse,
                isOldTestament = state.isOldTestament
            )
            ReaderModalState.None -> {}
        }

        val selVerse = selectedVerse
        val selPassage = selectedPassage
        val selIsPrimary = selectedIsPrimary
        if (showVerseOptions && selVerse != null && selPassage != null) {
            val db = if (selIsPrimary) databaseHelper else secondaryDatabaseHelper
            val loadedVerses = if (selIsPrimary) primaryLoadedVerses else secondaryLoadedVerses
            val key = selPassage.bookNumber to selPassage.chapter
            val content = loadedVerses[key] ?: emptyList()
            val chapterVerses = content.filterIsInstance<VerseContent.VerseVal>().map { it.verse }
            VerseOptionsModal(
                show = showVerseOptions,
                onDismiss = { showVerseOptions = false },
                passage = selPassage,
                verse = selVerse,
                chapterVerses = chapterVerses,
                databaseHelper = db,
                onAddBookmark = { refreshKey++ },
                onAddHighlight = { refreshKey++ },
                onShare = { selectedVerses ->
                    val verseNumbers = selectedVerses.map { it.verseNumber }
                    val minV = verseNumbers.minOrNull() ?: selVerse.verseNumber
                    val maxV = verseNumbers.maxOrNull() ?: selVerse.verseNumber
                    val rangeString = if (minV == maxV) "$minV" else "$minV-$maxV"
                    val contextLine = "${selPassage.bookName} ${selPassage.chapter}:$rangeString"
                    val shareText = buildString {
                        appendLine(contextLine)
                        selectedVerses.forEach {
                            appendLine("${it.verseNumber} ${SimpleVerseProcessor.stripXmlTags(it.text)}")
                        }
                    }.trimEnd()
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val chooserTitle = if (selectedVerses.size == 1) "Share verse" else "Share verses"
                    contextFont.startActivity(Intent.createChooser(shareIntent, chooserTitle))
                },
                onAddNote = { verses ->
                    selectedVersesForNote = verses
                    showNotesModal = true
                    showVerseOptions = false
                },
                appViewModel = viewModel
            )
        }
    }
    NotesModal(
        show = showNotesModal,
        onDismiss = { showNotesModal = false },
        verses = selectedVersesForNote,
        passage = passage,
        databaseHelper = databaseHelper,
        onSave = { refreshKey++ },
        appViewModel = viewModel
    )
}