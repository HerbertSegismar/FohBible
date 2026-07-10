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
fun LearnHebrewScreen() {
    val viewModel: AppViewModel = viewModel()
    val letters = remember {
        listOf(
            Letter("Aleph", "Silent") { progress, isDarkMode -> drawAlef(progress, isDarkMode) },
            Letter("Bet", "B/V") { progress, isDarkMode -> drawBet(progress, isDarkMode) },
            Letter("Gimel", "G") { progress, isDarkMode -> drawGimel(progress, isDarkMode) },
            Letter("Dalet", "D") { progress, isDarkMode -> drawDalet(progress, isDarkMode) },
            Letter("He", "H") { progress, isDarkMode -> drawHe(progress, isDarkMode) },
            Letter("Vav", "V") { progress, isDarkMode -> drawVav(progress, isDarkMode) },
            Letter("Zayin", "Z") { progress, isDarkMode -> drawZayin(progress, isDarkMode) },
            Letter("Chet", "Ch") { progress, isDarkMode -> drawChet(progress, isDarkMode) },
            Letter("Tet", "T") { progress, isDarkMode -> drawTet(progress, isDarkMode) },
            Letter("Yod", "Y") { progress, isDarkMode -> drawYod(progress, isDarkMode) },
            Letter("Kaf", "K/Kh") { progress, isDarkMode -> drawKaf(progress, isDarkMode) },
            Letter("Kaf Sofit", "Kh") { progress, isDarkMode -> drawKafSofit(progress, isDarkMode) },
            Letter("Lamed", "L") { progress, isDarkMode -> drawLamed(progress, isDarkMode) },
            Letter("Mem", "M") { progress, isDarkMode -> drawMem(progress, isDarkMode) },
            Letter("Mem Sofit", "M") { progress, isDarkMode -> drawMemSofit(progress, isDarkMode) },
            Letter("Nun", "N") { progress, isDarkMode -> drawNun(progress, isDarkMode) },
            Letter("Nun Sofit", "N") { progress, isDarkMode -> drawNunSofit(progress, isDarkMode) },
            Letter("Samech", "S") { progress, isDarkMode -> drawSamech(progress, isDarkMode) },
            Letter("Ayin", "Silent") { progress, isDarkMode -> drawAyin(progress, isDarkMode) },
            Letter("Peh", "P/F") { progress, isDarkMode -> drawPeh(progress, isDarkMode) },
            Letter("Peh Sofit", "F") { progress, isDarkMode -> drawPehSofit(progress, isDarkMode) },
            Letter("Tsadeh", "Ts") { progress, isDarkMode -> drawTsadeh(progress, isDarkMode) },
            Letter("Tsadeh Sofit", "Ts") { progress, isDarkMode -> drawTsadehSofit(progress, isDarkMode) },
            Letter("Qof", "Q") { progress, isDarkMode -> drawQof(progress, isDarkMode) },
            Letter("Resh", "R") { progress, isDarkMode -> drawResh(progress, isDarkMode) },
            Letter("Shin", "Sh/S") { progress, isDarkMode -> drawShin(progress, isDarkMode) },
            Letter("Tav", "T") { progress, isDarkMode -> drawTav(progress, isDarkMode) }
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
                        isAllCompleted = isAllCompleted,
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
                        isAllCompleted = isAllCompleted,
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