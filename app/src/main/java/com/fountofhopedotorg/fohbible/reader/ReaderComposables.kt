package com.fountofhopedotorg.fohbible.reader

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.allScreens
import com.fountofhopedotorg.fohbible.app_composables.AnimatedIconButton
import com.fountofhopedotorg.fohbible.app_composables.DropdownMenuItemWithIcon
import com.fountofhopedotorg.fohbible.app_composables.FontSizeControls
import com.fountofhopedotorg.fohbible.app_composables.LoadingIndicator
import com.fountofhopedotorg.fohbible.app_composables.OverlayOpacitySlider
import com.fountofhopedotorg.fohbible.color_wheel.ColorPickerRow
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Quadruple
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.VerseContent
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppBar(
    currentScreen: Screen.Reader,
    currentVersionAbbr: String,
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScreenChange: (Screen) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: AppViewModel = viewModel()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val topAppBarHeight = if (isLandscape) 40.dp else 80.dp
    val iconSize = 35.dp

    TopAppBar(
        title = {
            if (!viewModel.multiVersion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBibleIconClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .height(25.dp)
                            .weight(if (isLandscape) 2f else 1.2f)
                            .padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentScreen.passage?.bookName ?: "Reader",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = viewModel.headerButtonsColor,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = currentScreen.passage?.chapter?.let { " $it" } ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = viewModel.headerButtonsColor,
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.showPrimaryVersionDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(25.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = currentVersionAbbr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = viewModel.headerButtonsColor,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = modifier
            .height(topAppBarHeight)
            .background(
                Brush.verticalGradient(
                    0.6f to LocalAppTheme.current.primaryColor,
                    0.85f to LocalAppTheme.current.primaryColor,
                    1.0f to Color.Transparent
                )
            ),
        navigationIcon = {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick = {
                                onBack()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = viewModel.headerButtonsColor
                    )
                }
            }
        },
        actions = {
            Spacer(modifier = Modifier.width(if (isLandscape) 200.dp else 0.dp))
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                if (viewModel.multiVersion && !isLandscape|| !viewModel.multiVersion && isLandscape || isLandscape && viewModel.multiVersion && viewModel.multiViewLayout == "horizontal") {
                    IconButton(
                        onClick = { onScreenChange(Screen.Home) },
                        modifier = Modifier.size(iconSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = viewModel.headerButtonsColor,
                            modifier = Modifier.size(iconSize * 0.65f)
                        )
                    }
                    IconButton(
                        onClick = { onScreenChange(Screen.Bookmarks) },
                        modifier = Modifier.size(iconSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Bookmarks",
                            tint = viewModel.headerButtonsColor,
                            modifier = Modifier.size(iconSize * 0.55f)
                        )
                    }
                    IconButton(
                        onClick = { onScreenChange(Screen.Notes) },
                        modifier = Modifier.size(iconSize)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Note,
                            contentDescription = "Notes",
                            tint = viewModel.headerButtonsColor,
                            modifier = Modifier.size(iconSize * 0.55f).rotate(90f)
                        )
                    }
                    IconButton(
                        onClick = { onScreenChange(Screen.Search) },
                        modifier = Modifier.size(iconSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = viewModel.headerButtonsColor,
                            modifier = Modifier.size(iconSize * 0.7f)
                        )
                    }
                    IconButton(
                        onClick = { onScreenChange(Screen.Settings) },
                        modifier = Modifier.size(iconSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = viewModel.headerButtonsColor,
                            modifier = Modifier.size(iconSize * 0.6f)
                        )
                    }
                }
                if (!viewModel.multiVersion || !isLandscape && viewModel.multiViewLayout == "horizontal") {
                    AnimatedIconButton(
                        onClick = onThemeToggle,
                        icon = if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2,
                        contentDescription = "Toggle Theme",
                        modifier = Modifier.size(iconSize),
                        iconSize = iconSize * 0.6f,
                        rotation = 180f,
                        viewModel = viewModel
                    )
                    AnimatedIconButton(
                        onClick = onColorLensClick,
                        icon = Icons.Filled.ColorLens,
                        contentDescription = "Color Scheme",
                        modifier = Modifier.size(iconSize),
                        iconSize = iconSize * 0.6f,
                        rotation = 180f,
                        viewModel = viewModel
                    )
                    if (viewModel.multiVersion) {
                        ScrollSyncButton(
                            viewModel = viewModel,
                            containerSize = 35.dp,
                            iconScale = 0.6f
                        )
                    }
                    WindowsLayoutDropdown(
                        viewModel = viewModel,
                        modifier = Modifier.size(iconSize)
                    )
                    ReaderAppBarMenu(
                        isLandscape = isLandscape,
                        viewModel = viewModel,
                        onScreenChange = onScreenChange,
                        coroutineScope = rememberCoroutineScope(),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    )
}

@Composable
fun ReaderDropdownContent(
    isLandscape: Boolean,
    viewModel: AppViewModel,
    onScreenChange: (Screen) -> Unit,
    coroutineScope: CoroutineScope
) {
    val commonItems = @Composable {
        allScreens.forEach { (title, icon) ->
            val isActive = when (title) {
                "Reader" -> true
                else -> false
            }
            DropdownMenuItemWithIcon(
                title = title,
                icon = icon,
                isActive = isActive,
                onClick = {
                    val targetScreen = when (title) {
                        "Home" -> Screen.Home
                        "Reader" -> Screen.Reader()
                        "Bookmarks" -> Screen.Bookmarks
                        "Notes" -> Screen.Notes
                        "Search" -> Screen.Search
                        "Settings" -> Screen.Settings
                        else -> Screen.Home
                    }
                    onScreenChange(targetScreen)
                }
            )
        }
        if (!isLandscape) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Background Texture", modifier = Modifier.fillMaxWidth()) },
                leadingIcon = { Icon(Icons.Outlined.Texture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { viewModel.showBgModal = true }
            )
            HorizontalDivider()
            if (viewModel.bgImageIndex != 0) {
                OverlayOpacitySlider(viewModel)
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(text = if (viewModel.isDictionaryMode) "Dictionary Mode On" else "Word Marker On", modifier = Modifier.fillMaxWidth()) },
                leadingIcon = { Icon(if (viewModel.isDictionaryMode) Icons.AutoMirrored.Filled.Label else Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    viewModel.isDictionaryMode = !viewModel.isDictionaryMode
                    coroutineScope.launch { delay(400.milliseconds) }
                }
            )
            if (!viewModel.isDictionaryMode) {
                HorizontalDivider()
                ColorPickerRow(label = "Word Marker Color", iconSize = 22, color = viewModel.wordMarkerColor, onClick = { viewModel.showWordMarkerColorWheelDialog = true })
            }
            else {
                HorizontalDivider()
                ColorPickerRow(label = "Verse Marker Color", iconSize = 22, color = viewModel.verseMarkerColor, onClick = { viewModel.showVerseMarkerColorWheelDialog = true })
            }
            HorizontalDivider()
            ColorPickerRow(label = "Jesus' Words Color", iconSize = 22, color = viewModel.wordsOfJesus, onClick = { viewModel.showJesusWordsColorWheelDialog = true })
            HorizontalDivider()
            if (viewModel.darkTheme) {
                ColorPickerRow(
                    label = "Font Color",
                    iconSize = 22,
                    color = viewModel.darkThemeReaderFontColor,
                    onClick = { viewModel.showDarkReaderFontColorWheelDialog= true }
                )
            }
            else {
                ColorPickerRow(
                    label = "Font Color",
                    iconSize = 22,
                    color = viewModel.lightThemeReaderFontColor,
                    onClick = { viewModel.showLightReaderFontColorWheelDialog= true }
                )
            }
            HorizontalDivider()
            FontSizeControls(viewModel)
        }
    }
    if (isLandscape) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)) {
                commonItems()
                if (viewModel.bgImageIndex != 0) {
                    DropdownMenuItem(
                        text = { Text("Background Texture", modifier = Modifier.fillMaxWidth()) },
                        leadingIcon = { Icon(Icons.Outlined.Texture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { viewModel.showBgModal = true }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            VerticalDivider()
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)) {
                ExtraReaderControls(viewModel, coroutineScope)
            }
        }
    } else {
        Column { commonItems() }
    }
}
@Composable
fun ExtraReaderControls(viewModel: AppViewModel, coroutineScope: CoroutineScope) {
    if (viewModel.bgImageIndex != 0) {
        OverlayOpacitySlider(viewModel)
        HorizontalDivider()
    } else {
        DropdownMenuItem(
            text = { Text("Background Texture", modifier = Modifier.fillMaxWidth()) },
            leadingIcon = { Icon(Icons.Outlined.Texture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            onClick = { viewModel.showBgModal = true }
        )
        HorizontalDivider()
    }

    DropdownMenuItem(
        text = { Text(text = if (viewModel.isDictionaryMode) "Dictionary Mode On" else "Word Marker On", modifier = Modifier.fillMaxWidth()) },
        leadingIcon = { Icon(if (viewModel.isDictionaryMode) Icons.AutoMirrored.Filled.Label else Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        onClick = {
            viewModel.isDictionaryMode = !viewModel.isDictionaryMode
            coroutineScope.launch { delay(400.milliseconds) }
        }
    )
    if (!viewModel.isDictionaryMode) {
        HorizontalDivider()
        ColorPickerRow(label = "Word Marker Color", iconSize = 22, color = viewModel.wordMarkerColor, onClick = { viewModel.showWordMarkerColorWheelDialog = true })
    }
    else {
        HorizontalDivider()
        ColorPickerRow(label = "Verse Marker Color", iconSize = 22, color = viewModel.verseMarkerColor, onClick = { viewModel.showVerseMarkerColorWheelDialog = true })
    }
    HorizontalDivider()
    ColorPickerRow(label = "Jesus' Words Color", iconSize = 22, color = viewModel.wordsOfJesus, onClick = { viewModel.showJesusWordsColorWheelDialog = true })
    HorizontalDivider()
    if (viewModel.darkTheme) {
        ColorPickerRow(
            label = "Font Color",
            iconSize = 22,
            color = viewModel.darkThemeReaderFontColor,
            onClick = { viewModel.showDarkReaderFontColorWheelDialog= true }
        )
    }
    else {
        ColorPickerRow(
            label = "Font Color",
            iconSize = 22,
            color = viewModel.lightThemeReaderFontColor,
            onClick = { viewModel.showLightReaderFontColorWheelDialog= true }
        )
    }
    HorizontalDivider()
    FontSizeControls(viewModel)
}

@Composable
fun ReaderAppBarMenu(
    isLandscape: Boolean,
    viewModel: AppViewModel,
    onScreenChange: (Screen) -> Unit,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    tint: Color = viewModel.headerButtonsColor
) {
    val iconSize = 35.dp
    var showMenu by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (showMenu) 180f else 0f,
        animationSpec = tween(300),
        label = "menuIconRotation"
    )
    Box(modifier = modifier) {
        IconButton(
            modifier = Modifier.size(iconSize),
            onClick = { showMenu = !showMenu }
        ) {
            Crossfade(
                targetState = showMenu,
                animationSpec = tween(300),
                label = "iconCrossfade"
            ) { isOpen ->
                Icon(
                    imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Menu,
                    contentDescription = if (isOpen) "Close Navigation" else "Open Navigation",
                    tint = tint,
                    modifier = Modifier.size(iconSize * 0.65f).rotate(rotation)
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(
                if (viewModel.darkTheme) viewModel.darkModalBackgroundColor
                else viewModel.lightModalBackgroundColor
            )
        ) {
            ReaderDropdownContent(
                isLandscape = isLandscape,
                viewModel = viewModel,
                onScreenChange = onScreenChange,
                coroutineScope = coroutineScope
            )
        }
    }
}

@Composable
fun ScrollSyncButton(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    containerSize: Dp = 35.dp,
    iconScale: Float = 0.6f
) {
    val scope = rememberCoroutineScope()
    val pendingJob = remember { mutableStateOf<Job?>(null) }
    val icon = if (viewModel.scrollSync) Icons.Filled.Link else Icons.Filled.LinkOff

    AnimatedIconButton(
        onClick = {
            if (pendingJob.value?.isActive == true) return@AnimatedIconButton
            pendingJob.value = scope.launch {
                delay(250L.milliseconds)
                viewModel.scrollSync = !viewModel.scrollSync
                pendingJob.value = null
                viewModel.scrollSyncAction = true
            }
        },
        icon = icon,
        contentDescription = "Toggle Scroll Sync",
        modifier = modifier.then(Modifier.size(containerSize)),
        iconSize = containerSize * iconScale,
        rotation = 180f,
        viewModel = viewModel
    )
}

@Composable
fun FeedbackPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium
        )
    }
}