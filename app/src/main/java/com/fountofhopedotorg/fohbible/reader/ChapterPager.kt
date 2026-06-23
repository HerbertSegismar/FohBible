package com.fountofhopedotorg.fohbible.reader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.ChapterPagerConfig
import com.fountofhopedotorg.fohbible.data.PassageSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberChapterPagerConfig(current: PassageSelection): ChapterPagerConfig {
    val currentBook by remember(current.bookNumber) {
        derivedStateOf { BibleData.getBookByCustomNumber(current.bookNumber) }
    }
    val prev by remember(current, currentBook) {
        derivedStateOf { getPreviousChapter(current, currentBook) }
    }
    val next by remember(current, currentBook) {
        derivedStateOf { getNextChapter(current, currentBook) }
    }
    val hasPrev by remember(prev) { derivedStateOf { prev != current } }
    val hasNext by remember(next) { derivedStateOf { next != current } }
    val passages by remember(current, prev, next, hasPrev, hasNext) {
        derivedStateOf {
            buildList {
                if (hasPrev) add(prev)
                add(current)
                if (hasNext) add(next)
            }
        }
    }
    val currentOffset by remember(hasPrev) { derivedStateOf { if (hasPrev) 1 else 0 } }
    val pageCount by remember(passages) { derivedStateOf { passages.size } }
    return ChapterPagerConfig(passages, currentOffset, pageCount, hasPrev, hasNext)
}

@Composable
fun ChapterPager(
    config: ChapterPagerConfig,
    modifier: Modifier = Modifier,
    scheduleFade: () -> Unit,
    onPassageChange: (PassageSelection) -> Unit,
    content: @Composable (pageIndex: Int, passage: PassageSelection, isCurrentPage: Boolean) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = config.currentOffset, pageCount = { config.pageCount })
    val scope = rememberCoroutineScope()
    var isUserSwiping by remember { mutableStateOf(false) }
    var swipeCompleted by remember { mutableStateOf(false) }
    var pendingPassageChange by remember { mutableStateOf<PassageSelection?>(null) }
    var edgeMessage by remember { mutableStateOf<String?>(null) }
    var triggerKey by remember { mutableLongStateOf(0L) }

    val overscrollConnection = remember(config.hasPrev, config.hasNext, pagerState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput || source == NestedScrollSource.UserInput) {
                    val isAtStart = pagerState.currentPage == 0 && available.x > 20f && !config.hasPrev
                    val isAtEnd = pagerState.currentPage == config.pageCount - 1 && available.x < -20f && !config.hasNext

                    if (isAtStart || isAtEnd) {
                        edgeMessage = "Only one chapter available for this book"
                        triggerKey = System.currentTimeMillis()
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(edgeMessage) {
        if (edgeMessage != null) {
            delay(1500.milliseconds)
            edgeMessage = null
        }
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            isUserSwiping = true
            swipeCompleted = false
            val offset = config.currentOffset
            if (pagerState.currentPage < offset && config.hasPrev) {
                pendingPassageChange = config.passages.first()
            } else if (pagerState.currentPage > offset && config.hasNext) {
                pendingPassageChange = config.passages.last()
            }
        } else if (isUserSwiping) {
            isUserSwiping = false
            val target = pendingPassageChange
            if (target != null && !swipeCompleted) {
                swipeCompleted = true
                onPassageChange(target)
                pendingPassageChange = null
            } else {
                scope.launch { pagerState.scrollToPage(config.currentOffset) }
            }
        }
    }

    LaunchedEffect(config.passages[config.currentOffset]) {
        if (!isUserSwiping) {
            scope.launch { pagerState.scrollToPage(config.currentOffset) }
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .nestedScroll(overscrollConnection)
                .pointerInput(Unit) { detectTapGestures { scheduleFade() } },
            key = { pageIndex -> "${config.passages[pageIndex].bookNumber}-${config.passages[pageIndex].chapter}" }
        ) { pageIndex ->
            content(pageIndex, config.passages[pageIndex], pageIndex == config.currentOffset)
        }
        AnimatedVisibility(
            visible = edgeMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            edgeMessage?.let {
                FeedbackPill(text = it)
            }
        }
    }
}