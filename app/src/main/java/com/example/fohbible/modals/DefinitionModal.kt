package com.example.fohbible.modals

import android.text.method.LinkMovementMethod
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import java.util.Locale

@Composable
fun DefinitionModal(
    show: Boolean,
    onDismiss: () -> Unit,
    word: String,
    definition: String,
    selectedDictionary: String,
    onSwitch: () -> Unit
) {
    if (show) {
        val dbDisplayName = if (selectedDictionary == "noah") "Noah" else "ATSBD"
        val other = if (selectedDictionary == "noah") "ATSBD" else "Noah"
        val textColor = MaterialTheme.colorScheme.onBackground
        val linkColor = MaterialTheme.colorScheme.primary
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Definition of " + word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } + " - $dbDisplayName",
                    style = MaterialTheme.typography.titleMedium
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
                        textView.text = HtmlCompat.fromHtml(definition, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        textView.setTextColor(textColor.toArgb())
                        textView.setLinkTextColor(linkColor.toArgb())
                    },
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = onSwitch) {
                    Text("Switch to $other")
                }
            }
        )
    }
}