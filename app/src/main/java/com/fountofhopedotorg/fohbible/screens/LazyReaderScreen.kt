package com.fountofhopedotorg.fohbible.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.data.BibleBook
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.ProcessedVerse
import com.fountofhopedotorg.fohbible.data.SelectedWord
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.VerseContent
import com.fountofhopedotorg.fohbible.data.getVersesWithSubheadings
import com.fountofhopedotorg.fohbible.modals.InteractiveModal
import com.fountofhopedotorg.fohbible.modals.NotesModal
import com.fountofhopedotorg.fohbible.modals.VerseOptionsModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun createCommentaryHelperIfExists(
    context: Context,
    baseDbName: String?
): DatabaseHelper? {
    if (baseDbName.isNullOrEmpty()) return null
    val comName = baseDbName.replace(".sqlite3", "com.sqlite3")
    val assetPath = "databases/$comName"
    return try {
        context.assets.open(assetPath).use { }
        DatabaseHelper(context, comName)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun LazyReaderScreen(
    passage: PassageSelection,
    databaseHelper: DatabaseHelper?,
    onPassageChange: (PassageSelection) -> Unit = {}
) {
    val viewModel = viewModel<AppViewModel>()
    val colorScheme = MaterialTheme.colorScheme
    val themeColors = remember(colorScheme, viewModel.darkTheme) {
        ThemeColors(
            textColor = colorScheme.onBackground,
            verseNumber = colorScheme.primary,
            primary = colorScheme.primary,
            tagColor = colorScheme.secondary,
            tagBg = colorScheme.secondary.copy(alpha = 0.1f),
            wordsOfJesus = Color(0xFFDA4227),
            searchHighlightBg = if (viewModel.darkTheme) Color(0xFF26EC2E).copy(alpha = 0.2f) else Color.Yellow.copy(alpha = 0.3f),
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
        } else {
            null
        }
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
    var wordDb by remember { mutableStateOf<DatabaseHelper?>(null) }

    var showStrongsModal by remember { mutableStateOf(false) }
    var currentStrongNumber by remember { mutableStateOf("") }
    var strongDefinition by remember { mutableStateOf("") }
    var strongDb by remember { mutableStateOf<DatabaseHelper?>(null) }

    var showCommentaryModal by remember { mutableStateOf(false) }
    var commentaryTitle by remember { mutableStateOf("") }
    var commentaryContent by remember { mutableStateOf("") }
    var commentaryBibleDb by remember { mutableStateOf<DatabaseHelper?>(null) }

    var showVerseCommentaryModal by remember { mutableStateOf(false) }
    var verseCommentaryBook by remember { mutableIntStateOf(0) }
    var verseCommentaryChapter by remember { mutableIntStateOf(0) }
    var verseCommentaryVerse by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel.selectedDictionary, databaseHelper?.databaseName) {
        dictionaryDbHelper?.close()
        dictionaryDbHelper = DatabaseHelper(contextFont, "${viewModel.selectedDictionary}.dictionary.sqlite3")
        strongDbHelper?.close()
        strongDbHelper = DatabaseHelper(contextFont, "secedictionary.sqlite3")
        commentaryDbHelper?.close()
        commentaryDbHelper = createCommentaryHelperIfExists(contextFont, databaseHelper?.databaseName)
    }

    LaunchedEffect(viewModel.multiVersion, viewModel.secondaryDbName, secondaryDatabaseHelper?.databaseName) {
        secondaryCommentaryDbHelper?.close()
        secondaryCommentaryDbHelper = createCommentaryHelperIfExists(contextFont, secondaryDatabaseHelper?.databaseName)
    }

    val onWordPress: (String, Boolean) -> Unit = { word, isPrimary ->
        currentModalIsOldTestament = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
        val trimmed = word.trim()
        val definition = dictionaryDbHelper?.getWordDefinition(trimmed) ?: "Definition not found."
        currentWord = trimmed
        wordDefinition = definition
        wordDb = if (isPrimary) databaseHelper else secondaryDatabaseHelper
        showWordModal = true
    }

    val onStrongsPress: (String, Int, Boolean) -> Unit = { strongNumber, _, isPrimary ->
        val isOldTestamentForVersion = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
        currentModalIsOldTestament = isOldTestamentForVersion
        val trimmed = strongNumber.trim()
        val prefixed = if (trimmed.firstOrNull()?.isLetter() ?: false) trimmed else (if (isOldTestamentForVersion) "H" else "G") + trimmed
        val definition = strongDbHelper?.getStrongDefinition(prefixed) ?: "Strong's definition not found."
        currentStrongNumber = prefixed
        strongDefinition = definition
        strongDb = if (isPrimary) databaseHelper else secondaryDatabaseHelper
        showStrongsModal = true
    }

    val onTagPress: (String, Int, Int, Int, Boolean) -> Unit = { marker, bookNumber, chapter, verseNumber, isPrimary ->
        currentModalIsOldTestament = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
        val dbHelper = if (isPrimary) commentaryDbHelper else secondaryCommentaryDbHelper
        val text = dbHelper?.getCommentary(bookNumber, chapter, verseNumber, marker) ?: "No commentary found."
        commentaryTitle = "Notes on ${BibleData.getBookByCustomNumber(bookNumber)?.name ?: ""} $chapter:$verseNumber$marker"
        commentaryContent = text
        commentaryBibleDb = if (isPrimary) databaseHelper else secondaryDatabaseHelper
        showCommentaryModal = true
    }

    val onVerseCommentaryClick: (bookNumber: Int, chapter: Int, verseNumber: Int) -> Unit = { book, chap, verseNum ->
        verseCommentaryBook = book
        verseCommentaryChapter = chap
        verseCommentaryVerse = verseNum
        currentModalIsOldTestament = viewModel.isOldTestament
        showVerseCommentaryModal = true
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.customTextureUri = it.toString(); viewModel.bgImageIndex = 34 }
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

    var showCrossRefModal by remember { mutableStateOf(false) }
    var crossRefSource by remember { mutableStateOf("") }
    var crossRefContent by remember { mutableStateOf("") }
    var crossRefBibleDb by remember { mutableStateOf<DatabaseHelper?>(null) }

    val onCrossRefClick: (Int, Int, Int, Boolean) -> Unit = { book, chapter, verse, isPrimary ->
        val dbForVerses = if (isPrimary) databaseHelper else secondaryDatabaseHelper
        val refs = crossRefHelper?.getCrossReferences(book, chapter, verse) ?: emptyList()
        val bookName = BibleData.getBookByCustomNumber(book)?.name ?: book.toString()
        crossRefSource = "References for $bookName $chapter:$verse"
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
        crossRefContent = htmlItems
        crossRefBibleDb = dbForVerses
        crossRefBook = book
        crossRefChapter = chapter
        crossRefVerse = verse
        currentModalIsOldTestament = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
        showCrossRefModal = true
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
                onWordPress = onWordPress,
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
                onWordPress = onWordPress,
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
                onWordPress = onWordPress,
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
                                } else {
                                    Modifier
                                }
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

        if (showBgModal) {
            BgModal(
                currentIndex = viewModel.bgImageIndex,
                customUri = viewModel.customTextureUri,
                onSelect = { index ->
                    viewModel.bgImageIndex = index
                    showBgModal = false
                },
                onDismiss = { showBgModal = false },
                onPickCustom = { imagePickerLauncher.launch("image/*") },
                onRemoveCustom = { viewModel.customTextureUri = null }
            )
        }

        InteractiveModal(
            show = showWordModal,
            onDismiss = { showWordModal = false },
            onNavigateToReader = { p ->
                viewModel.primaryPassage = p
                viewModel.navigateTo(Screen.Reader(p))
            },
            databaseHelper = wordDb,
            initialType = "definition",
            word = currentWord,
            definition = wordDefinition,
            isOldTestament = currentModalIsOldTestament
        )

        InteractiveModal(
            show = showStrongsModal,
            onDismiss = { showStrongsModal = false },
            onNavigateToReader = { p ->
                viewModel.primaryPassage = p
                viewModel.navigateTo(Screen.Reader(p))
            },
            databaseHelper = strongDb,
            initialType = "strong",
            strongNumber = currentStrongNumber,
            strongDefinition = strongDefinition,
            isOldTestament = currentModalIsOldTestament
        )

        InteractiveModal(
            show = showCommentaryModal,
            onDismiss = { showCommentaryModal = false },
            onNavigateToReader = { p ->
                viewModel.primaryPassage = p
                viewModel.navigateTo(Screen.Reader(p))
            },
            databaseHelper = commentaryBibleDb,
            initialType = "commentary",
            initialTitle = commentaryTitle,
            initialContent = commentaryContent,
            isOldTestament = currentModalIsOldTestament
        )

        InteractiveModal(
            show = showCrossRefModal,
            onDismiss = { showCrossRefModal = false },
            onNavigateToReader = { p ->
                viewModel.primaryPassage = p
                viewModel.navigateTo(Screen.Reader(p))
            },
            databaseHelper = crossRefBibleDb,
            initialType = "crossreference",
            initialTitle = crossRefSource,
            initialContent = crossRefContent,
            bookNumber = crossRefBook,
            chapter = crossRefChapter,
            verse = crossRefVerse,
            isOldTestament = currentModalIsOldTestament
        )

        InteractiveModal(
            show = showVerseCommentaryModal,
            onDismiss = { showVerseCommentaryModal = false },
            onNavigateToReader = { p ->
                viewModel.primaryPassage = p
                viewModel.navigateTo(Screen.Reader(p))
            },
            databaseHelper = databaseHelper,
            initialType = "versecommentary",
            bookNumber = verseCommentaryBook,
            chapter = verseCommentaryChapter,
            verse = verseCommentaryVerse,
            isOldTestament = currentModalIsOldTestament
        )

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
}

@Composable
private fun SingleVersionReader(
    primaryCurrent: PassageSelection,
    targetVerse: Int?,
    databaseHelper: DatabaseHelper?,
    subheadingsDbHelper: DatabaseHelper,
    primaryLoadedVerses: MutableMap<Pair<Int, Int>, List<VerseContent>>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    onPassageChange: (PassageSelection) -> Unit,
    scheduleFade: () -> Unit,
    onWordPress: (String, Boolean) -> Unit,
    onStrongsPress: (String, Int, Boolean) -> Unit,
    onTagPress: (String, Int, Int, Int, Boolean) -> Unit,
    onVerseLongPress: (Verse, PassageSelection, Boolean) -> Unit,
    onCrossRefClick: (Int, Int, Int, Boolean) -> Unit,
    crossRefHelper: DatabaseHelper?,
    refreshKey: Int,
    onVerseCommentaryClick: (Int, Int, Int) -> Unit,
    markerColor: Color,
    onWordHighlightAction: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val currentBook by remember(primaryCurrent.bookNumber) {
        derivedStateOf { BibleData.getBookByCustomNumber(primaryCurrent.bookNumber) }
    }
    val prevPassage by remember(primaryCurrent, currentBook) {
        derivedStateOf {
            if (currentBook == null) primaryCurrent else lazyGetPreviousChapter(primaryCurrent, currentBook)
        }
    }
    val nextPassage by remember(primaryCurrent, currentBook) {
        derivedStateOf {
            if (currentBook == null) primaryCurrent else lazyGetNextChapter(primaryCurrent, currentBook)
        }
    }
    val hasPrev by remember(prevPassage) { derivedStateOf { prevPassage != primaryCurrent } }
    val hasNext by remember(nextPassage) { derivedStateOf { nextPassage != primaryCurrent } }

    val passages by remember(primaryCurrent, prevPassage, nextPassage, hasPrev, hasNext) {
        derivedStateOf {
            buildList {
                if (hasPrev) add(prevPassage)
                add(primaryCurrent)
                if (hasNext) add(nextPassage)
            }
        }
    }
    val pageCount by remember(passages) { derivedStateOf { passages.size } }
    val currentOffset by remember(hasPrev) { derivedStateOf { if (hasPrev) 1 else 0 } }

    var pendingPassageChange by remember { mutableStateOf<PassageSelection?>(null) }

    LaunchedEffect(primaryCurrent, hasPrev, hasNext, databaseHelper) {
        val currentKey = primaryCurrent.bookNumber to primaryCurrent.chapter
        if (currentKey !in primaryLoadedVerses) {
            primaryLoadedVerses[currentKey] = withContext(Dispatchers.IO) {
                databaseHelper?.let { versesHelper ->
                    getVersesWithSubheadings(versesHelper, subheadingsDbHelper, primaryCurrent.bookNumber, primaryCurrent.chapter)
                } ?: emptyList()
            }
        }
        if (hasPrev) {
            val prevKey = prevPassage.bookNumber to prevPassage.chapter
            if (prevKey !in primaryLoadedVerses) {
                primaryLoadedVerses[prevKey] = withContext(Dispatchers.IO) {
                    databaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, prevPassage.bookNumber, prevPassage.chapter)
                    } ?: emptyList()
                }
            }
        }
        if (hasNext) {
            val nextKey = nextPassage.bookNumber to nextPassage.chapter
            if (nextKey !in primaryLoadedVerses) {
                primaryLoadedVerses[nextKey] = withContext(Dispatchers.IO) {
                    databaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, nextPassage.bookNumber, nextPassage.chapter)
                    } ?: emptyList()
                }
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
            .pointerInput(Unit) { detectTapGestures { scheduleFade() } },
        key = { pageIndex ->
            val passageKey = passages[pageIndex]
            "${passageKey.bookNumber}-${passageKey.chapter}-${viewModel.currentDbName}"
        }
    ) { pageIndex ->
        val thisPassage = passages[pageIndex]
        val primaryContent = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
        val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

        Box(modifier = Modifier.fillMaxSize()) {
            if (primaryContent.isEmpty()) {
                LoadingIndicator()
            } else {
                val primaryState = rememberLazyListState()
                ChapterView(
                    passage = thisPassage,
                    content = primaryContent,
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
                    onTagPress = onTagPress,
                    onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, true) },
                    databaseHelper = databaseHelper,
                    crossRefHelper = crossRefHelper,
                    onCrossRefClick = onCrossRefClick,
                    refreshKey = refreshKey,
                    onVerseCommentaryClick = onVerseCommentaryClick,
                    markerColor = markerColor,
                    onWordHighlightAction = onWordHighlightAction
                )
            }
        }
    }
}

@Composable
private fun SyncedMultiVersionReader(
    primaryCurrent: PassageSelection,
    targetVerse: Int?,
    databaseHelper: DatabaseHelper?,
    secondaryDatabaseHelper: DatabaseHelper?,
    subheadingsDbHelper: DatabaseHelper,
    primaryLoadedVerses: MutableMap<Pair<Int, Int>, List<VerseContent>>,
    secondaryLoadedVerses: MutableMap<Pair<Int, Int>, List<VerseContent>>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    onPassageChange: (PassageSelection) -> Unit,
    scheduleFade: () -> Unit,
    onWordPress: (String, Boolean) -> Unit,
    onStrongsPress: (String, Int, Boolean) -> Unit,
    onTagPress: (String, Int, Int, Int, Boolean) -> Unit,
    onVerseLongPress: (Verse, PassageSelection, Boolean) -> Unit,
    onCrossRefClick: (Int, Int, Int, Boolean) -> Unit,
    crossRefHelper: DatabaseHelper?,
    refreshKey: Int,
    onVerseCommentaryClick: (Int, Int, Int) -> Unit,
    markerColor: Color,
    onWordHighlightAction: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val currentBook by remember(primaryCurrent.bookNumber) {
        derivedStateOf { BibleData.getBookByCustomNumber(primaryCurrent.bookNumber) }
    }
    val prevPassage by remember(primaryCurrent, currentBook) {
        derivedStateOf {
            if (currentBook == null) primaryCurrent else lazyGetPreviousChapter(primaryCurrent, currentBook)
        }
    }
    val nextPassage by remember(primaryCurrent, currentBook) {
        derivedStateOf {
            if (currentBook == null) primaryCurrent else lazyGetNextChapter(primaryCurrent, currentBook)
        }
    }
    val hasPrev by remember(prevPassage) { derivedStateOf { prevPassage != primaryCurrent } }
    val hasNext by remember(nextPassage) { derivedStateOf { nextPassage != primaryCurrent } }

    val passages by remember(primaryCurrent, prevPassage, nextPassage, hasPrev, hasNext) {
        derivedStateOf {
            buildList {
                if (hasPrev) add(prevPassage)
                add(primaryCurrent)
                if (hasNext) add(nextPassage)
            }
        }
    }
    val pageCount by remember(passages) { derivedStateOf { passages.size } }
    val currentOffset by remember(hasPrev) { derivedStateOf { if (hasPrev) 1 else 0 } }

    var pendingPassageChange by remember { mutableStateOf<PassageSelection?>(null) }

    LaunchedEffect(primaryCurrent, hasPrev, hasNext, databaseHelper, secondaryDatabaseHelper) {
        val currentKey = primaryCurrent.bookNumber to primaryCurrent.chapter
        if (currentKey !in primaryLoadedVerses) {
            primaryLoadedVerses[currentKey] = withContext(Dispatchers.IO) {
                databaseHelper?.let { versesHelper ->
                    getVersesWithSubheadings(versesHelper, subheadingsDbHelper, primaryCurrent.bookNumber, primaryCurrent.chapter)
                } ?: emptyList()
            }
        }
        if (currentKey !in secondaryLoadedVerses) {
            secondaryLoadedVerses[currentKey] = withContext(Dispatchers.IO) {
                secondaryDatabaseHelper?.let { versesHelper ->
                    getVersesWithSubheadings(versesHelper, subheadingsDbHelper, primaryCurrent.bookNumber, primaryCurrent.chapter)
                } ?: emptyList()
            }
        }
        if (hasPrev) {
            val prevKey = prevPassage.bookNumber to prevPassage.chapter
            if (prevKey !in primaryLoadedVerses) {
                primaryLoadedVerses[prevKey] = withContext(Dispatchers.IO) {
                    databaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, prevPassage.bookNumber, prevPassage.chapter)
                    } ?: emptyList()
                }
            }
            if (prevKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[prevKey] = withContext(Dispatchers.IO) {
                    secondaryDatabaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, prevPassage.bookNumber, prevPassage.chapter)
                    } ?: emptyList()
                }
            }
        }
        if (hasNext) {
            val nextKey = nextPassage.bookNumber to nextPassage.chapter
            if (nextKey !in primaryLoadedVerses) {
                primaryLoadedVerses[nextKey] = withContext(Dispatchers.IO) {
                    databaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, nextPassage.bookNumber, nextPassage.chapter)
                    } ?: emptyList()
                }
            }
            if (nextKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[nextKey] = withContext(Dispatchers.IO) {
                    secondaryDatabaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, nextPassage.bookNumber, nextPassage.chapter)
                    } ?: emptyList()
                }
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
            .pointerInput(Unit) { detectTapGestures { scheduleFade() } },
        key = { pageIndex ->
            val passageKey = passages[pageIndex]
            "${passageKey.bookNumber}-${passageKey.chapter}-${viewModel.currentDbName}-${viewModel.secondaryDbName}"
        }
    ) { pageIndex ->
        val thisPassage = passages[pageIndex]
        val primaryContent = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
        val secondaryContent = secondaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
        val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

        Box(modifier = Modifier.fillMaxSize()) {
            if (primaryContent.isEmpty() || secondaryContent.isEmpty()) {
                LoadingIndicator()
            } else {
                val primaryState = rememberLazyListState()
                val secondaryState = rememberLazyListState()

                if (viewModel.scrollSync && !suppressSync) {
                    var isSyncing by remember { mutableStateOf(false) }

                    LaunchedEffect(primaryState) {
                        snapshotFlow {
                            primaryState.firstVisibleItemIndex to primaryState.firstVisibleItemScrollOffset
                        }.collect { (index, offset) ->
                            if (!isSyncing && primaryState.isScrollInProgress) {
                                isSyncing = true
                                secondaryState.scrollToItem(index, offset)
                                isSyncing = false
                            }
                        }
                    }
                    LaunchedEffect(secondaryState) {
                        snapshotFlow {
                            secondaryState.firstVisibleItemIndex to secondaryState.firstVisibleItemScrollOffset
                        }.collect { (index, offset) ->
                            if (!isSyncing && secondaryState.isScrollInProgress) {
                                isSyncing = true
                                primaryState.scrollToItem(index, offset)
                                isSyncing = false
                            }
                        }
                    }
                }

                if (viewModel.multiViewLayout == "horizontal") {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ChapterView(
                            passage = thisPassage,
                            content = primaryContent,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, true) },
                            databaseHelper = databaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
                        )
                        ChapterView(
                            passage = thisPassage,
                            content = secondaryContent,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, false) },
                            databaseHelper = secondaryDatabaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ChapterView(
                            passage = thisPassage,
                            content = primaryContent,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, true) },
                            databaseHelper = databaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
                        )
                        ChapterView(
                            passage = thisPassage,
                            content = secondaryContent,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, false) },
                            databaseHelper = secondaryDatabaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndependentMultiVersionReader(
    primaryCurrent: PassageSelection,
    secondaryCurrent: PassageSelection,
    targetVerse: Int?,
    secondaryTargetVerse: Int?,
    databaseHelper: DatabaseHelper?,
    secondaryDatabaseHelper: DatabaseHelper?,
    subheadingsDbHelper: DatabaseHelper,
    primaryLoadedVerses: MutableMap<Pair<Int, Int>, List<VerseContent>>,
    secondaryLoadedVerses: MutableMap<Pair<Int, Int>, List<VerseContent>>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    onPrimaryPassageChange: (PassageSelection) -> Unit,
    onSecondaryPassageChange: (PassageSelection) -> Unit,
    scheduleFade: () -> Unit,
    onWordPress: (String, Boolean) -> Unit,
    onStrongsPress: (String, Int, Boolean) -> Unit,
    onTagPress: (String, Int, Int, Int, Boolean) -> Unit,
    onVerseLongPress: (Verse, PassageSelection, Boolean) -> Unit,
    onCrossRefClick: (Int, Int, Int, Boolean) -> Unit,
    crossRefHelper: DatabaseHelper?,
    refreshKey: Int,
    onVerseCommentaryClick: (Int, Int, Int) -> Unit,
    markerColor: Color,
    onWordHighlightAction: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val primaryBook by remember(primaryCurrent.bookNumber) {
        derivedStateOf { BibleData.getBookByCustomNumber(primaryCurrent.bookNumber) }
    }
    val primaryPrev by remember(primaryCurrent, primaryBook) {
        derivedStateOf {
            if (primaryBook == null) primaryCurrent else lazyGetPreviousChapter(primaryCurrent, primaryBook)
        }
    }
    val primaryNext by remember(primaryCurrent, primaryBook) {
        derivedStateOf {
            if (primaryBook == null) primaryCurrent else lazyGetNextChapter(primaryCurrent, primaryBook)
        }
    }
    val primaryHasPrev by remember(primaryPrev) { derivedStateOf { primaryPrev != primaryCurrent } }
    val primaryHasNext by remember(primaryNext) { derivedStateOf { primaryNext != primaryCurrent } }

    val primaryPassages by remember(primaryCurrent, primaryPrev, primaryNext, primaryHasPrev, primaryHasNext) {
        derivedStateOf {
            buildList {
                if (primaryHasPrev) add(primaryPrev)
                add(primaryCurrent)
                if (primaryHasNext) add(primaryNext)
            }
        }
    }
    val primaryPageCount by remember(primaryPassages) { derivedStateOf { primaryPassages.size } }
    val primaryOffset by remember(primaryHasPrev) { derivedStateOf { if (primaryHasPrev) 1 else 0 } }
    var primaryPendingChange by remember { mutableStateOf<PassageSelection?>(null) }

    val secondaryBook by remember(secondaryCurrent.bookNumber) {
        derivedStateOf { BibleData.getBookByCustomNumber(secondaryCurrent.bookNumber) }
    }
    val secondaryPrev by remember(secondaryCurrent, secondaryBook) {
        derivedStateOf {
            if (secondaryBook == null) secondaryCurrent else lazyGetPreviousChapter(secondaryCurrent, secondaryBook)
        }
    }
    val secondaryNext by remember(secondaryCurrent, secondaryBook) {
        derivedStateOf {
            if (secondaryBook == null) secondaryCurrent else lazyGetNextChapter(secondaryCurrent, secondaryBook)
        }
    }
    val secondaryHasPrev by remember(secondaryPrev) { derivedStateOf { secondaryPrev != secondaryCurrent } }
    val secondaryHasNext by remember(secondaryNext) { derivedStateOf { secondaryNext != secondaryCurrent } }

    val secondaryPassages by remember(secondaryCurrent, secondaryPrev, secondaryNext, secondaryHasPrev, secondaryHasNext) {
        derivedStateOf {
            buildList {
                if (secondaryHasPrev) add(secondaryPrev)
                add(secondaryCurrent)
                if (secondaryHasNext) add(secondaryNext)
            }
        }
    }
    val secondaryPageCount by remember(secondaryPassages) { derivedStateOf { secondaryPassages.size } }
    val secondaryOffset by remember(secondaryHasPrev) { derivedStateOf { if (secondaryHasPrev) 1 else 0 } }
    var secondaryPendingChange by remember { mutableStateOf<PassageSelection?>(null) }

    LaunchedEffect(primaryCurrent, primaryHasPrev, primaryHasNext, databaseHelper) {
        val currentKey = primaryCurrent.bookNumber to primaryCurrent.chapter
        if (currentKey !in primaryLoadedVerses) {
            primaryLoadedVerses[currentKey] = withContext(Dispatchers.IO) {
                databaseHelper?.let { versesHelper ->
                    getVersesWithSubheadings(versesHelper, subheadingsDbHelper, primaryCurrent.bookNumber, primaryCurrent.chapter)
                } ?: emptyList()
            }
        }
        if (primaryHasPrev) {
            val prevKey = primaryPrev.bookNumber to primaryPrev.chapter
            if (prevKey !in primaryLoadedVerses) {
                primaryLoadedVerses[prevKey] = withContext(Dispatchers.IO) {
                    databaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, primaryPrev.bookNumber, primaryPrev.chapter)
                    } ?: emptyList()
                }
            }
        }
        if (primaryHasNext) {
            val nextKey = primaryNext.bookNumber to primaryNext.chapter
            if (nextKey !in primaryLoadedVerses) {
                primaryLoadedVerses[nextKey] = withContext(Dispatchers.IO) {
                    databaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, primaryNext.bookNumber, primaryNext.chapter)
                    } ?: emptyList()
                }
            }
        }
    }

    LaunchedEffect(secondaryCurrent, secondaryHasPrev, secondaryHasNext, secondaryDatabaseHelper) {
        val currentKey = secondaryCurrent.bookNumber to secondaryCurrent.chapter
        if (currentKey !in secondaryLoadedVerses) {
            secondaryLoadedVerses[currentKey] = withContext(Dispatchers.IO) {
                secondaryDatabaseHelper?.let { versesHelper ->
                    getVersesWithSubheadings(versesHelper, subheadingsDbHelper, secondaryCurrent.bookNumber, secondaryCurrent.chapter)
                } ?: emptyList()
            }
        }
        if (secondaryHasPrev) {
            val prevKey = secondaryPrev.bookNumber to secondaryPrev.chapter
            if (prevKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[prevKey] = withContext(Dispatchers.IO) {
                    secondaryDatabaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, secondaryPrev.bookNumber, secondaryPrev.chapter)
                    } ?: emptyList()
                }
            }
        }
        if (secondaryHasNext) {
            val nextKey = secondaryNext.bookNumber to secondaryNext.chapter
            if (nextKey !in secondaryLoadedVerses) {
                secondaryLoadedVerses[nextKey] = withContext(Dispatchers.IO) {
                    secondaryDatabaseHelper?.let { versesHelper ->
                        getVersesWithSubheadings(versesHelper, subheadingsDbHelper, secondaryNext.bookNumber, secondaryNext.chapter)
                    } ?: emptyList()
                }
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
    val containerModifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { scheduleFade() } }

    if (layoutHorizontal) {
        Row(modifier = containerModifier) {
            HorizontalPager(
                state = primaryPagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex ->
                    val pk = primaryPassages[pageIndex]
                    "${pk.bookNumber}-${pk.chapter}-primary-${viewModel.currentDbName}"
                }
            ) { pageIndex ->
                val thisPassage = primaryPassages[pageIndex]
                val content = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = thisPassage,
                            content = content,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, true) },
                            databaseHelper = databaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
                        )
                    }
                }
            }

            HorizontalPager(
                state = secondaryPagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex ->
                    val pk = secondaryPassages[pageIndex]
                    "${pk.bookNumber}-${pk.chapter}-secondary-${viewModel.secondaryDbName}"
                }
            ) { pageIndex ->
                val thisPassage = secondaryPassages[pageIndex]
                val content = secondaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == secondaryCurrent.bookNumber && thisPassage.chapter == secondaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = thisPassage,
                            content = content,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, false) },
                            databaseHelper = secondaryDatabaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
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
                    "${pk.bookNumber}-${pk.chapter}-primary-${viewModel.currentDbName}"
                }
            ) { pageIndex ->
                val thisPassage = primaryPassages[pageIndex]
                val content = primaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == primaryCurrent.bookNumber && thisPassage.chapter == primaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = thisPassage,
                            content = content,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, true) },
                            databaseHelper = databaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
                        )
                    }
                }
            }

            HorizontalPager(
                state = secondaryPagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex ->
                    val pk = secondaryPassages[pageIndex]
                    "${pk.bookNumber}-${pk.chapter}-secondary-${viewModel.secondaryDbName}"
                }
            ) { pageIndex ->
                val thisPassage = secondaryPassages[pageIndex]
                val content = secondaryLoadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()
                val isCurrentPage = thisPassage.bookNumber == secondaryCurrent.bookNumber && thisPassage.chapter == secondaryCurrent.chapter

                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = thisPassage,
                            content = content,
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
                            onTagPress = onTagPress,
                            onVerseLongPress = { verse, passageSelection -> onVerseLongPress(verse, passageSelection, false) },
                            databaseHelper = secondaryDatabaseHelper,
                            crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick,
                            refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor,
                            onWordHighlightAction = onWordHighlightAction
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
    content: List<VerseContent>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    isCurrentPage: Boolean,
    targetVerse: Int?,
    versionAbbr: String,
    isPrimary: Boolean,
    state: LazyListState,
    modifier: Modifier = Modifier,
    isKjvPlus: Boolean = false,
    onInitialScrollComplete: () -> Unit = {},
    onWordPress: ((String, Boolean) -> Unit)? = null,
    onStrongsPress: ((String, Int, Boolean) -> Unit)? = null,
    onTagPress: ((String, Int, Int, Int, Boolean) -> Unit)? = null,
    onVerseLongPress: ((Verse, PassageSelection) -> Unit)? = null,
    databaseHelper: DatabaseHelper? = null,
    crossRefHelper: DatabaseHelper? = null,
    onCrossRefClick: ((Int, Int, Int, Boolean) -> Unit)? = null,
    refreshKey: Int = 0,
    onVerseCommentaryClick: ((Int, Int, Int) -> Unit)? = null,
    markerColor: Color,
    onWordHighlightAction: (() -> Unit)? = null
) {
    val isOldTestamentForThisVersion = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament

    val processedVerses by produceState(
        initialValue = emptyMap(),
        content,
        viewModel.fontSize,
        themeColors,
        isKjvPlus,
        refreshKey,
        isOldTestamentForThisVersion
    ) {
        value = withContext(Dispatchers.Default) {
            val result = mutableMapOf<Int, ProcessedVerse>()

            val processor = VerseTextProcessor()
            content.forEach { item ->
                if (item is VerseContent.VerseVal) {
                    val verse = item.verse
                    val fullVerse = verse.copy(bookName = passage.bookName, chapter = passage.chapter)
                    val isPersistentHighlighted = databaseHelper?.isHighlighted(fullVerse) ?: false

                    val onStrongsLocal: ((String) -> Unit)? = onStrongsPress?.let { callback ->
                        { strong -> callback(strong, passage.bookNumber, isPrimary) }
                    }
                    val onTagLocal: ((String) -> Unit)? = onTagPress?.let { callback ->
                        { marker -> callback(marker, passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary) }
                    }
                    val onWordLocal: ((String) -> Unit)? = onWordPress?.let { callback ->
                        { word -> callback(word, isPrimary) }
                    }

                    val processed = processor.processVerse(
                        verseText = verse.text,
                        baseFontSize = viewModel.fontSize.sp,
                        themeColors = themeColors,
                        textColor = themeColors.textColor,
                        onTagPress = onTagLocal,
                        onWordPress = onWordLocal,
                        onStrongsPress = onStrongsLocal,
                        isHighlighted = isPersistentHighlighted,
                        isKjvPlus = isKjvPlus,
                        isOldTestament = isOldTestamentForThisVersion
                    )
                    result[verse.verseNumber] = processed
                }
            }
            result
        }
    }

    var highlightedVerse by remember { mutableStateOf<Int?>(null) }

    val bookmarkIconSize = viewModel.fontSize
    val bookmarkInlineContent = InlineTextContent(
        Placeholder(
            width = bookmarkIconSize.sp,
            height = bookmarkIconSize.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = "Bookmarked",
            tint = themeColors.verseNumber,
            modifier = Modifier.fillMaxSize()
        )
    }

    val noteInlineContent = InlineTextContent(
        Placeholder(
            width = bookmarkIconSize.sp,
            height = bookmarkIconSize.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Note,
            contentDescription = "Has Note",
            tint = themeColors.verseNumber,
            modifier = Modifier.fillMaxSize().rotate(90f)
        )
    }

    val crossRefCounts = remember { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(passage.bookNumber, passage.chapter, crossRefHelper) {
        val counts = crossRefHelper?.getCrossReferenceCountsForChapter(passage.bookNumber, passage.chapter) ?: emptyMap()
        crossRefCounts.clear()
        crossRefCounts.putAll(counts)
    }

    var selectedWords by remember(
        passage.bookNumber,
        passage.chapter,
        refreshKey,
        databaseHelper?.databaseName
    ) {
        mutableStateOf(
            buildSet {
                if (databaseHelper != null) {
                    content.forEach { item ->
                        if (item is VerseContent.VerseVal) {
                            val fullVerse = item.verse.copy(bookName = passage.bookName, chapter = passage.chapter)
                            val highlights = databaseHelper.getWordHighlightsForVerse(fullVerse)
                            addAll(highlights)
                        }
                    }
                }
            }
        )
    }

    val groupedHighlights = remember(selectedWords) {
        selectedWords.groupBy { it.verseNumber }
    }

    Box(modifier = modifier) {
        val texture = when (viewModel.bgImageIndex) {
            0 -> null
            34 -> if (viewModel.customTextureUri != null) viewModel.customTextureUri else null
            else -> "file:///android_asset/textures/${viewModel.bgImageIndex}.jpg"
        }

        if (texture != null) {
            AsyncImage(
                model = texture,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            val overlayColor = (if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor)
                .copy(alpha = viewModel.overlayOpacity)
            Box(modifier = Modifier.fillMaxSize().background(overlayColor))
        }

        Column {
            if (viewModel.multiVersion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.primary)
                        .padding(4.dp),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(20.dp).weight(0.7f).padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = passage.bookName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = passage.chapter.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isPrimary) viewModel.showPrimaryVersionDropdown = true
                            else viewModel.showSecondaryVersionDropdown = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(20.dp).weight(0.5f)
                    ) {
                        Text(
                            text = versionAbbr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }

            LazyColumn(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                items(
                    items = content,
                    key = { item ->
                        when (item) {
                            is VerseContent.SubheadingVal -> "sub_${item.subheading.text.hashCode()}"
                            is VerseContent.VerseVal -> "verse_${item.verse.verseNumber}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is VerseContent.SubheadingVal -> {
                            Text(
                                text = item.subheading.text,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.primary,
                                fontSize = (viewModel.fontSize + 1).sp,
                                lineHeight = ((viewModel.fontSize + 1) * 1.2f).sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                textAlign = TextAlign.Left,
                                fontFamily = currentFontFamily
                            )
                        }

                        is VerseContent.VerseVal -> {
                            val verse = item.verse
                            val processedVerse = processedVerses[verse.verseNumber]
                            val isTemporaryHighlighted = verse.verseNumber == highlightedVerse
                            val fullVerse = verse.copy(bookName = passage.bookName, chapter = passage.chapter)
                            val isPersistentHighlighted = databaseHelper?.isHighlighted(fullVerse) ?: false
                            val persistentHighlightColor = if (isPersistentHighlighted) {
                                databaseHelper.getHighlightColor(fullVerse) ?: themeColors.searchHighlightBg
                            } else null
                            val isBookmarked = databaseHelper?.isBookmarked(fullVerse) ?: false
                            val isNote = databaseHelper?.hasNote(fullVerse) ?: false
                            val refCount = crossRefCounts[verse.verseNumber] ?: 0

                            val backgroundModifier = when {
                                persistentHighlightColor != null -> Modifier.background(persistentHighlightColor)
                                isTemporaryHighlighted -> Modifier.background(themeColors.searchHighlightBg)
                                else -> Modifier
                            }

                            val currentMarkerColor by rememberUpdatedState(markerColor)
                            val wordHighlightsForVerse = groupedHighlights[verse.verseNumber] ?: emptyList()
                            val highlightsHash = wordHighlightsForVerse.hashCode()

                            key("verse_${verse.verseNumber}_${isPersistentHighlighted}_${isBookmarked}_${isNote}_$highlightsHash") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .then(backgroundModifier)
                                ) {
                                    processedVerse?.header?.let { header ->
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

                                    val annotatedString = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = themeColors.verseNumber,
                                                fontSize = bookmarkIconSize.sp * 0.8f
                                            )
                                        ) {
                                            append("${verse.verseNumber} ")
                                        }
                                        append(processedVerse?.body ?: verse.text)

                                        if (isBookmarked) appendInlineContent("bookmark", "[bookmark]")
                                        if (isNote) appendInlineContent("note", "[note]")

                                        if (viewModel.isStudyMode) {
                                            if (refCount > 0 && onCrossRefClick != null) {
                                                append(" ")
                                                appendInlineContent("crossref_${verse.verseNumber}", "[$refCount]")
                                            }
                                            if (onVerseCommentaryClick != null) {
                                                appendInlineContent("commentary_${verse.verseNumber}", "[C]")
                                            }
                                        }
                                    }

                                    val inlineContentMap = remember(
                                        verse.verseNumber,
                                        refCount,
                                        isNote,
                                        isBookmarked,
                                        viewModel.fontSize
                                    ) {
                                        buildMap {
                                            if (isBookmarked) put("bookmark", bookmarkInlineContent)
                                            if (isNote) put("note", noteInlineContent)

                                            if (viewModel.isStudyMode) {
                                                if (refCount > 0 && onCrossRefClick != null) {
                                                    put("crossref_${verse.verseNumber}", InlineTextContent(
                                                        Placeholder(
                                                            width = (viewModel.fontSize * 1.4).sp,
                                                            height = (viewModel.fontSize).sp,
                                                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                                        )
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clickable {
                                                                    onCrossRefClick(
                                                                        passage.bookNumber,
                                                                        passage.chapter,
                                                                        verse.verseNumber,
                                                                        isPrimary
                                                                    )
                                                                }
                                                                .background(
                                                                    themeColors.primary.copy(alpha = 0.15f),
                                                                    RoundedCornerShape(2.dp)
                                                                )
                                                                .fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "$refCount",
                                                                fontSize = (viewModel.fontSize * 0.7f).sp,
                                                                color = themeColors.primary,
                                                                fontWeight = FontWeight.Bold,
                                                                style = TextStyle(lineHeight = (viewModel.fontSize).sp)
                                                            )
                                                        }
                                                    })
                                                }

                                                if (onVerseCommentaryClick != null) {
                                                    put("commentary_${verse.verseNumber}", InlineTextContent(
                                                        Placeholder(
                                                            width = (viewModel.fontSize * 1.5f).sp,
                                                            height = (viewModel.fontSize).sp,
                                                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                                        )
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clickable {
                                                                    onVerseCommentaryClick(
                                                                        passage.bookNumber,
                                                                        passage.chapter,
                                                                        verse.verseNumber
                                                                    )
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.ChevronRight,
                                                                contentDescription = "View Verse Commentaries",
                                                                tint = themeColors.verseNumber,
                                                                modifier = Modifier.size((viewModel.fontSize * 1.5f).dp)
                                                            )
                                                        }
                                                    })
                                                }
                                            }
                                        }
                                    }

                                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                    val finalAnnotatedString = remember(
                                        annotatedString,
                                        wordHighlightsForVerse
                                    ) {
                                        if (wordHighlightsForVerse.isEmpty()) {
                                            annotatedString
                                        } else {
                                            val builder = AnnotatedString.Builder()
                                            builder.append(annotatedString)
                                            wordHighlightsForVerse.sortedByDescending { it.start }.forEach { word ->
                                                if (word.start in 0 until builder.length && word.end in word.start..builder.length) {
                                                    builder.addStyle(
                                                        SpanStyle(background = word.color),
                                                        word.start,
                                                        word.end
                                                    )
                                                }
                                            }
                                            builder.toAnnotatedString()
                                        }
                                    }

                                    Text(
                                        text = finalAnnotatedString,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(viewModel.isDictionaryMode) {
                                                detectTapGestures(
                                                    onTap = { offset ->
                                                        textLayoutResult?.let { layout ->
                                                            val position = layout.getOffsetForPosition(offset)
                                                            val annotations = finalAnnotatedString.getStringAnnotations(
                                                                start = position,
                                                                end = position
                                                            )
                                                            val wordAnnotation = annotations.find { it.tag == "word" }
                                                            if (wordAnnotation != null) {
                                                                if (viewModel.isDictionaryMode) {
                                                                    onWordPress?.invoke(wordAnnotation.item, isPrimary)
                                                                } else {
                                                                    val word = SelectedWord(
                                                                        verse.verseNumber,
                                                                        wordAnnotation.start,
                                                                        wordAnnotation.end,
                                                                        currentMarkerColor
                                                                    )
                                                                    val wasSelected = selectedWords.contains(word)
                                                                    selectedWords = if (wasSelected) {
                                                                        selectedWords - word
                                                                    } else {
                                                                        selectedWords + word
                                                                    }
                                                                    if (databaseHelper != null) {
                                                                        if (wasSelected) {
                                                                            databaseHelper.removeWordHighlight(fullVerse, word.start, word.end)
                                                                        } else {
                                                                            databaseHelper.addWordHighlight(
                                                                                fullVerse,
                                                                                word.start,
                                                                                word.end,
                                                                                currentMarkerColor.toArgb()
                                                                            )
                                                                        }
                                                                        onWordHighlightAction?.invoke()
                                                                    }
                                                                }
                                                            } else {
                                                                annotations.forEach { annotation ->
                                                                    when (annotation.tag) {
                                                                        "strong" -> onStrongsPress?.invoke(
                                                                            annotation.item,
                                                                            passage.bookNumber,
                                                                            isPrimary
                                                                        )
                                                                        "tag" -> onTagPress?.invoke(
                                                                            annotation.item,
                                                                            passage.bookNumber,
                                                                            passage.chapter,
                                                                            verse.verseNumber,
                                                                            isPrimary
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onLongPress = {
                                                        onVerseLongPress?.invoke(verse, passage)
                                                    }
                                                )
                                            },
                                        fontSize = viewModel.fontSize.sp,
                                        lineHeight = (viewModel.fontSize * 1.333f).sp,
                                        fontFamily = currentFontFamily,
                                        inlineContent = inlineContentMap,
                                        onTextLayout = { textLayoutResult = it }
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (isCurrentPage) {
        LaunchedEffect(targetVerse, content) {
            if (targetVerse != null && content.isNotEmpty() && targetVerse > 0) {
                delay(100)
                val targetIndex = content.indexOfFirst { it is VerseContent.VerseVal && it.verse.verseNumber == targetVerse }
                if (targetIndex >= 0) {
                    state.animateScrollToItem(targetIndex)
                    highlightedVerse = targetVerse
                    delay(2000)
                    highlightedVerse = null
                }
            }
            onInitialScrollComplete()
        }
    }
}
fun lazyGetPreviousChapter(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters <= 2 && current.chapter == 1) return current
    return if (current.chapter == 1) {
        current.copy(chapter = currentBook.chapters, verse = null)
    } else {
        current.copy(chapter = current.chapter - 1, verse = null)
    }
}

fun lazyGetNextChapter(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters <= 2 && current.chapter == currentBook.chapters) return current
    return if (current.chapter == currentBook.chapters) {
        current.copy(chapter = 1, verse = null)
    } else {
        current.copy(chapter = current.chapter + 1, verse = null)
    }
}