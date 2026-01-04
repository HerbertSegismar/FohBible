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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fohbible.data.BibleBook
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.BibleRepository
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.data.Verse
import com.example.fohbible.ui.theme.FohBibleTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    passage: PassageSelection,
    @Suppress("unused") databaseHelper: DatabaseHelper? = null,
    onPassageChange: (PassageSelection) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { BibleRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    // Track current passage - initialize with the passed passage
    var currentPassage by remember {
        mutableStateOf(passage.copy(verse = 1))
    }

    // Get current book info
    val currentBook by remember(currentPassage.bookNumber) {
        derivedStateOf { BibleData.getBookByCustomNumber(currentPassage.bookNumber) }
    }

    // Calculate previous and next passages
    val prevPassage by remember(currentPassage, currentBook) {
        derivedStateOf {
            if (currentBook == null) currentPassage
            else getPreviousPassage(currentPassage, currentBook)
        }
    }

    val nextPassage by remember(currentPassage, currentBook) {
        derivedStateOf {
            if (currentBook == null) currentPassage
            else getNextPassage(currentPassage, currentBook)
        }
    }

    val hasPrev by remember(prevPassage) {
        derivedStateOf { prevPassage != currentPassage }
    }
    val hasNext by remember(nextPassage) {
        derivedStateOf { nextPassage != currentPassage }
    }

    // FIX: When passage changes from parent, update currentPassage immediately
    // instead of in a LaunchedEffect
    if (passage.bookNumber != currentPassage.bookNumber ||
        passage.chapter != currentPassage.chapter) {
        currentPassage = passage.copy(verse = 1)
    }

    // Track target passage for swipe completion
    var pendingPassageChange by remember { mutableStateOf<PassageSelection?>(null) }

    // Track loaded verses
    val loadedVerses = remember { mutableStateMapOf<Pair<Int, Int>, List<Verse>>() }

    // Load verses whenever the passage changes
    LaunchedEffect(currentPassage) {
        // Load current passage
        val currentKey = currentPassage.bookNumber to currentPassage.chapter
        if (currentKey !in loadedVerses) {
            loadedVerses[currentKey] = repository.getVerses(currentPassage.bookNumber, currentPassage.chapter)
        }

        // Load previous passage if available
        if (hasPrev) {
            val prevKey = prevPassage.bookNumber to prevPassage.chapter
            if (prevKey !in loadedVerses) {
                loadedVerses[prevKey] = repository.getVerses(prevPassage.bookNumber, prevPassage.chapter)
            }
        }

        // Load next passage if available
        if (hasNext) {
            val nextKey = nextPassage.bookNumber to nextPassage.chapter
            if (nextKey !in loadedVerses) {
                loadedVerses[nextKey] = repository.getVerses(nextPassage.bookNumber, nextPassage.chapter)
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 3 }
    )

    // Track when user starts and ends a swipe
    var isUserSwiping by remember { mutableStateOf(false) }

    // Listen to drag state changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { isScrolling ->
            isUserSwiping = isScrolling

            // If swipe just ended and we have a pending passage change
            if (!isScrolling && pendingPassageChange != null) {
                val targetPassage = pendingPassageChange

                // Apply the passage change after swipe completes
                targetPassage?.let { newPassage ->
                    // FIX: Update currentPassage first, which will trigger recomputation
                    // of prevPassage and nextPassage
                    currentPassage = newPassage
                    onPassageChange(newPassage)

                    // Reset pager to center
                    coroutineScope.launch {
                        pagerState.scrollToPage(1)
                    }
                }
            }
        }
    }

    // Handle page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            // Only process if user is swiping (not programmatic scroll)
            if (isUserSwiping) {
                when (page) {
                    0 -> { // Swiped to previous
                        if (hasPrev && prevPassage != currentPassage) {
                            pendingPassageChange = prevPassage
                        } else {
                            // Reset to center if no previous chapter
                            coroutineScope.launch {
                                pagerState.scrollToPage(1)
                            }
                        }
                    }
                    2 -> { // Swiped to next
                        if (hasNext && nextPassage != currentPassage) {
                            pendingPassageChange = nextPassage
                        } else {
                            // Reset to center if no next chapter
                            coroutineScope.launch {
                                pagerState.scrollToPage(1)
                            }
                        }
                    }
                }
            }
        }
    }

    // Reset pager to center when currentPassage changes
    LaunchedEffect(currentPassage) {
        if (!isUserSwiping) {
            coroutineScope.launch {
                pagerState.scrollToPage(1)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { pageIndex ->
                val passageKey = when (pageIndex) {
                    0 -> prevPassage
                    1 -> currentPassage
                    2 -> nextPassage
                    else -> currentPassage
                }
                "${passageKey.bookNumber}-${passageKey.chapter}-${pageIndex}"
            }
        ) { pageIndex ->
            val thisPassage = when (pageIndex) {
                0 -> if (hasPrev) prevPassage else currentPassage
                1 -> currentPassage
                2 -> if (hasNext) nextPassage else currentPassage
                else -> currentPassage
            }

            val thisVerses = loadedVerses[thisPassage.bookNumber to thisPassage.chapter] ?: emptyList()

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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        items(thisVerses) { verse ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${verse.verseNumber}.",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = verse.text,
                                    modifier = Modifier.weight(1f)
                                )
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
            // If no previous book, stay at first chapter of current book
            current.copy(chapter = currentBook.chapters, verse = 1)
        }
    } else {
        // Normal previous chapter within same book
        current.copy(chapter = current.chapter - 1, verse = 1)
    }
}

fun getNextPassage(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current

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
    FohBibleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ReaderScreen(
                passage = PassageSelection(
                    bookNumber = 10,
                    bookName = "Genesis",
                    chapter = 1,
                    verse = 1
                )
            )
        }
    }
}
