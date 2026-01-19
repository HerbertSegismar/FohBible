@file:Suppress("AssignedValueIsNeverRead")

package com.example.fohbible.modals

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.PassageSelection

@Composable
fun CommentaryModal(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String,
    content: String,
    onVerseLinkClick: (PassageSelection) -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (show) {
        val textColor = MaterialTheme.colorScheme.onBackground
        val linkColor = MaterialTheme.colorScheme.primary
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
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
                        val spanned = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT)
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
                                            onVerseLinkClick(passage)
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
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
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

        // Extract just the starting verse (ignore range for navigation)
        val verseStart = if (versePart.contains("-")) {
            versePart.substringBefore("-").toInt()
        } else {
            versePart.toInt()
        }

        // Get the book name from BibleData using the book number
        val book = BibleData.getBookByCustomNumber(bookNumber)

        return PassageSelection(
            bookNumber = bookNumber,
            bookName = book?.name ?: "",  // Use the book name from BibleData
            chapter = chapter,
            verse = verseStart
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}