package com.example.fohbible.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fohbible.ui.theme.FohBibleTheme

data class Verse(
    val verseNumber: Int,
    val text: String
)

data class BiblePassage(
    val bookName: String,
    val chapterNumber: Int,
    val verses: List<Verse>
)

@Composable
fun ReaderScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    passage: BiblePassage? = null
) {
    // State for bookmark status
    var isBookmarked by remember { mutableStateOf(false) }

    // State for text size
    var textSize by remember { mutableStateOf(18.sp) }

    // Use sample data if no passage is provided
    val currentPassage = passage ?: samplePassage

    Scaffold(
        topBar = {
            ReaderTopAppBar(
                bookName = currentPassage.bookName,
                chapterNumber = currentPassage.chapterNumber,
                isBookmarked = isBookmarked,
                onBookmarkToggle = { isBookmarked = !isBookmarked },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            ReaderFloatingActions(
                textSize = textSize,
                onIncreaseTextSize = {
                    textSize = (textSize.value + 1).sp
                },
                onDecreaseTextSize = {
                    val newValue = textSize.value - 1
                    textSize = if (newValue >= 12) newValue.sp else 12.sp
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                PassageHeader(
                    bookName = currentPassage.bookName,
                    chapterNumber = currentPassage.chapterNumber,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(currentPassage.verses) { verse ->
                VerseItem(
                    verse = verse,
                    textSize = textSize,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                ChapterNavigation(
                    currentChapter = currentPassage.chapterNumber,
                    onPreviousChapter = { /* Will be implemented later */ },
                    onNextChapter = { /* Will be implemented later */ },
                    modifier = Modifier.padding(16.dp)
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopAppBar(
    bookName: String,
    chapterNumber: Int,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = bookName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chapter $chapterNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onBookmarkToggle) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove Bookmark" else "Add Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { /* Share functionality */ }) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share"
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun PassageHeader(
    bookName: String,
    chapterNumber: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = bookName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chapter $chapterNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
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
        // Verse number badge
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

        // Verse text
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

@Composable
fun ChapterNavigation(
    currentChapter: Int,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPreviousChapter,
                enabled = currentChapter > 1,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Previous")
            }

            Text(
                text = "Chapter $currentChapter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = onNextChapter,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun ReaderFloatingActions(
    textSize: androidx.compose.ui.unit.TextUnit,
    onIncreaseTextSize: () -> Unit,
    onDecreaseTextSize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Text size controls
        ElevatedCard(
            modifier = Modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.TextFields,
                    contentDescription = "Text Size",
                    modifier = Modifier.size(20.dp)
                )

                IconButton(
                    onClick = onDecreaseTextSize,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("A", fontSize = 14.sp)
                }

                Text(
                    text = "${textSize.value.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onIncreaseTextSize,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Sample data for preview and development
private val samplePassage = BiblePassage(
    bookName = "John",
    chapterNumber = 3,
    verses = listOf(
        Verse(1, "Now there was a man of the Pharisees named Nicodemus, a ruler of the Jews."),
        Verse(2, "This man came to Jesus by night and said to him, 'Rabbi, we know that you are a teacher come from God, for no one can do these signs that you do unless God is with him.'"),
        Verse(3, "Jesus answered him, 'Truly, truly, I say to you, unless one is born again he cannot see the kingdom of God.'"),
        Verse(4, "Nicodemus said to him, 'How can a man be born when he is old? Can he enter a second time into his mother's womb and be born?'"),
        Verse(5, "Jesus answered, 'Truly, truly, I say to you, unless one is born of water and the Spirit, he cannot enter the kingdom of God.'"),
        Verse(6, "'That which is born of the flesh is flesh, and that which is born of the Spirit is spirit.'"),
        Verse(7, "'Do not marvel that I said to you, You must be born again.'"),
        Verse(8, "'The wind blows where it wishes, and you hear its sound, but you do not know where it comes from or where it goes. So it is with everyone who is born of the Spirit.'"),
        Verse(9, "Nicodemus said to him, 'How can these things be?'"),
        Verse(10, "Jesus answered him, 'Are you the teacher of Israel and yet you do not understand these things?'"),
        Verse(11, "'Truly, truly, I say to you, we speak of what we know, and bear witness to what we have seen, but you do not receive our testimony.'"),
        Verse(12, "'If I have told you earthly things and you do not believe, how can you believe if I tell you heavenly things?'"),
        Verse(13, "'No one has ascended into heaven except he who descended from heaven, the Son of Man.'"),
        Verse(14, "'And as Moses lifted up the serpent in the wilderness, so must the Son of Man be lifted up,'"),
        Verse(15, "'that whoever believes in him may have eternal life.'"),
        Verse(16, "'For God so loved the world, that he gave his only Son, that whoever believes in him should not perish but have eternal life.'"),
        Verse(17, "'For God did not send his Son into the world to condemn the world, but in order that the world might be saved through him.'"),
        Verse(18, "'Whoever believes in him is not condemned, but whoever does not believe is condemned already, because he has not believed in the name of the only Son of God.'"),
        Verse(19, "'And this is the judgment: the light has come into the world, and people loved the darkness rather than the light because their works were evil.'"),
        Verse(20, "'For everyone who does wicked things hates the light and does not come to the light, lest his works should be exposed.'"),
        Verse(21, "'But whoever does what is true comes to the light, so that it may be clearly seen that his works have been carried out in God.'")
    )
)

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    FohBibleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ReaderScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerseItemPreview() {
    FohBibleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            VerseItem(
                verse = Verse(16, "For God so loved the world, that he gave his only Son, that whoever believes in him should not perish but have eternal life."),
                textSize = 18.sp
            )
        }
    }
}