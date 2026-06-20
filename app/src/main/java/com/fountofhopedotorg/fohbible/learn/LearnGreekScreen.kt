package com.fountofhopedotorg.fohbible.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.Letter
import com.fountofhopedotorg.fohbible.models.AppViewModel

@Composable
fun LearnGreekScreen() {
    val viewModel: AppViewModel = viewModel()

    val letters = remember {
        listOf(
            Letter("Alpha", "A") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ALPHA_UPPER, progress, isDarkMode)
            },
            Letter("alpha", "a") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ALPHA_LOWER, progress, isDarkMode)
            },
            Letter("Beta", "B") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.BETA_UPPER, progress, isDarkMode)
            },
            Letter("beta", "b") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.BETA_LOWER, progress, isDarkMode)
            },
            Letter("Gamma", "Γ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.GAMMA_UPPER, progress, isDarkMode)
            },
            Letter("gamma", "γ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.GAMMA_LOWER, progress, isDarkMode)
            },
            Letter("Delta", "Δ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.DELTA_UPPER, progress, isDarkMode)
            },
            Letter("delta", "δ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.DELTA_LOWER, progress, isDarkMode)
            },
            Letter("Epsilon", "Ε") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.EPSILON_UPPER, progress, isDarkMode)
            },
            Letter("epsilon", "ε") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.EPSILON_LOWER, progress, isDarkMode)
            },
            Letter("Zeta", "Ζ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ZETA_UPPER, progress, isDarkMode)
            },
            Letter("zeta", "ζ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ZETA_LOWER, progress, isDarkMode)
            },
            Letter("Eta", "Η") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ETA_UPPER, progress, isDarkMode)
            },
            Letter("eta", "η") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ETA_LOWER, progress, isDarkMode)
            },
            Letter("Theta", "Θ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.THETA_UPPER, progress, isDarkMode)
            },
            Letter("theta", "θ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.THETA_LOWER, progress, isDarkMode)
            },
            Letter("Iota", "Ι") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.IOTA_UPPER, progress, isDarkMode)
            },
            Letter("iota", "ι") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.IOTA_LOWER, progress, isDarkMode)
            },
            Letter("Kappa", "Κ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.KAPPA_UPPER, progress, isDarkMode)
            },
            Letter("kappa", "κ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.KAPPA_LOWER, progress, isDarkMode)
            },
            Letter("Lambda", "Λ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.LAMBDA_UPPER, progress, isDarkMode)
            },
            Letter("lambda", "λ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.LAMBDA_LOWER, progress, isDarkMode)
            },
            Letter("Mu", "Μ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.MU_UPPER, progress, isDarkMode)
            },
            Letter("mu", "μ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.MU_LOWER, progress, isDarkMode)
            }
        )
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var replayTrigger by rememberSaveable { mutableIntStateOf(0) }
    var showEnglish by rememberSaveable { mutableStateOf(false) }
    val progressAnimatable = remember(currentIndex, replayTrigger) { Animatable(0f) }

    var isTesting by rememberSaveable { mutableStateOf(false) }
    val shuffledIndices = rememberSaveable {
        mutableStateOf(letters.indices.shuffled())
    }
    val testLetters = shuffledIndices.value.map { letters[it] }

    var testIndex by rememberSaveable { mutableIntStateOf(0) }
    var testScore by rememberSaveable { mutableIntStateOf(0) }
    var selectedOption by rememberSaveable { mutableStateOf<String?>(null) }

    val currentOptions by remember(isTesting, testIndex, testLetters) {
        derivedStateOf {
            if (isTesting && testIndex < testLetters.size) {
                val correct = testLetters[testIndex]
                val wrong = letters.filter { it.name != correct.name }
                    .shuffled()
                    .take(3)
                    .map { it.name }
                (wrong + correct.name).shuffled()
            } else {
                emptyList()
            }
        }
    }

    val startTest = {
        shuffledIndices.value = letters.indices.shuffled()
        testIndex = 0
        testScore = 0
        selectedOption = null
        isTesting = true
    }

    LaunchedEffect(isTesting, testIndex, testLetters) {
        if (isTesting) {
            selectedOption = null
        }
    }

    LaunchedEffect(currentIndex, replayTrigger) {
        if (!isTesting && currentIndex < letters.size) {
            showEnglish = false
            progressAnimatable.snapTo(0f)
            progressAnimatable.animateTo(1f, animationSpec = tween(4000))
            showEnglish = true
        }
    }

    if (isTesting) {
        TestScreen(
            testLetters = testLetters,
            testIndex = testIndex,
            testScore = testScore,
            currentOptions = currentOptions,
            selectedOption = selectedOption,
            onOptionSelected = { option ->
                if (selectedOption == null) {
                    selectedOption = option
                    if (option == testLetters[testIndex].name) {
                        testScore++
                    }
                }
            },
            onNextQuestion = { testIndex++ },
            onBackToLearn = {
                isTesting = false
                currentIndex = 0
            },
            onRestartTest = { startTest() },
            isDarkMode = viewModel.darkTheme
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val isLandscape = maxWidth > maxHeight
            val isAllCompleted = currentIndex == letters.size
            val previouslySeenLetters = letters.take(currentIndex)

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    HistoryTable(
                        letters = previouslySeenLetters,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isDarkMode = viewModel.darkTheme
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    if (isAllCompleted) {
                        CompletionScreen(
                            onRestart = { currentIndex = 0 },
                            onStartTest = startTest,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    } else {
                        MainLetterContent(
                            currentLetter = letters[currentIndex],
                            progress = progressAnimatable.value,
                            showEnglish = showEnglish,
                            isLastLetter = currentIndex == letters.size - 1,
                            onReplay = {
                                showEnglish = false
                                replayTrigger++
                            },
                            onNext = {
                                showEnglish = false
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            isDarkMode = viewModel.darkTheme
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    HistoryTable(
                        letters = previouslySeenLetters,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        isDarkMode = viewModel.darkTheme
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAllCompleted) {
                        CompletionScreen(
                            onRestart = { currentIndex = 0 },
                            onStartTest = startTest,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    } else {
                        MainLetterContent(
                            currentLetter = letters[currentIndex],
                            progress = progressAnimatable.value,
                            showEnglish = showEnglish,
                            isLastLetter = currentIndex == letters.size - 1,
                            onReplay = {
                                showEnglish = false
                                replayTrigger++
                            },
                            onNext = {
                                showEnglish = false
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isDarkMode = viewModel.darkTheme
                        )
                    }
                }
            }
        }
    }
}