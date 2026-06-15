package com.fountofhopedotorg.fohbible.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.fountofhopedotorg.fohbible.data.QuizItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizItemCard(
    index: Int,
    item: QuizItem,
    userAnswer: String,
    onAnswerChange: (String) -> Unit,
    submitted: Boolean
) {
    val isCorrect = submitted && userAnswer.trim().equals(item.missingWord, ignoreCase = true)
    val isWrong = submitted && !isCorrect

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isWrong) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else if (isCorrect) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.1f))
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ---- Header row with verse reference and status icon ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${index + 1}. ${item.verse.bookName} ${item.verse.chapter}:${item.verse.verseNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isCorrect) {
                    Icon(Icons.Default.Check, "Correct", tint = Color(0xFF4CAF50))
                } else if (isWrong) {
                    Icon(Icons.Default.Close, "Wrong", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Verse text (with highlighted answer when submitted) ----
            if (submitted) {
                VerseTextWithAnswer(item.displayText, item.missingWord)
            } else {
                Text(text = item.displayText, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Multiple‑choice options (flow layout) ----
            if (item.options.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.options.forEach { option ->
                        val selected = option == userAnswer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .widthIn(min = 150.dp)   // ensures two items fit on most screens
                                .weight(1f, fill = true) // fills equal width in row
                                .clickable(enabled = !submitted) { onAnswerChange(option) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { if (!submitted) onAnswerChange(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Feedback text after submission
                if (submitted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your answer: $userAnswer",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    if (isWrong) {
                        Text(
                            text = "Correct: ${item.missingWord}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                // Fill‑in‑the‑blank text field (unchanged)
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = onAnswerChange,
                    label = { Text("Your answer") },
                    enabled = !submitted,
                    modifier = Modifier.fillMaxWidth(),
                    isError = isWrong,
                    supportingText = if (isWrong) {
                        { Text("Correct: ${item.missingWord}") }
                    } else if (isCorrect) {
                        { Text("Correct!") }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun VerseTextWithAnswer(displayText: String, missingWord: String) {
    val blank = "_".repeat(missingWord.length)
    val annotatedString = buildAnnotatedString {
        val parts = displayText.split(blank)
        if (parts.size >= 2) {
            append(parts[0])
            withStyle(
                SpanStyle(
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(missingWord)
            }
            append(parts[1])
        } else {
            append(displayText)
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge
    )
}