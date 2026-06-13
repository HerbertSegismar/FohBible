package com.fountofhopedotorg.fohbible.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.QuizItem

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
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correct",
                        tint = Color(0xFF4CAF50)
                    )
                } else if (isWrong) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Wrong",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.displayText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
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