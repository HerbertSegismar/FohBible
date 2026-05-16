package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.core.ChapterPager
import com.fountofhopedotorg.fohbible.core.ChapterView
import com.fountofhopedotorg.fohbible.core.preloadChapter
import com.fountofhopedotorg.fohbible.core.rememberChapterPagerConfig
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Quadruple
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.VerseContent
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun SingleVersionReader(
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
    onWordPress: (String) -> Unit,
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
                    onWordHighlightAction = onWordHighlightAction,
                    onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                    onColorLensClick = { viewModel.showColorThemeDialog = true },
                    onScreenChange = { screen ->
                        val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                        viewModel.navigateTo(targetScreen)
                    },
                    scrollSyncEnabled = viewModel.scrollSync
                )
            }
        }
    }
}

@Composable
fun SyncedMultiVersionReader(
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
    onPrimaryWordPress: (String) -> Unit,
    onSecondaryWordPress: (String) -> Unit,
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

    val onInitialScrollComplete = remember {
        {
            completedScrolls++
            if (completedScrolls == 2) {
                suppressSync = false
                completedScrolls = 0
            }
        }
    }

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
            val stableOnThemeToggle = remember { { viewModel.darkTheme = !viewModel.darkTheme } }
            val stableOnColorLensClick = remember { { viewModel.showColorThemeDialog = true } }
            val stableOnScreenChange = remember {
                { screen: Screen ->
                    val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                    viewModel.navigateTo(targetScreen)
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
                    onInitialScrollComplete = onInitialScrollComplete,
                    onWordPress = if (isPrimary) onPrimaryWordPress else onSecondaryWordPress,
                    onStrongsPress = onStrongsPress,
                    onTagPress = onTagPress,
                    onVerseLongPress = { v, p -> onVerseLongPress(v, p, isPrimary) },
                    databaseHelper = helper,
                    crossRefHelper = crossRefHelper,
                    onCrossRefClick = onCrossRefClick,
                    refreshKey = refreshKey,
                    onVerseCommentaryClick = onVerseCommentaryClick,
                    markerColor = markerColor,
                    onWordHighlightAction = onWordHighlightAction,
                    onThemeToggle = stableOnThemeToggle,
                    onColorLensClick = stableOnColorLensClick,
                    onScreenChange = stableOnScreenChange,
                    scrollSyncEnabled = viewModel.scrollSync
                )
            }

            if (viewModel.multiViewLayout == "horizontal") {
                Row(Modifier.fillMaxSize()) {
                    RenderChapter(true, primaryState, databaseHelper, Modifier.weight(1f))
                    VerticalDivider(color = MaterialTheme.colorScheme.secondary.copy(0.2f))
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
fun IndependentMultiVersionReader(
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
    onPrimaryWordPress: (String) -> Unit,
    onSecondaryWordPress: (String) -> Unit,
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
                            onWordPress = onPrimaryWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, true) },
                            databaseHelper = databaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction,
                            onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                            onColorLensClick = { viewModel.showColorThemeDialog = true },
                            onScreenChange = { screen ->
                                val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                viewModel.navigateTo(targetScreen)
                            },
                            scrollSyncEnabled = viewModel.scrollSync
                        )
                    }
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.secondary.copy(0.2f))
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
                            onWordPress = onSecondaryWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, false) },
                            databaseHelper = secondaryDatabaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction,
                            onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                            onColorLensClick = { viewModel.showColorThemeDialog = true },
                            onScreenChange = { screen ->
                                val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                viewModel.navigateTo(targetScreen)
                            },
                            scrollSyncEnabled = viewModel.scrollSync
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
                            onWordPress = onPrimaryWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, true) },
                            databaseHelper = databaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction,
                            onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                            onColorLensClick = { viewModel.showColorThemeDialog = true },
                            onScreenChange = { screen ->
                                val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                viewModel.navigateTo(targetScreen)
                            },
                            scrollSyncEnabled = viewModel.scrollSync
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
                            onWordPress = onSecondaryWordPress, onStrongsPress = onStrongsPress, onTagPress = onTagPress,
                            onVerseLongPress = { verse, p -> onVerseLongPress(verse, p, false) },
                            databaseHelper = secondaryDatabaseHelper, crossRefHelper = crossRefHelper,
                            onCrossRefClick = onCrossRefClick, refreshKey = refreshKey,
                            onVerseCommentaryClick = onVerseCommentaryClick,
                            markerColor = markerColor, onWordHighlightAction = onWordHighlightAction,
                            onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                            onColorLensClick = { viewModel.showColorThemeDialog = true },
                            onScreenChange = { screen ->
                                val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                viewModel.navigateTo(targetScreen)
                            },
                            scrollSyncEnabled = viewModel.scrollSync
                        )
                    }
                }
            }
        }
    }
}