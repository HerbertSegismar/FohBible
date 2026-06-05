package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.ProcessingOptions
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor

@Composable
fun FetchVerseSection(
    referenceInput: String,
    onReferenceChange: (String) -> Unit,
    fetchError: String?,
    onFetch: () -> Unit,
    fetchedVerses: List<Verse>,
    currentReference: String,
    themeColors: ThemeColors,
    viewModel: AppViewModel,
    verseProcessor: VerseTextProcessor,
    showFetchInput: Boolean = true
) {
    Column(modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp).fillMaxWidth()) {
        if (showFetchInput) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = referenceInput,
                    onValueChange = onReferenceChange,
                    label = { Text("Ref(e.g. John 3:16)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = fetchError != null
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = onFetch) {
                    Text("Fetch", color = Color.White)
                }
            }

            if (fetchError != null) {
                Text(
                    fetchError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (fetchedVerses.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Fetched Verses:", style = MaterialTheme.typography.titleSmall)

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.primary.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Text(
                            currentReference,
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        fetchedVerses.forEach { verse ->
                            val processed = verseProcessor.processVerse(
                                verseText = verse.text,
                                baseFontSize = 16.sp,
                                themeColors = themeColors,
                                isOldTestament = viewModel.isOldTestament,
                                options = ProcessingOptions(showHeaders = false)
                            )
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = themeColors.verseNumber)) {
                                        append("${verse.verseNumber} ")
                                    }
                                    append(processed.body)
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}