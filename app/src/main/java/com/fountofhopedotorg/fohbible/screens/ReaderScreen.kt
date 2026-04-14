package com.fountofhopedotorg.fohbible.screens
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.core.ChapterPager
import com.fountofhopedotorg.fohbible.core.ChapterView
import com.fountofhopedotorg.fohbible.core.LoadingIndicator
import com.fountofhopedotorg.fohbible.core.createCommentaryHelperIfExists
import com.fountofhopedotorg.fohbible.core.preloadChapter
import com.fountofhopedotorg.fohbible.core.rememberChapterPagerConfig
import com.fountofhopedotorg.fohbible.data.*
import com.fountofhopedotorg.fohbible.modals.InteractiveModal
import com.fountofhopedotorg.fohbible.modals.NotesModal
import com.fountofhopedotorg.fohbible.modals.VerseOptionsModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    passage: PassageSelection,
    databaseHelper: DatabaseHelper?,
    onPassageChange: (PassageSelection) -> Unit = {}
) {
    val viewModel = viewModel<AppViewModel>()
    val colorScheme = MaterialTheme.colorScheme
    val themeColors = remember(colorScheme, viewModel.darkTheme) {
        ThemeColors(
            textColor = colorScheme.onBackground,
            verseNumber = colorScheme.primary.copy(0.8f),
            primary = colorScheme.primary,
            tagColor = colorScheme.secondary,
            tagBg = colorScheme.secondary.copy(alpha = 0.1f),
            wordsOfJesus = Color(0xFFDA4227).copy(0.8f),
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
    val onVerseCommentaryClick: (Int, Int, Int) -> Unit = { book, chap, verseNum ->
        verseCommentaryBook = book
        verseCommentaryChapter = chap
        verseCommentaryVerse = verseNum
        currentModalIsOldTestament = viewModel.isOldTestament
        showVerseCommentaryModal = true
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.customTextureUri = it.toString()
            viewModel.bgImageIndex = 34
        }
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
            SingleVersionReaderLazy(
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
            SyncedMultiVersionReaderLazy(
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
            IndependentMultiVersionReaderLazy(
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
@Composable
private fun SingleVersionReaderLazy(
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
    onWordHighlightAction: (() -> Unit)?
) {
    val config = rememberChapterPagerConfig(primaryCurrent)

    LaunchedEffect(primaryCurrent, databaseHelper) {
        preloadChapter(primaryCurrent, primaryLoadedVerses, databaseHelper, subheadingsDbHelper)
        if (config.hasPrev) preloadChapter(config.passages.first(), primaryLoadedVerses, databaseHelper, subheadingsDbHelper)
        if (config.hasNext) preloadChapter(config.passages.last(), primaryLoadedVerses, databaseHelper, subheadingsDbHelper)
    }

    ChapterPager(
        config = config,
        modifier = Modifier.fillMaxSize(),
        scheduleFade = scheduleFade,
        onPassageChange = onPassageChange
    ) { _, passage, isCurrentPage ->
        val content = primaryLoadedVerses[passage.bookNumber to passage.chapter] ?: emptyList()
        Box(modifier = Modifier.fillMaxSize()) {
            if (content.isEmpty()) {
                LoadingIndicator()
            } else {
                val lazyState = rememberLazyListState()
                ChapterView(
                    passage = passage,
                    content = content,
                    themeColors = themeColors,
                    currentFontFamily = currentFontFamily,
                    viewModel = viewModel,
                    isCurrentPage = isCurrentPage,
                    targetVerse = targetVerse,
                    versionAbbr = viewModel.currentVersionAbbr,
                    isPrimary = true,
                    lazyState = lazyState,
                    modifier = Modifier.fillMaxSize(),
                    onWordPress = onWordPress,
                    onStrongsPress = onStrongsPress,
                    onTagPress = onTagPress,
                    onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, true) },
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
private fun SyncedMultiVersionReaderLazy(
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
    onWordHighlightAction: (() -> Unit)?
) {
    val config = rememberChapterPagerConfig(primaryCurrent)

    LaunchedEffect(primaryCurrent, databaseHelper, secondaryDatabaseHelper) {
        val loadBoth = suspend { p: PassageSelection ->
            preloadChapter(p, primaryLoadedVerses, databaseHelper, subheadingsDbHelper)
            preloadChapter(p, secondaryLoadedVerses, secondaryDatabaseHelper, subheadingsDbHelper)
        }
        loadBoth(primaryCurrent)
        if (config.hasPrev) loadBoth(config.passages.first())
        if (config.hasNext) loadBoth(config.passages.last())
    }

    var suppressSync by remember { mutableStateOf(false) }
    var completedScrolls by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetVerse) {
        suppressSync = true
        completedScrolls = 0
    }

    ChapterPager(
        config = config,
        modifier = Modifier.fillMaxSize(),
        scheduleFade = scheduleFade,
        onPassageChange = onPassageChange
    ) { _, passage, isCurrentPage ->
        val primaryContent = primaryLoadedVerses[passage.bookNumber to passage.chapter] ?: emptyList()
        val secondaryContent = secondaryLoadedVerses[passage.bookNumber to passage.chapter] ?: emptyList()

        if (primaryContent.isEmpty() || secondaryContent.isEmpty()) {
            LoadingIndicator()
        } else {
            val primaryState = rememberLazyListState()
            val secondaryState = rememberLazyListState()
            val primarySize by rememberUpdatedState(primaryContent.size)
            val secondarySize by rememberUpdatedState(secondaryContent.size)

            if (viewModel.scrollSync && !suppressSync) {
                var driver by remember { mutableIntStateOf(0) }
                val heightCache = remember { mutableMapOf<Int, Int>() }
                var primaryAvgHeight by remember { mutableFloatStateOf(150f) }
                var secondaryAvgHeight by remember { mutableFloatStateOf(150f) }

                fun getStableHeight(state: LazyListState, index: Int, isPrimary: Boolean): Int {
                    val measured = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size
                    if (measured != null) {
                        heightCache[index] = measured
                        if (isPrimary) primaryAvgHeight = (primaryAvgHeight * 0.9f) + (measured * 0.1f)
                        else secondaryAvgHeight = (secondaryAvgHeight * 0.9f) + (measured * 0.1f)
                        return measured
                    }
                    return heightCache[index] ?: (if (isPrimary) primaryAvgHeight else secondaryAvgHeight).toInt()
                }

                LaunchedEffect(primaryState) {
                    val scope = this
                    var lastIdx = 0
                    var lastOff = 0

                    snapshotFlow {
                        val layoutInfo = primaryState.layoutInfo
                        val first = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@snapshotFlow null
                        val items = layoutInfo.visibleItemsInfo.map { Triple(it.index, it.offset, it.size) }

                        Quadruple(
                            Triple(first.index, primaryState.firstVisibleItemScrollOffset, items),
                            layoutInfo.viewportSize.height,
                            primaryState.canScrollBackward,
                            primaryState.canScrollForward
                        )
                    }.filterNotNull().collect { (firstData, viewportHeight, canBack, canForward) ->
                        if (driver == 1) {
                            val (fIndex, fOff, items) = firstData
                            val maxIndex = minOf(primarySize, secondarySize) - 1
                            if (maxIndex < 0) return@collect
                            if (!canBack) { scope.launch { secondaryState.animateScrollToItem(0, 0) }; return@collect }
                            if (!canForward) { scope.launch { secondaryState.animateScrollToItem(secondarySize - 1, 0) }; return@collect }

                            val isDown = fIndex < lastIdx || (fIndex == lastIdx && fOff < lastOff)
                            lastIdx = fIndex
                            lastOff = fOff

                            if (isDown) {
                                val validLast = items.lastOrNull { it.first <= maxIndex } ?: items.first()
                                val itemBottom = validLast.second + validLast.third
                                val distFromBottom = viewportHeight - itemBottom

                                val ratio = distFromBottom.toFloat() / validLast.third.coerceAtLeast(1)
                                val sSize = getStableHeight(secondaryState, validLast.first, false)
                                val sViewport = secondaryState.layoutInfo.viewportSize.height
                                val targetOffset = sViewport - (sSize * ratio).toInt() - sSize

                                scope.launch {
                                    secondaryState.scrollToItem(validLast.first, -targetOffset)
                                }
                            } else {
                                val validFirst = items.firstOrNull { it.first <= maxIndex } ?: items.last()
                                val ratio = (-validFirst.second).toFloat() / validFirst.third.coerceAtLeast(1)
                                val sSize = getStableHeight(secondaryState, validFirst.first, false)

                                scope.launch {
                                    secondaryState.scrollToItem(validFirst.first, (sSize * ratio).toInt())
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(secondaryState) {
                    val scope = this
                    var lastIdx = 0
                    var lastOff = 0

                    snapshotFlow {
                        val layoutInfo = secondaryState.layoutInfo
                        val first = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@snapshotFlow null
                        val items = layoutInfo.visibleItemsInfo.map { Triple(it.index, it.offset, it.size) }

                        Quadruple(
                            Triple(first.index, secondaryState.firstVisibleItemScrollOffset, items),
                            layoutInfo.viewportSize.height,
                            secondaryState.canScrollBackward,
                            secondaryState.canScrollForward
                        )
                    }.filterNotNull().collect { (firstData, viewportHeight, canBack, canForward) ->
                        if (driver == 2) {
                            val (fIndex, fOff, items) = firstData
                            val maxIndex = minOf(primarySize, secondarySize) - 1
                            if (maxIndex < 0) return@collect

                            if (!canBack) { scope.launch { primaryState.animateScrollToItem(0, 0) }; return@collect }
                            if (!canForward) { scope.launch { primaryState.animateScrollToItem(primarySize - 1, 0) }; return@collect }

                            val isDown = fIndex < lastIdx || (fIndex == lastIdx && fOff < lastOff)
                            lastIdx = fIndex
                            lastOff = fOff

                            if (isDown) {
                                val validLast = items.lastOrNull { it.first <= maxIndex } ?: items.first()
                                val itemBottom = validLast.second + validLast.third
                                val distFromBottom = viewportHeight - itemBottom

                                val ratio = distFromBottom.toFloat() / validLast.third.coerceAtLeast(1)
                                val pSize = getStableHeight(primaryState, validLast.first, true)
                                val pViewport = primaryState.layoutInfo.viewportSize.height

                                val targetOffset = pViewport - (pSize * ratio).toInt() - pSize

                                scope.launch {
                                    primaryState.scrollToItem(validLast.first, -targetOffset)
                                }
                            } else {
                                val validFirst = items.firstOrNull { it.first <= maxIndex } ?: items.last()
                                val ratio = (-validFirst.second).toFloat() / validFirst.third.coerceAtLeast(1)
                                val pSize = getStableHeight(primaryState, validFirst.first, true)

                                scope.launch {
                                    primaryState.scrollToItem(validFirst.first, (pSize * ratio).toInt())
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(primaryState.isScrollInProgress, secondaryState.isScrollInProgress) {
                    if (!primaryState.isScrollInProgress && !secondaryState.isScrollInProgress) {
                        driver = 0
                    } else if (primaryState.isScrollInProgress && !secondaryState.isScrollInProgress) {
                        driver = 1
                    } else if (secondaryState.isScrollInProgress && !primaryState.isScrollInProgress) {
                        driver = 2
                    }
                }
            }

            @Composable
            fun RenderChapter(isPrimary: Boolean, state: LazyListState, helper: DatabaseHelper?, modifier: Modifier) {
                ChapterView(
                    passage = passage,
                    content = if (isPrimary) primaryContent else secondaryContent,
                    themeColors = themeColors,
                    currentFontFamily = currentFontFamily,
                    viewModel = viewModel,
                    isCurrentPage = isCurrentPage,
                    targetVerse = targetVerse,
                    versionAbbr = if (isPrimary) viewModel.currentVersionAbbr else viewModel.secondaryVersionAbbr,
                    isPrimary = isPrimary,
                    lazyState = state,
                    modifier = modifier,
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
                    onVerseLongPress = { v, p -> onVerseLongPress(v, p, isPrimary) },
                    databaseHelper = helper,
                    crossRefHelper = crossRefHelper,
                    onCrossRefClick = onCrossRefClick,
                    refreshKey = refreshKey,
                    onVerseCommentaryClick = onVerseCommentaryClick,
                    markerColor = markerColor,
                    onWordHighlightAction = onWordHighlightAction
                )
            }

            if (viewModel.multiViewLayout == "horizontal") {
                Row(Modifier.fillMaxSize()) {
                    RenderChapter(true, primaryState, databaseHelper, Modifier.weight(1f))
                    RenderChapter(false, secondaryState, secondaryDatabaseHelper, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    RenderChapter(true, primaryState, databaseHelper, Modifier.weight(1f))
                    RenderChapter(false, secondaryState, secondaryDatabaseHelper, Modifier.weight(1f))
                }
            }
        }
    }
}
@Composable
private fun IndependentMultiVersionReaderLazy(
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
    onWordHighlightAction: (() -> Unit)?
) {
    val primaryConfig = rememberChapterPagerConfig(primaryCurrent)
    val secondaryConfig = rememberChapterPagerConfig(secondaryCurrent)

    LaunchedEffect(primaryCurrent, databaseHelper) {
        preloadChapter(primaryCurrent, primaryLoadedVerses, databaseHelper, subheadingsDbHelper)
        if (primaryConfig.hasPrev) preloadChapter(primaryConfig.passages.first(), primaryLoadedVerses, databaseHelper, subheadingsDbHelper)
        if (primaryConfig.hasNext) preloadChapter(primaryConfig.passages.last(), primaryLoadedVerses, databaseHelper, subheadingsDbHelper)
    }

    LaunchedEffect(secondaryCurrent, secondaryDatabaseHelper) {
        preloadChapter(secondaryCurrent, secondaryLoadedVerses, secondaryDatabaseHelper, subheadingsDbHelper)
        if (secondaryConfig.hasPrev) preloadChapter(secondaryConfig.passages.first(), secondaryLoadedVerses, secondaryDatabaseHelper, subheadingsDbHelper)
        if (secondaryConfig.hasNext) preloadChapter(secondaryConfig.passages.last(), secondaryLoadedVerses, secondaryDatabaseHelper, subheadingsDbHelper)
    }

    if (viewModel.multiViewLayout == "horizontal") {
        Row(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { scheduleFade() } }) {
            ChapterPager(config = primaryConfig, modifier = Modifier.weight(1f), scheduleFade = scheduleFade, onPassageChange = onPrimaryPassageChange) { _, passage, isCurrentPage ->
                val content = primaryLoadedVerses[passage.bookNumber to passage.chapter] ?: emptyList()
                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) LoadingIndicator() else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = passage, content = content, themeColors = themeColors,
                            currentFontFamily = currentFontFamily, viewModel = viewModel,
                            isCurrentPage = isCurrentPage, targetVerse = targetVerse,
                            versionAbbr = viewModel.currentVersionAbbr, isPrimary = true,
                            lazyState = state, modifier = Modifier.fillMaxSize(),
                            onWordPress = onWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, true) },
                            databaseHelper = databaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction
                        )
                    }
                }
            }
            ChapterPager(config = secondaryConfig, modifier = Modifier.weight(1f), scheduleFade = scheduleFade, onPassageChange = onSecondaryPassageChange) { _, passage, isCurrentPage ->
                val content = secondaryLoadedVerses[passage.bookNumber to passage.chapter] ?: emptyList()
                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) LoadingIndicator() else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = passage, content = content, themeColors = themeColors,
                            currentFontFamily = currentFontFamily, viewModel = viewModel,
                            isCurrentPage = isCurrentPage, targetVerse = secondaryTargetVerse,
                            versionAbbr = viewModel.secondaryVersionAbbr, isPrimary = false,
                            lazyState = state, modifier = Modifier.fillMaxSize(),
                            onWordPress = onWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, false) },
                            databaseHelper = secondaryDatabaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction
                        )
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { scheduleFade() } }) {
            ChapterPager(config = primaryConfig, modifier = Modifier.weight(1f), scheduleFade = scheduleFade, onPassageChange = onPrimaryPassageChange) { _, passage, isCurrentPage ->
                val content = primaryLoadedVerses[passage.bookNumber to passage.chapter] ?: emptyList()
                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) LoadingIndicator() else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = passage, content = content, themeColors = themeColors,
                            currentFontFamily = currentFontFamily, viewModel = viewModel,
                            isCurrentPage = isCurrentPage, targetVerse = targetVerse,
                            versionAbbr = viewModel.currentVersionAbbr, isPrimary = true,
                            lazyState = state, modifier = Modifier.fillMaxSize(),
                            onWordPress = onWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, true) },
                            databaseHelper = databaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction
                        )
                    }
                }
            }
            ChapterPager(config = secondaryConfig, modifier = Modifier.weight(1f), scheduleFade = scheduleFade, onPassageChange = onSecondaryPassageChange) { _, passage, isCurrentPage ->
                val content = secondaryLoadedVerses[passage.bookNumber to passage.chapter] ?: emptyList()
                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) LoadingIndicator() else {
                        val state = rememberLazyListState()
                        ChapterView(
                            passage = passage, content = content, themeColors = themeColors,
                            currentFontFamily = currentFontFamily, viewModel = viewModel,
                            isCurrentPage = isCurrentPage, targetVerse = secondaryTargetVerse,
                            versionAbbr = viewModel.secondaryVersionAbbr, isPrimary = false,
                            lazyState = state, modifier = Modifier.fillMaxSize(),
                            onWordPress = onWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, false) },
                            databaseHelper = secondaryDatabaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction
                        )
                    }
                }
            }
        }
    }
}