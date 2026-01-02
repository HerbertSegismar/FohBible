package com.example.fohbible.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fohbible.data.BibleRepository
import com.example.fohbible.data.BibleViewModel
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.ui.theme.FohBibleTheme

data class Verse(
    val verseNumber: Int,
    val text: String
)

@Composable
fun ReaderScreen(
    passage: PassageSelection?,
    databaseHelper: DatabaseHelper? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val repository = remember { BibleRepository(context) }
    val viewModel = remember { BibleViewModel(repository) }
    val verses by viewModel.verses.collectAsState()

    LaunchedEffect(passage) {
        passage?.let {
            viewModel.loadVerses(it.bookNumber, it.chapter)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            passage == null -> {
                Text("No passage selected")
            }
            verses.isEmpty() -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading verses...")
                    Text("Book: ${passage.bookNumber}, Chapter: ${passage.chapter}")
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${passage.bookName} Chapter ${passage.chapter}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(verses) { verse ->
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

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
fun VerseItem(
    verse: Verse,
    textSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = verse.verseNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = verse.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = textSize,
                lineHeight = (textSize.value * 1.5).sp
            ),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Justify
        )
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
                ),
                onNavigateBack = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerseItemPreview() {
    FohBibleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                VerseItem(
                    verse = Verse(16, "For God so loved the world, that he gave his only Son, that whoever believes in him should not perish but have eternal life."),
                    textSize = 18.sp
                )
            }
        }
    }
}