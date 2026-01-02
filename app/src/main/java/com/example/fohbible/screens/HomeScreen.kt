package com.example.fohbible.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fohbible.MainActivity
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.Verse
import com.example.fohbible.ui.theme.FohBibleTheme

// Data classes for HomeScreen
data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

data class RecentReading(
    val title: String,
    val preview: String
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onBibleClick: () -> Unit,
    databaseHelper: DatabaseHelper? = null
) {
    val context = LocalContext.current
    var dailyVerses by remember { mutableStateOf<List<Verse>?>(null) }

    // Load random verses on first composition
    LaunchedEffect(Unit) {
        if (dailyVerses == null) {
            loadRandomVerses(context, databaseHelper) { verses ->
                dailyVerses = verses
            }
        }
    }

    val recentReadings = listOf(
        RecentReading("Psalm 23", "The Lord is my shepherd..."),
        RecentReading("Matthew 6:9-13", "The Lord's Prayer"),
        RecentReading("1 Corinthians 13", "The Love Chapter")
    )

    val quickActions = listOf(
        QuickAction("Read Bible", Icons.Filled.Book, color = MaterialTheme.colorScheme.primary),
        QuickAction("Audio Bible", Icons.AutoMirrored.Filled.VolumeUp, color = MaterialTheme.colorScheme.primary),
        QuickAction("Reading Plan", Icons.Filled.History, color = MaterialTheme.colorScheme.primary),
        QuickAction("Bookmarks", Icons.Filled.Bookmark, color = MaterialTheme.colorScheme.primary)
    )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            DailyVerseCard(
                verses = dailyVerses,
                onRefresh = {
                    loadRandomVerses(context, databaseHelper) { verses ->
                        dailyVerses = verses
                    }
                }
            )
        }

        item {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            QuickActionsGrid(actions = quickActions, onBibleClick = onBibleClick)
        }

        item {
            RecentReadingsSection(readings = recentReadings)
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DailyVerseCard(
    verse: String = "",
    verses: List<Verse>? = null,
    onRefresh: () -> Unit = {}
) {
    val isLoading = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fresh Revelations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = {
                        isLoading.value = true
                        onRefresh()
                        // Reset loading state after a delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            isLoading.value = false
                        }, 500)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (verses != null && verses.isNotEmpty()) {
                val firstVerse = verses.first()
                val reference = if (verses.size == 1) {
                    "${firstVerse.bookName ?: ""} ${firstVerse.chapter ?: 0}:${firstVerse.verseNumber}"
                } else {
                    val lastVerse = verses.last()
                    "${firstVerse.bookName ?: ""} ${firstVerse.chapter ?: 0}:${firstVerse.verseNumber}-${lastVerse.verseNumber}"
                }

                Text(
                    text = reference,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                verses.forEach { verse ->
                    Text(
                        text = "${verse.verseNumber}. ${verse.text}",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            } else if (verse.isNotEmpty()) {
                // Fallback to hardcoded verse if no random verses
                Text(
                    text = verse,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Justify
                )
            } else {
                // Loading state
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        // Save to bookmarks
                        verses?.let {
                            // Implement bookmark saving logic here
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Share",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            // Share verse
                            verses?.let {
                                val shareText = buildString {
                                    it.forEach { verse ->
                                        append("${verse.bookName ?: ""} ${verse.chapter ?: 0}:${verse.verseNumber} ${verse.text}\n")
                                    }
                                }
                                // Implement share logic
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    actions: List<QuickAction>,
    onBibleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        actions.forEach { action ->
            QuickActionItem(action = action, onClick = {
                if (action.title == "Read Bible") onBibleClick()
            })
        }
    }
}

@Composable
fun QuickActionItem(action: QuickAction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = action.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentReadingsSection(readings: List<RecentReading>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Readings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See All",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* Navigate to all readings */ }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        readings.forEachIndexed { index, reading ->
            RecentReadingItem(reading = reading)
            if (index < readings.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun RecentReadingItem(reading: RecentReading) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to Reader */ }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = reading.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = reading.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { /* Bookmark */ }) {
            Icon(
                Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Helper function to load random verses
private fun loadRandomVerses(
    context: android.content.Context,
    databaseHelper: DatabaseHelper?,
    onComplete: (List<Verse>) -> Unit
) {
    if (databaseHelper != null) {
        // Use existing database helper
        Thread {
            val verses = databaseHelper.getRandomVerses()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    } else {
        // Create new database helper
        Thread {
            val dbHelper = DatabaseHelper(context as MainActivity)
            val verses = dbHelper.getRandomVerses()
            dbHelper.close()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FohBibleTheme {
        HomeScreen(onBibleClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun DailyVerseCardPreview() {
    FohBibleTheme {
        DailyVerseCard(verse = "For God so loved the world...")
    }
}