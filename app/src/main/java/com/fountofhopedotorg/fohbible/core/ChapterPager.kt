package com.fountofhopedotorg.fohbible.core

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.PassageSelection
import kotlinx.coroutines.launch

data class ChapterPagerConfig(
    val passages: List<PassageSelection>,
    val currentOffset: Int,
    val pageCount: Int,
    val hasPrev: Boolean,
    val hasNext: Boolean
)

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

    HorizontalPager(
        state = pagerState,
        modifier = modifier.pointerInput(Unit) { detectTapGestures { scheduleFade() } },
        key = { pageIndex -> "${config.passages[pageIndex].bookNumber}-${config.passages[pageIndex].chapter}" }
    ) { pageIndex ->
        val passage = config.passages[pageIndex]
        val isCurrentPage = pageIndex == config.currentOffset
        content(pageIndex, passage, isCurrentPage)
    }
}