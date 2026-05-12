package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.BibleVersionInfo
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor


@Composable
fun BibleVersionInfoDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    versionInfo: BibleVersionInfo? = null,
    title: String = "Version Info",
    titleTextStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold
    ),
    loadingContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    },
    emptyInfoText: String = "No detailed information available.",
    errorText: String = "Could not load version information.",
    errorColor: Color = MaterialTheme.colorScheme.error,
    confirmButtonText: String = "Close",
    transformText: (String?) -> String? = { input ->
        input?.let { SimpleVerseProcessor.stripXmlTags(it) }
    }
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = titleTextStyle
                )
            },
            text = {
                if (isLoading) {
                    loadingContent()
                } else {
                    val info = versionInfo
                    if (info != null) {
                        val cleanDescription = transformText(info.description)
                        val cleanDetailedInfo = transformText(info.detailedInfo)

                        Column {
                            if (!cleanDescription.isNullOrEmpty()) {
                                Text(
                                    text = cleanDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            if (!cleanDetailedInfo.isNullOrEmpty()) {
                                Text(
                                    text = cleanDetailedInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (cleanDescription.isNullOrEmpty() && cleanDetailedInfo.isNullOrEmpty()) {
                                Text(
                                    text = emptyInfoText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = errorColor
                                )
                            }
                        }
                    } else {
                        Text(
                            text = errorText,
                            color = errorColor
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(confirmButtonText)
                }
            }
        )
    }
}