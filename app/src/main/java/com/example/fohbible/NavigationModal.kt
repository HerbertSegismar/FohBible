package com.example.fohbible

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fohbible.ui.theme.FohBibleTheme

data class Book(val book_number: Int, val long_name: String, val short_name: String, val testament: String)
data class ChapterInfo(val chapter: Int, val verseCount: Int)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationModal(onDismissRequest: () -> Unit) {
    // Data - TODO: Replace with actual data from a database
    val oldTestamentBooks = remember { (1..39).map { Book(it, "Book $it", "B$it", "OT") } }
    val newTestamentBooks = remember { (40..66).map { Book(it, "Book $it", "B$it", "NT") } }

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
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentView == "books") {
                    item {
                        Text(
                            "Old Testament",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            oldTestamentBooks.forEach { book ->
                                BookCard(book = book, onClick = { selectedBook = book })
                            }
                        }
                    }
                    item {
                        Text(
                            "New Testament",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            newTestamentBooks.forEach { book ->
                                BookCard(book = book, onClick = { selectedBook = book })
                            }
                        }
                    }
                } else { // chapters view
                    item {
                        Text(
                            selectedBook!!.long_name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    item {
                        Text(
                            "Select Chapter",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chapters.forEach { chapterInfo ->
                                ChapterChip(
                                    chapterInfo = chapterInfo,
                                    onClick = {
                                        selectedChapter = it
                                        // TODO: handle navigation with selected book and chapter
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(book: Book, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(80.dp)) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(book.short_name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterChip(chapterInfo: ChapterInfo, onClick: (Int) -> Unit) {
    Card(onClick = { onClick(chapterInfo.chapter) }) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${chapterInfo.chapter}", fontWeight = FontWeight.Bold)
            Text("${chapterInfo.verseCount} vs", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationModalPreview() {
    FohBibleTheme {
        NavigationModal(onDismissRequest = {})
    }
}
