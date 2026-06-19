package com.fountofhopedotorg.fohbible.learn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.Letter
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun HistoryTable(letters: List<Letter>, modifier: Modifier = Modifier, isDarkMode: Boolean = false ) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 48.dp),
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(letters) { letter ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        letter.draw(this, 1f, isDarkMode)
                    }
                }
                Text(
                    text = letter.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MainLetterContent(
    currentLetter: Letter,
    progress: Float,
    showEnglish: Boolean,
    isLastLetter: Boolean,
    onReplay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val dashPath = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                val guideLineColor = Color.Gray.copy(alpha = 0.8f)
                val lineThickness = 3f

                val topY = canvasHeight * 0.27f
                val midY = canvasHeight * 0.555f
                val bottomY = canvasHeight * 0.84f

                drawLine(
                    color = guideLineColor,
                    start = Offset(0f, topY),
                    end = Offset(canvasWidth, topY),
                    strokeWidth = lineThickness,
                    pathEffect = dashPath
                )

                drawLine(
                    color = guideLineColor,
                    start = Offset(0f, midY),
                    end = Offset(canvasWidth, midY),
                    strokeWidth = lineThickness,
                    pathEffect = dashPath
                )

                drawLine(
                    color = guideLineColor,
                    start = Offset(0f, bottomY),
                    end = Offset(canvasWidth, bottomY),
                    strokeWidth = lineThickness,
                    pathEffect = dashPath
                )

                currentLetter.draw(this, progress, isDarkMode)
            }
        }
        Text(currentLetter.name, style = MaterialTheme.typography.bodyLarge, fontSize = 24.sp)
        Text(
            text = "English: ${currentLetter.english}",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(if (showEnglish) 1f else 0f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = onReplay) {
                Text("Replay", color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = onNext) {
                Text(if (isLastLetter) "Finish" else "Next", color = Color.White)
            }
        }
    }
}

@Composable
fun CompletionScreen(
    onRestart: () -> Unit,
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Flash Cards Completed!", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRestart) {
            Text("Restart", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onStartTest) {
            Text("Take Test", color = Color.White)
        }
    }
}

@Composable
fun TestScreen(
    testLetters: List<Letter>,
    testIndex: Int,
    testScore: Int,
    currentOptions: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onBackToLearn: () -> Unit,
    onRestartTest: () -> Unit,
    isDarkMode: Boolean = false
) {
    val isTestCompleted = testIndex >= testLetters.size
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isTestCompleted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Test Completed!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Your Score: $testScore / ${testLetters.size}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRestartTest,
                modifier = Modifier.fillMaxWidth(if (isLandscape) 0.4f else 0.6f)
            ) {
                Text("Retake Test", color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBackToLearn,
                modifier = Modifier.fillMaxWidth(if (isLandscape) 0.4f else 0.6f)
            ) {
                Text("Back to Learning")
            }
        }
    } else {
        val currentLetter = testLetters[testIndex]

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    QuestionHeader(testIndex, testLetters.size, currentLetter, isDarkMode)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OptionsSection(currentOptions, currentLetter, selectedOption, onOptionSelected)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBackToLearn) {
                            Text("Cancel Test", color = MaterialTheme.colorScheme.error)
                        }
                        NextButtonSection(selectedOption, testIndex, testLetters.size, onNextQuestion)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QuestionHeader(testIndex, testLetters.size, currentLetter, isDarkMode)
                Spacer(modifier = Modifier.height(24.dp))
                OptionsSection(currentOptions, currentLetter, selectedOption, onOptionSelected)
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBackToLearn) {
                        Text("Cancel Test", color = MaterialTheme.colorScheme.error)
                    }
                    NextButtonSection(selectedOption, testIndex, testLetters.size, onNextQuestion)
                }
            }
        }
    }
}


@Composable
private fun QuestionHeader(
    testIndex: Int,
    totalQuestions: Int,
    currentLetter: Letter,
    isDarkMode: Boolean = false
) {
    Text(
        text = "Question ${testIndex + 1} of $totalQuestions",
        style = MaterialTheme.typography.labelLarge
    )
    Spacer(modifier = Modifier.height(4.dp))

    Canvas(
        modifier = Modifier
            .size(240.dp)
    ) {
        currentLetter.draw(this, 1f, isDarkMode)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Which letter is this?", style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun OptionsSection(
    currentOptions: List<String>,
    currentLetter: Letter,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    currentOptions.forEach { option ->
        val isCorrect = option == currentLetter.name
        val isSelected = option == selectedOption

        val containerColor = when {
            selectedOption == null -> MaterialTheme.colorScheme.primaryContainer
            isSelected && isCorrect -> Color(0xFF4CAF50)
            isSelected && !isCorrect -> Color(0xFFF44336)
            isCorrect -> Color(0xFF4CAF50)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }

        val contentColor = when {
            selectedOption == null -> MaterialTheme.colorScheme.onPrimaryContainer
            isSelected || isCorrect -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Button(
            onClick = { onOptionSelected(option) },
            enabled = selectedOption == null,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor,
                disabledContentColor = contentColor
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 6.dp)
        ) {
            Text(text = option, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun NextButtonSection(
    selectedOption: String?,
    testIndex: Int,
    totalQuestions: Int,
    onNextQuestion: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    if (selectedOption != null) {
        Box(
            modifier = Modifier.fillMaxWidth(0.8f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(onClick = onNextQuestion) {
                Text(text = if (testIndex == totalQuestions - 1) "View Results" else "Next", color = Color.White)
            }
        }
    } else {
        Spacer(modifier = Modifier.height(48.dp))
    }
}