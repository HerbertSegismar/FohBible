package com.fountofhopedotorg.fohbible.learn

import android.content.res.Configuration
import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.Letter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HistoryTable(
    letters: List<Letter>,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    onLetterClick: (Int) -> Unit = {},
    isAllCompleted: Boolean = false
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(letters.size) {
        if (letters.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val lineColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
    var allLines by remember { mutableStateOf<List<Line>>(emptyList()) }
    var drawnLines by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var currentAnimLineIndex by remember { mutableIntStateOf(-1) }
    val currentAnimProgress = remember { Animatable(0f) }
    var animationStarted by remember { mutableStateOf(false) }

    var outerRect by remember { mutableStateOf<Rect?>(null) }
    val boxProgress = remember { Animatable(0f) }
    var boxAnimationStarted by remember { mutableStateOf(false) }

    val boundsMap = remember { mutableStateMapOf<Int, Rect>() }
    var boxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(isAllCompleted) {
        if (isAllCompleted) {
            if (!animationStarted) {
                animationStarted = true

                snapshotFlow { boundsMap.size }
                    .first { it >= letters.size && letters.isNotEmpty() }

                delay(50.milliseconds)

                val items = boundsMap.toMap()
                val lines = computeGridLinesFromBounds(items)
                allLines = lines

                if (items.isNotEmpty()) {
                    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
                    items.values.forEach { rect ->
                        if (rect.left < minX) minX = rect.left
                        if (rect.top < minY) minY = rect.top
                        if (rect.right > maxX) maxX = rect.right
                        if (rect.bottom > maxY) maxY = rect.bottom
                    }
                    outerRect = Rect(minX, minY, maxX, maxY)
                }

                if (lines.isNotEmpty()) {
                    val shuffled = lines.indices.shuffled()
                    for (index in shuffled) {
                        currentAnimLineIndex = index
                        currentAnimProgress.snapTo(0f)
                        currentAnimProgress.animateTo(1f, animationSpec = tween(500))
                        drawnLines = drawnLines + index
                    }
                    currentAnimLineIndex = -1
                }

                if (outerRect != null && !boxAnimationStarted) {
                    boxAnimationStarted = true
                    boxProgress.snapTo(0f)
                    boxProgress.animateTo(1f, animationSpec = tween(1000))
                }
            }
        } else {
            animationStarted = false
            allLines = emptyList()
            drawnLines = emptySet()
            currentAnimLineIndex = -1
            outerRect = null
            boxAnimationStarted = false
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val safeZonePadding = 8.dp
        val availableWidth = maxWidth - safeZonePadding * 2
        val minSize = 48.dp
        val spacing = 8.dp

        val columns = maxOf(1, ((availableWidth + spacing) / (minSize + spacing)).toInt())
        val itemWidth = (availableWidth - spacing * (columns - 1)) / columns

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .onGloballyPositioned { boxCoords = it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(safeZonePadding),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                val rows = (letters.size + columns - 1) / columns
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (c in 0 until columns) {
                            val index = r * columns + c
                            if (index < letters.size) {
                                val letter = letters[index]
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(itemWidth)
                                        .onGloballyPositioned { itemCoords ->
                                            boxCoords?.let { parent ->
                                                if (parent.isAttached && itemCoords.isAttached) {
                                                    boundsMap[index] = parent.localBoundingBoxOf(itemCoords)
                                                }
                                            }
                                        }
                                        .clickable { onLetterClick(index) }
                                ) {
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
                            } else {
                                Spacer(modifier = Modifier.width(itemWidth))
                            }
                        }
                    }
                }
            }

            if (isAllCompleted && (allLines.isNotEmpty() || outerRect != null)) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    // Visually expand bounds by 6dp so the lines look like a framed box,
                    // and keep it 2dp away from the 8dp safe zone edge to stop clipping.
                    val visualPadding = 6.dp.toPx()

                    val adjustedLines = allLines.map { line ->
                        if (kotlin.math.abs(line.start.x - line.end.x) < 1f) {
                            Line(
                                Offset(line.start.x, line.start.y - visualPadding),
                                Offset(line.end.x, line.end.y + visualPadding)
                            )
                        } else {
                            Line(
                                Offset(line.start.x - visualPadding, line.start.y),
                                Offset(line.end.x + visualPadding, line.end.y)
                            )
                        }
                    }

                    drawnLines.forEach { i ->
                        drawGridLine(adjustedLines[i], color = lineColor)
                    }
                    if (currentAnimLineIndex in adjustedLines.indices) {
                        drawGridLine(
                            adjustedLines[currentAnimLineIndex],
                            progress = currentAnimProgress.value,
                            color = lineColor
                        )
                    }

                    outerRect?.let { rect ->
                        val expandedRect = Rect(
                            left = rect.left - visualPadding,
                            top = rect.top - visualPadding,
                            right = rect.right + visualPadding,
                            bottom = rect.bottom + visualPadding
                        )
                        drawBoundingBox(
                            rect = expandedRect,
                            progress = if (boxAnimationStarted) boxProgress.value else 0f,
                            color = lineColor
                        )
                    }
                }
            }
        }
    }
}

private data class Line(val start: Offset, val end: Offset)

private fun computeGridLinesFromBounds(items: Map<Int, Rect>): List<Line> {
    if (items.isEmpty()) return emptyList()

    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
    items.values.forEach { rect ->
        if (rect.left < minX) minX = rect.left
        if (rect.top < minY) minY = rect.top
        if (rect.right > maxX) maxX = rect.right
        if (rect.bottom > maxY) maxY = rect.bottom
    }

    val colGroups = items.values.groupBy { it.left.roundToInt() }
    val rowGroups = items.values.groupBy { it.top.roundToInt() }

    val sortedColRights = colGroups.keys.sorted().map { key -> colGroups[key]!!.maxOf { it.right } }
    val sortedColLefts = colGroups.keys.sorted().map { key -> colGroups[key]!!.minOf { it.left } }

    val sortedRowBottoms = rowGroups.keys.sorted().map { key -> rowGroups[key]!!.maxOf { it.bottom } }
    val sortedRowTops = rowGroups.keys.sorted().map { key -> rowGroups[key]!!.minOf { it.top } }

    val verticalLines = mutableListOf<Line>()
    for (i in 0 until sortedColRights.size - 1) {
        val right = sortedColRights[i]
        val leftNext = sortedColLefts[i + 1]
        val x = (right + leftNext) / 2f
        verticalLines.add(Line(Offset(x, minY), Offset(x, maxY)))
    }

    val horizontalLines = mutableListOf<Line>()
    for (i in 0 until sortedRowBottoms.size - 1) {
        val bottom = sortedRowBottoms[i]
        val topNext = sortedRowTops[i + 1]
        val y = (bottom + topNext) / 2f
        horizontalLines.add(Line(Offset(minX, y), Offset(maxX, y)))
    }

    return verticalLines + horizontalLines
}

private fun DrawScope.drawGridLine(
    line: Line,
    progress: Float = 1f,
    color: Color = Color.Black
) {
    val path = Path().apply {
        moveTo(line.start.x, line.start.y)
        lineTo(line.end.x, line.end.y)
    }
    drawPathSegment(path, progress, color)
}

private fun DrawScope.drawBoundingBox(
    rect: Rect,
    progress: Float,
    color: Color
) {
    if (progress <= 0f) return
    val path = Path().apply {
        moveTo(rect.left, rect.top)
        lineTo(rect.right, rect.top)
        lineTo(rect.right, rect.bottom)
        lineTo(rect.left, rect.bottom)
        close()
    }
    drawPathSegment(path, progress.coerceIn(0f, 1f), color)
}

private fun DrawScope.drawPathSegment(
    path: Path,
    progress: Float,
    color: Color
) {
    val pathMeasure = PathMeasure(path.asAndroidPath(), false)
    val length = pathMeasure.length
    val stop = length * progress
    if (stop > 0f) {
        val segment = android.graphics.Path()
        pathMeasure.getSegment(0f, stop, segment, true)
        drawPath(
            path = segment.asComposePath(),
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )
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
        val isLastQuestion = testIndex == testLetters.size - 1

        val nextButton = @Composable {
            Button(
                onClick = onNextQuestion,
                enabled = selectedOption != null,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Text(text = if (isLastQuestion) "Finish" else "Next", color = Color.White)
            }
        }

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
                        nextButton()
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
                    nextButton()
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