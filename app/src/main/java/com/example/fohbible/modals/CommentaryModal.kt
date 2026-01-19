@file:Suppress("AssignedValueIsNeverRead")
package com.example.fohbible.modals

import android.os.Build
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection

data class ModalPage(val title: String, val content: String)

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun CommentaryModal(
    show: Boolean,
    onDismiss: () -> Unit,
    initialTitle: String,
    initialContent: String,
    databaseHelper: DatabaseHelper?,
    onBack: (() -> Unit)? = null // Kept for compatibility, but not used internally
) {
    if (show) {
        val stack = remember { mutableStateListOf<ModalPage>() }
        LaunchedEffect(true) {
            if (show) {
                stack.clear()
                stack.add(ModalPage(initialTitle, initialContent))
            }
        }
        if (stack.isEmpty()) return
        val currentPage = stack.last()
        val textColor = MaterialTheme.colorScheme.onBackground
        val linkColor = MaterialTheme.colorScheme.primary
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = currentPage.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            movementMethod = LinkMovementMethod.getInstance()
                        }
                    },
                    update = { textView ->
                        val spanned = HtmlCompat.fromHtml(currentPage.content, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        val spannable = SpannableString(spanned)
                        val urlSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
                        for (urlSpan in urlSpans) {
                            val start = spannable.getSpanStart(urlSpan)
                            val end = spannable.getSpanEnd(urlSpan)
                            val flags = spannable.getSpanFlags(urlSpan)
                            val href = urlSpan.url
                            if (href.startsWith("B:")) {
                                val linkText = spannable.substring(start, end)
                                val passage = parseVerseLink(href, linkText)
                                if (passage != null) {
                                    val clickableSpan = object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            val verseText = fetchVersesText(passage, databaseHelper)
                                            val newTitle = "${passage.bookName} ${passage.chapter}:${passage.verse}" +
                                                    if (passage.verseEnd != null) "-${passage.verseEnd}" else ""
                                            stack.add(ModalPage(newTitle, verseText))
                                        }
                                    }
                                    spannable.setSpan(clickableSpan, start, end, flags)
                                    spannable.removeSpan(urlSpan)
                                }
                            }
                        }
                        textView.text = spannable
                        textView.setTextColor(textColor.toArgb())
                        textView.setLinkTextColor(linkColor.toArgb())
                    },
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                )
            },
            confirmButton = {
                Row {
                    if (stack.size > 1) {
                        TextButton(onClick = { stack.removeLast() }) {
                            Text("Back")
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

private fun parseVerseLink(href: String, linkText: String): PassageSelection? {
    try {
        // Parse href: B:220 38:4 or B:220 38:4-7
        val parts = href.substringAfter("B:").trim().split(" ")
        if (parts.size != 2) return null
        val bookNumber = parts[0].toInt()
        val chapterVersePart = parts[1]
        // Handle verse ranges like 38:4 or 38:4-7
        val chapterVerseSplit = chapterVersePart.split(":")
        if (chapterVerseSplit.size != 2) return null
        val chapter = chapterVerseSplit[0].toInt()
        val versePart = chapterVerseSplit[1]
        val verseStart = versePart.substringBefore("-").toInt()
        val verseEnd = if (versePart.contains("-")) versePart.substringAfter("-").toInt() else null
        // Get the book name from BibleData using the book number
        val book = BibleData.getBookByCustomNumber(bookNumber)
        return PassageSelection(
            bookNumber = bookNumber,
            bookName = book?.name ?: "",
            chapter = chapter,
            verse = verseStart,
            verseEnd = verseEnd
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

private fun fetchVersesText(passage: PassageSelection, db: DatabaseHelper?): String {
    if (db == null) return "<p>Database not available.</p>"
    val verses = db.getVerses(passage.bookNumber, passage.chapter)
    val start = passage.verse
    val end = passage.verseEnd ?: start
    val selectedVerses = verses.filter { it.verseNumber in start..end }
    if (selectedVerses.isEmpty()) return "<p>No verses found.</p>"
    return selectedVerses.joinToString("") {
        "<p><b>${it.verseNumber}</b> ${it.text}</p>"
    }
}