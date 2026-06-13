package com.fountofhopedotorg.fohbible.quiz

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.QuizItem
import com.fountofhopedotorg.fohbible.modals.VersionSelectionModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BibleQuizScreen() {
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current

    var selectedVersionAbbr by remember { mutableStateOf(viewModel.currentVersionAbbr) }
    var selectedDbName by remember { mutableStateOf(viewModel.currentDbName) }

    val dbHelper = remember(viewModel.currentDbName) {
        DatabaseHelper(context, viewModel.currentDbName)
    }

    DisposableEffect(viewModel.currentDbName) {
        onDispose { dbHelper.close() }
    }

    var quizCount by remember { mutableIntStateOf(10) }
    var quizItems by remember { mutableStateOf<List<QuizItem>>(emptyList()) }
    var userAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var submitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var showVersionModal by remember { mutableStateOf(false) }
    var versionTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(versionTrigger, dbHelper) {
        val items = generateQuizItems(dbHelper, quizCount)
        quizItems = items
        userAnswers = List(items.size) { "" }
        submitted = false
        score = 0
    }

    Scaffold(
        floatingActionButton = {
            if (quizItems.isNotEmpty() && !submitted) {
                ExtendedFloatingActionButton(
                    onClick = {
                        var correct = 0
                        quizItems.forEachIndexed { index, item ->
                            val userAnswer = userAnswers[index].trim().lowercase()
                            if (userAnswer == item.missingWord.lowercase()) correct++
                        }
                        score = correct
                        submitted = true
                        Toast.makeText(
                            context,
                            "You scored $correct / ${quizItems.size}",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    content = { Text("Submit Answers") }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Fill in the blanks Quiz",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                BasicTextField(
                    value = if (quizCount == 0) "" else quizCount.toString(),
                    onValueChange = { value ->
                        if (value.isEmpty()) {
                            quizCount = 0
                        } else {
                            val cleanValue = value.trimStart('0')
                            if (cleanValue.isEmpty()) {
                                quizCount = 0
                            } else {
                                val newCount = cleanValue.toIntOrNull()
                                if (newCount != null && newCount <= 50) {
                                    quizCount = newCount
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(64.dp)
                        .height(35.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }
                    }
                )

                Button(
                    onClick = {
                        if (quizCount < 1) {
                            Toast.makeText(context, "Enter a number ≥ 1", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        versionTrigger++
                    },
                    modifier = Modifier.height(35.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Generate", color = Color.White)
                }

                IconButton(
                    onClick = { showVersionModal = true },
                ) {
                    Icon(
                        modifier = Modifier.size(35.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        imageVector = Icons.Default.Language,
                        contentDescription = "Select Bible version"
                    )
                }
            }

            if (quizItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(quizItems) { index, item ->
                        QuizItemCard(
                            index = index,
                            item = item,
                            userAnswer = userAnswers[index],
                            onAnswerChange = { newAns ->
                                userAnswers = userAnswers.toMutableList()
                                    .also { it[index] = newAns }
                                    .toList()
                            },
                            submitted = submitted
                        )
                    }

                    if (submitted) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Final Score: $score / ${quizItems.size}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { versionTrigger++ }) {
                                    Text("New Quiz", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVersionModal) {
        VersionSelectionModal(
            currentVersionKey = viewModel.currentDbName,
            isSecondary = false,
            onVersionSelected = { file ->
                selectedDbName = file
                selectedVersionAbbr = BibleVersionUtils.versionMap[file] ?: "Bible"
                viewModel.currentDbName = file
                viewModel.currentVersionAbbr = selectedVersionAbbr
                showVersionModal = false
            },
            onDismiss = { showVersionModal = false },
            colors = mapOf(
                "primary" to MaterialTheme.colorScheme.primary,
                "card" to if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor,
                "text" to MaterialTheme.colorScheme.onSurface,
                "muted" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                "border" to MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}