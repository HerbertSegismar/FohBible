package com.example.fohbible.modals

import android.graphics.Color
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun StrongsModal(
    show: Boolean,
    onDismiss: () -> Unit,
    strongNumber: String,
    definition: String
) {
    if (show) {
        val textColor = MaterialTheme.colorScheme.onBackground
        val linkColor = MaterialTheme.colorScheme.primary
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Strong's Definition for $strongNumber",
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
                        val spanned = HtmlCompat.fromHtml(definition, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        textView.text = spanned
                        textView.setTextColor(textColor.toArgb())
                        textView.setLinkTextColor(linkColor.toArgb())
                        textView.textDirection = View.TEXT_DIRECTION_LTR
                        textView.gravity = Gravity.START

                        // Handle dark mode text colors
                        if (isDark && textView.text is Spannable) {
                            val spannable = textView.text as Spannable
                            val spans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)

                            for (span in spans) {
                                val spanColor = span.foregroundColor

                                // Check if color is dark (low luminance)
                                if (isDarkColor(spanColor)) {
                                    val start = spannable.getSpanStart(span)
                                    val end = spannable.getSpanEnd(span)
                                    val flags = spannable.getSpanFlags(span)

                                    // Replace the dark color span with white
                                    spannable.removeSpan(span)
                                    spannable.setSpan(
                                        ForegroundColorSpan(Color.WHITE),
                                        start,
                                        end,
                                        flags
                                    )
                                }
                            }

                            // Also handle any remaining HTML colors that might not be spans
                            // by applying a default text color for the entire view
                            textView.setTextColor(Color.WHITE)
                        }
                    },
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 4.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Helper function to determine if a color is dark
 * Colors with luminance less than 0.5 are considered dark
 */
private fun isDarkColor(color: Int): Boolean {
    val r = Color.red(color) / 255.0
    val g = Color.green(color) / 255.0
    val b = Color.blue(color) / 255.0

    // Calculate relative luminance (sRGB)
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

    // Consider colors with luminance < 0.3 as dark (adjust threshold as needed)
    return luminance < 0.3
}