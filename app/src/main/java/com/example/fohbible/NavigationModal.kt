package com.example.fohbible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fohbible.ui.theme.FohBibleTheme

// Data class for UI representation
data class BookUi(
    val bookNumber: Int,
    val longName: String,
    val shortName: String,
    val testament: Testament,
    val totalChapters: Int
)

// Data class for chapter information
data class ChapterInfo(
    val chapter: Int,
    val verseCount: Int
)

// Conversion from BibleBook to BookUi
fun BibleBook.toBookUi(): BookUi {
    return BookUi(
        bookNumber = number,
        longName = name,
        shortName = abbreviation,
        testament = testament,
        totalChapters = chapters
    )
}

fun Color.lighten(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[2] = (hsv[2] + amount).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationModal(
    onDismissRequest: () -> Unit,
    onPassageSelected: (bookName: String, chapter: Int) -> Unit = { _, _ -> }
) {
    // Data from BibleData
    val oldTestamentBooks = remember { BibleData.oldTestamentBooks.map { it.toBookUi() } }
    val newTestamentBooks = remember { BibleData.newTestamentBooks.map { it.toBookUi() } }

    // State
    var selectedBook by remember { mutableStateOf<BookUi?>(null) }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }

    // Derived states
    val currentView by remember { derivedStateOf { if (selectedBook == null) "books" else "chapters" } }

    // Get actual chapter data for selected book
    val chapters = remember(selectedBook) {
        selectedBook?.let { bookUi ->
            val bibleBook = BibleData.getBookByNumber(bookUi.bookNumber)
            if (bibleBook != null) {
                (1..bibleBook.chapters).map { chapterNum ->
                    ChapterInfo(
                        chapter = chapterNum,
                        verseCount = bibleBook.getVersesForChapter(chapterNum)
                    )
                }
            } else {
                emptyList()
            }
        } ?: emptyList()
    }

    // Calculate chapter chunks once, outside LazyColumn
    val chapterChunks by remember(chapters) {
        derivedStateOf {
            chapters.chunked(5)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentView) {
                                    "books" -> "Select a Book"
                                    else -> "Select a Chapter"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (currentView == "chapters") {
                                        selectedBook = null
                                        selectedChapter = null
                                    } else {
                                        onDismissRequest()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                    ) {
                        if (currentView == "books") {
                            item {
                                QuickJumpSection(
                                    onTestamentSelected = { testament ->
                                        // In a real implementation, you might scroll to the testament section
                                        // You would need to add a LazyListState and scroll to the appropriate index
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            item {
                                TestamentSection(
                                    title = "Old Testament",
                                    books = oldTestamentBooks,
                                    onBookSelected = { book ->
                                        selectedBook = book
                                    },
                                    defaultColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    textColor = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            item {
                                TestamentSection(
                                    title = "New Testament",
                                    books = newTestamentBooks,
                                    onBookSelected = { book ->
                                        selectedBook = book
                                    },
                                    defaultColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    textColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            selectedBook?.let { book ->
                                item {
                                    BookHeader(book = book)
                                }

                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                item {
                                    Text(
                                        text = "Chapters",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                itemsIndexed(
                                    items = chapterChunks,
                                    key = { index, row -> index } // Use index as key for stability
                                ) { index, rowChapters ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowChapters.forEach { chapterInfo ->
                                            ChapterChip(
                                                chapterInfo = chapterInfo,
                                                isSelected = selectedChapter == chapterInfo.chapter,
                                                onClick = { chapterNum ->
                                                    selectedChapter = if (selectedChapter == chapterNum) null else chapterNum
                                                }
                                            )
                                        }
                                    }
                                }

                                if (selectedChapter != null) {
                                    item {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = {
                                                onPassageSelected(
                                                    book.longName,
                                                    selectedChapter!!
                                                )
                                                onDismissRequest()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "Read ${book.longName} $selectedChapter",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickJumpSection(onTestamentSelected: (Testament) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick Jump",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onTestamentSelected(Testament.OLD) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Old Testament", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = { onTestamentSelected(Testament.NEW) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("New Testament", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun BookHeader(book: BookUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when (book.testament) {
            Testament.OLD -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            Testament.NEW -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = book.longName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = when (book.testament) {
                        Testament.OLD -> MaterialTheme.colorScheme.primary
                        Testament.NEW -> MaterialTheme.colorScheme.secondary
                    }
                )
                Text(
                    text = "${book.totalChapters} chapters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (book.testament) {
                            Testament.OLD -> MaterialTheme.colorScheme.primary
                            Testament.NEW -> MaterialTheme.colorScheme.secondary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.shortName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestamentSection(
    title: String,
    books: List<BookUi>,
    onBookSelected: (BookUi) -> Unit,
    defaultColor: Color,
    textColor: Color
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${books.size} books",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            books.forEach { book ->
                BookCard(
                    book = book,
                    backgroundColor = defaultColor,
                    textColor = textColor,
                    onClick = { onBookSelected(book) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(
    book: BookUi,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 60.dp, max = 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = book.shortName,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Chapter count
            Text(
                text = "${book.totalChapters} ch",
                color = textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterChip(
    chapterInfo: ChapterInfo,
    isSelected: Boolean,
    onClick: (Int) -> Unit
) {
    Surface(
        onClick = { onClick(chapterInfo.chapter) },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (isSelected) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = chapterInfo.chapter.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = "${chapterInfo.verseCount} vs",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Navigation Modal Light")
@Composable
fun NavigationModalPreviewLight() {
    FohBibleTheme(darkTheme = false) {
        NavigationModal(onDismissRequest = {})
    }
}

@Preview(showBackground = true, name = "Navigation Modal Dark")
@Composable
fun NavigationModalPreviewDark() {
    FohBibleTheme(darkTheme = true) {
        NavigationModal(onDismissRequest = {})
    }
}