package com.fountofhopedotorg.fohbible.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
            Letter("Gamma", "G") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.GAMMA_UPPER, progress, isDarkMode)
            },
            Letter("gamma", "g") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.GAMMA_LOWER, progress, isDarkMode)
            },
            Letter("Delta", "D") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.DELTA_UPPER, progress, isDarkMode)
            },
            Letter("delta", "d") { progress, isDarkMode ->
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
            Letter("zeta", "z") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ZETA_LOWER, progress, isDarkMode)
            },
            Letter("Eta", "Η") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ETA_UPPER, progress, isDarkMode)
            },
            Letter("eta", "h") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.ETA_LOWER, progress, isDarkMode)
            },
            Letter("Theta", "T") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.THETA_UPPER, progress, isDarkMode)
            },
            Letter("theta", "t") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.THETA_LOWER, progress, isDarkMode)
            },
            Letter("Iota", "Ι") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.IOTA_UPPER, progress, isDarkMode)
            },
            Letter("iota", "i") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.IOTA_LOWER, progress, isDarkMode)
            },
            Letter("Kappa", "Κ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.KAPPA_UPPER, progress, isDarkMode)
            },
            Letter("kappa", "κ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.KAPPA_LOWER, progress, isDarkMode)
            },
            Letter("Lambda", "L") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.LAMBDA_UPPER, progress, isDarkMode)
            },
            Letter("lambda", "l") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.LAMBDA_LOWER, progress, isDarkMode)
            },
            Letter("Mu", "Μ") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.MU_UPPER, progress, isDarkMode)
            },
            Letter("mu", "m") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.MU_LOWER, progress, isDarkMode)
            },
            Letter("Nu", "Ν") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.NU_UPPER, progress, isDarkMode)
            },
            Letter("nu", "n") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.NU_LOWER, progress, isDarkMode)
            },
            Letter("Xi", "X") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.XI_UPPER, progress, isDarkMode)
            },
            Letter("xi", "x") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.XI_LOWER, progress, isDarkMode)
            },
            Letter("Omicron", "Ο") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.OMICRON_UPPER, progress, isDarkMode)
            },
            Letter("omicron", "ο") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.OMICRON_LOWER, progress, isDarkMode)
            },
            Letter("Pi", "P") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.PI_UPPER, progress, isDarkMode)
            },
            Letter("pi", "p") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.PI_LOWER, progress, isDarkMode)
            },
            Letter("Rho", "R") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.RHO_UPPER, progress, isDarkMode)
            },
            Letter("rho", "r") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.RHO_LOWER, progress, isDarkMode)
            },
            Letter("Sigma", "S") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.SIGMA_UPPER, progress, isDarkMode)
            },
            Letter("sigma", "s") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.SIGMA_LOWER, progress, isDarkMode)
            },
            Letter("Tau", "T") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.TAU_UPPER, progress, isDarkMode)
            },
            Letter("tau", "t") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.TAU_LOWER, progress, isDarkMode)
            },
            Letter("Upsilon", "U") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.UPSILON_UPPER, progress, isDarkMode)
            },
            Letter("upsilon", "u") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.UPSILON_LOWER, progress, isDarkMode)
            },
            Letter("Phi", "Ph") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.PHI_UPPER, progress, isDarkMode)
            },
            Letter("phi", "ph") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.PHI_LOWER, progress, isDarkMode)
            },
            Letter("Chi", "Ch") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.CHI_UPPER, progress, isDarkMode)
            },
            Letter("chi", "ch") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.CHI_LOWER, progress, isDarkMode)
            },
            Letter("Psi", "Ps") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.PSI_UPPER, progress, isDarkMode)
            },
            Letter("psi", "ps") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.PSI_LOWER, progress, isDarkMode)
            },
            Letter("Omega", "Ō") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.OMEGA_UPPER, progress, isDarkMode)
            },
            Letter("omega", "ō") { progress, isDarkMode ->
                drawGreekGlyph(GreekGlyph.OMEGA_LOWER, progress, isDarkMode)
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
                        isDarkMode = viewModel.darkTheme,
                        onLetterClick = { index ->
                            currentIndex = index
                            showEnglish = false
                            replayTrigger++
                        }
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
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isDarkMode = viewModel.darkTheme,
                        onLetterClick = { index ->
                            currentIndex = index
                            showEnglish = false
                            replayTrigger++
                        }
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