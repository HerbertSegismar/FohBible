package com.example.fohbible

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fohbible.ui.theme.FohBibleTheme

data class Book(val book_number: Int, val long_name: String, val short_name: String)
data class ChapterInfo(val chapter: Int, val verseCount: Int)

fun Color.lighten(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[2] = (hsv[2] + amount).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationModal(onDismissRequest: () -> Unit) {
    // Data - TODO: Replace with actual data from a database
    val oldTestamentBooks = remember { (1..39).map { Book(it, "Book $it", "B$it") } }
    val newTestamentBooks = remember { (40..66).map { Book(it, "Book $it", "B$it") } }

    // State
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    val chapters = remember(selectedBook) {
        if (selectedBook != null) (1..20).map { ChapterInfo(it, 30) } else emptyList()
    }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }

    val currentView by remember { derivedStateOf { if (selectedBook == null) "books" else "chapters" } }

    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Choose Passage to Read") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (currentView == "chapters") {
                                selectedBook = null
                            } else {
                                onDismissRequest()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                if (currentView == "books") {
                    item {
                        Text("Select Book", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 12.dp))
                    }
                    item {
                        TestamentSection(title = "Old Testament", books = oldTestamentBooks, onBookSelected = { selectedBook = it }, defaultColor = Color(0xFFDC2626))
                    }
                    item {
                        TestamentSection(title = "New Testament", books = newTestamentBooks, onBookSelected = { selectedBook = it }, defaultColor = Color(0xFF059669))
                    }
                } else { // chapters view
                    selectedBook?.let {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = "${it.long_name}${selectedChapter?.let { c -> " $c"} ?: ""}",
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        item {
                            Text("Select Chapter", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 12.dp))
                        }
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chapters.forEach { chapterInfo ->
                                    ChapterChip(
                                        chapterInfo = chapterInfo,
                                        isSelected = selectedChapter == chapterInfo.chapter,
                                        onClick = { chapter -> selectedChapter = chapter }
                                    )
                                }
                            }
                        }
                        if (selectedChapter != null) {
                            item {
                                Button(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp)) {
                                    Text(text = "Go to ${it.long_name} $selectedChapter")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestamentSection(title: String, books: List<Book>, onBookSelected: (Book) -> Unit, defaultColor: Color) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
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
                BookCard(book = book, color = defaultColor, onClick = { onBookSelected(book) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(book: Book, color: Color, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) color.lighten(0.3f) else color.lighten(0.85f)
    val textColor = if (cardColor.luminance() > 0.5) Color.Black else Color.White

    Card(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 56.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = book.short_name,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterChip(chapterInfo: ChapterInfo, isSelected: Boolean, onClick: (Int) -> Unit) {
    val selectedColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    val unselectedColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        onClick = { onClick(chapterInfo.chapter) },
        colors = if (isSelected) selectedColors else unselectedColors
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${chapterInfo.chapter}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${chapterInfo.verseCount} vs", style = MaterialTheme.typography.labelSmall)
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
