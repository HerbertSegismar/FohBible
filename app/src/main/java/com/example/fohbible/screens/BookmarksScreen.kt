package com.example.fohbible.screens

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fohbible.MainActivity
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.Verse
import com.example.fohbible.utils.SimpleVerseProcessor

@Composable
fun BookmarksScreen(
    databaseHelper: DatabaseHelper? = null
) {
    val context = LocalContext.current
    var bookmarkedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }

    LaunchedEffect(Unit) {
        loadBookmarks(context, databaseHelper) { verses ->
            bookmarkedVerses = verses
        }
    }

    if (bookmarkedVerses.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No bookmarks yet")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(bookmarkedVerses.size) { index ->
                BookmarkItem(
                    verse = bookmarkedVerses[index],
                    databaseHelper = databaseHelper ?: DatabaseHelper(context as MainActivity, "kj2.sqlite3"),
                    onRemove = {
                        bookmarkedVerses = bookmarkedVerses.filterIndexed { i, _ -> i != index }
                    }
                )
            }
        }
    }
}

@Composable
fun BookmarkItem(
    verse: Verse,
    databaseHelper: DatabaseHelper,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "${verse.bookName} ${verse.chapter}:${verse.verseNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                IconButton(
                    onClick = {
                        removeBookmark(verse, databaseHelper)
                        onRemove()
                    }
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove Bookmark",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val annotatedText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                ) {
                    append("${verse.verseNumber} ")
                }
                append(SimpleVerseProcessor.stripXmlTags(verse.text))
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                textAlign = TextAlign.Justify
            )
        }
    }
}

// Helper function to load bookmarks
private fun loadBookmarks(
    context: Context,
    databaseHelper: DatabaseHelper?,
    onComplete: (List<Verse>) -> Unit
) {
    if (databaseHelper != null) {
        Thread {
            val verses = databaseHelper.getBookmarks()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    } else {
        Thread {
            val dbHelper = DatabaseHelper(
                context as MainActivity,
                databaseName = "kj2.sqlite3"
            )
            val verses = dbHelper.getBookmarks()
            dbHelper.close()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    }
}

// Helper function to remove a bookmark
private fun removeBookmark(verse: Verse, databaseHelper: DatabaseHelper) {
    Thread {
        databaseHelper.removeBookmark(verse)
    }.start()
}