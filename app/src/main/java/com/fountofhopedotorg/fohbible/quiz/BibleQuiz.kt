package com.fountofhopedotorg.fohbible.quiz

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BibleQuizScreen() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var selectedVersionAbbr by remember { mutableStateOf(viewModel.currentVersionAbbr) }
    var selectedDbName by remember { mutableStateOf(viewModel.currentDbName) }

    val dbHelper = remember(viewModel.currentDbName) {
        DatabaseHelper(context, viewModel.currentDbName)
    }

    DisposableEffect(viewModel.currentDbName) {
        onDispose { dbHelper.close() }
    }

    var quizCount by remember { mutableIntStateOf(10) }
    var quizType by remember { mutableStateOf(QuizType.FILL_IN_THE_BLANK) }
    var showVersionModal by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    var fillItems by remember { mutableStateOf<List<QuizItem>>(emptyList()) }
    var fillUserAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var fillSubmitted by remember { mutableStateOf(false) }
    var fillScore by remember { mutableIntStateOf(0) }

    var multiItems by remember { mutableStateOf<List<QuizItem>>(emptyList()) }
    var multiUserAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var multiSubmitted by remember { mutableStateOf(false) }
    var multiScore by remember { mutableIntStateOf(0) }
    var prefetchedMulti by remember { mutableStateOf<List<QuizItem>?>(null) }

    var initialized by remember { mutableStateOf(false) }
    var quizVersion by remember { mutableIntStateOf(0) }

    val currentItems = if (quizType == QuizType.FILL_IN_THE_BLANK) fillItems else multiItems
    val currentUserAnswers = if (quizType == QuizType.FILL_IN_THE_BLANK) fillUserAnswers else multiUserAnswers
    val currentSubmitted = if (quizType == QuizType.FILL_IN_THE_BLANK) fillSubmitted else multiSubmitted
    val currentScore = if (quizType == QuizType.FILL_IN_THE_BLANK) fillScore else multiScore

    fun generateFillQuiz() {
        scope.launch(Dispatchers.IO) {
            val newFill = generateQuizItems(dbHelper, quizCount, QuizType.FILL_IN_THE_BLANK)
            fillItems = newFill
            fillUserAnswers = List(newFill.size) { "" }
            fillSubmitted = false
            fillScore = 0
            quizVersion++
        }
    }

    fun generateMultiQuiz() {
        val pre = prefetchedMulti
        if (pre != null) {
            when {
                pre.size >= quizCount -> {
                    val trimmed = pre.take(quizCount)
                    multiItems = trimmed
                    multiUserAnswers = List(trimmed.size) { "" }
                    multiSubmitted = false
                    multiScore = 0
                    prefetchedMulti = null
                    quizVersion++

                    scope.launch(Dispatchers.IO) {
                        val next = generateQuizItems(dbHelper, quizCount, QuizType.MULTIPLE_CHOICE)
                        prefetchedMulti = next
                    }
                }
                pre.size < quizCount -> {
                    multiItems = pre
                    multiUserAnswers = List(pre.size) { "" }
                    multiSubmitted = false
                    multiScore = 0
                    prefetchedMulti = null
                    quizVersion++
                    val alreadyShown = pre.size

                    scope.launch(Dispatchers.IO) {
                        val fullNew = generateQuizItems(dbHelper, quizCount, QuizType.MULTIPLE_CHOICE)
                        val additional = fullNew.takeLast(quizCount - alreadyShown)
                        multiItems = pre + additional
                        multiUserAnswers = multiUserAnswers + List(additional.size) { "" }

                        val next = generateQuizItems(dbHelper, quizCount, QuizType.MULTIPLE_CHOICE)
                        prefetchedMulti = next
                    }
                }
            }
        } else {
            scope.launch(Dispatchers.IO) {
                val newMulti = generateQuizItems(dbHelper, quizCount, QuizType.MULTIPLE_CHOICE)
                multiItems = newMulti
                multiUserAnswers = List(newMulti.size) { "" }
                multiSubmitted = false
                multiScore = 0
                quizVersion++

                val next = generateQuizItems(dbHelper, quizCount, QuizType.MULTIPLE_CHOICE)
                prefetchedMulti = next
            }
        }
    }

    LaunchedEffect(dbHelper) {
        val fill = withContext(Dispatchers.IO) {
            generateQuizItems(dbHelper, quizCount, QuizType.FILL_IN_THE_BLANK)
        }
        fillItems = fill
        fillUserAnswers = List(fill.size) { "" }
        fillSubmitted = false
        fillScore = 0
        quizVersion++

        launch(Dispatchers.IO) {
            val firstMulti = generateQuizItems(dbHelper, quizCount, QuizType.MULTIPLE_CHOICE)
            multiItems = firstMulti
            multiUserAnswers = List(firstMulti.size) { "" }
            multiSubmitted = false
            multiScore = 0
            quizVersion++

            val next = generateQuizItems(dbHelper, quizCount, QuizType.MULTIPLE_CHOICE)
            prefetchedMulti = next
        }

        initialized = true
    }

    LaunchedEffect(quizType) {
        focusManager.clearFocus()
        listState.scrollToItem(0)

        if (initialized) {
            if (quizType == QuizType.FILL_IN_THE_BLANK && fillItems.isNotEmpty() && fillItems.size != quizCount) {
                generateFillQuiz()
            } else if (quizType == QuizType.MULTIPLE_CHOICE && multiItems.isNotEmpty() && multiItems.size != quizCount) {
                generateMultiQuiz()
            }
        }
    }

    LaunchedEffect(quizVersion) {
        focusManager.clearFocus()
        listState.scrollToItem(0)
    }

    Scaffold(
        floatingActionButton = {
            if (currentItems.isNotEmpty() && !currentSubmitted) {
                ExtendedFloatingActionButton(
                    onClick = {
                        var correct = 0
                        currentItems.forEachIndexed { index, item ->
                            val userAnswer = currentUserAnswers[index].trim().lowercase()
                            if (userAnswer == item.missingWord.lowercase()) correct++
                        }
                        if (quizType == QuizType.FILL_IN_THE_BLANK) {
                            fillScore = correct
                            fillSubmitted = true
                        } else {
                            multiScore = correct
                            multiSubmitted = true
                        }
                        Toast.makeText(context,
                            "You scored $correct / ${currentItems.size}",
                            Toast.LENGTH_LONG).show()
                    },
                    content = { Text("Submit Answers") }
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).clickable { typeMenuExpanded = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (quizType) {
                                QuizType.FILL_IN_THE_BLANK -> "Fill‑in the blanks Quiz"
                                QuizType.MULTIPLE_CHOICE -> "Multiple choice Quiz"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(
                        modifier = Modifier.background(primaryColor.copy(0.1f)),
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Fill‑in the blank") },
                            onClick = {
                                quizType = QuizType.FILL_IN_THE_BLANK
                                typeMenuExpanded = false
                            },
                            trailingIcon = {
                                if (quizType == QuizType.FILL_IN_THE_BLANK)
                                    Icon(Icons.Default.Check, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Multiple choice") },
                            onClick = {
                                quizType = QuizType.MULTIPLE_CHOICE
                                typeMenuExpanded = false
                            },
                            trailingIcon = {
                                if (quizType == QuizType.MULTIPLE_CHOICE)
                                    Icon(Icons.Default.Check, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                BasicTextField(
                    value = if (quizCount == 0) "" else quizCount.toString(),
                    onValueChange = { value ->
                        if (value.isEmpty()) quizCount = 0
                        else {
                            val clean = value.trimStart('0')
                            if (clean.isEmpty()) quizCount = 0
                            else {
                                val newCount = clean.toIntOrNull()
                                if (newCount != null && newCount <= 50) quizCount = newCount
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(64.dp).height(35.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (quizCount == 0)
                                Text("1–50", style = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)))
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
                        if (quizType == QuizType.FILL_IN_THE_BLANK) generateFillQuiz()
                        else generateMultiQuiz()
                    },
                    modifier = Modifier.height(35.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Generate", color = Color.White)
                }

                IconButton(onClick = { showVersionModal = true }) {
                    Icon(modifier = Modifier.size(35.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        imageVector = Icons.Default.Language,
                        contentDescription = "Select Bible version")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                !initialized -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                quizType == QuizType.MULTIPLE_CHOICE && multiItems.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Preparing multiple‑choice quiz…")
                        }
                    }
                }
                currentItems.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tap Generate to start a quiz",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(currentItems) { index, item ->
                            QuizItemCard(
                                index = index,
                                item = item,
                                userAnswer = currentUserAnswers[index],
                                onAnswerChange = { newAns ->
                                    if (quizType == QuizType.FILL_IN_THE_BLANK) {
                                        fillUserAnswers = fillUserAnswers.toMutableList()
                                            .also { it[index] = newAns }.toList()
                                    } else {
                                        multiUserAnswers = multiUserAnswers.toMutableList()
                                            .also { it[index] = newAns }.toList()
                                    }
                                },
                                submitted = currentSubmitted
                            )
                        }

                        if (currentSubmitted) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Final Score: $currentScore / ${currentItems.size}",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = {
                                        if (quizType == QuizType.FILL_IN_THE_BLANK) generateFillQuiz()
                                        else generateMultiQuiz()
                                    }) {
                                        Text("New Quiz", color = Color.White)
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(360.dp)) }
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