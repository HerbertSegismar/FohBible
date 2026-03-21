package com.fountofhopedotorg.fohbible.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.fountofhopedotorg.fohbible.data.BibleBook
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.SCOPE_NEW_TESTAMENT
import com.fountofhopedotorg.fohbible.data.SCOPE_OLD_TESTAMENT
import com.fountofhopedotorg.fohbible.data.SCOPE_RANGES
import com.fountofhopedotorg.fohbible.data.SCOPE_WHOLE
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.data.scopeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.fountofhopedotorg.fohbible.data.BookUi

fun BibleBook.toBookUi(): BookUi {
    return BookUi(
        bookNumber = customNumber,
        longName = name,
        shortName = abbreviation,
        testament = testament,
        totalChapters = chapters
    )
}

fun getScopeForBookNumber(bookNumber: Int): String? {
    for ((scope, range) in SCOPE_RANGES) {
        if (range != null && bookNumber >= range.start && bookNumber <= range.end && scope != SCOPE_WHOLE && scope != SCOPE_OLD_TESTAMENT && scope != SCOPE_NEW_TESTAMENT) {
            return scope
        }
    }
    return null
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationModal(
    onDismissRequest: () -> Unit,
    onPassageSelected: (PassageSelection) -> Unit = { _ -> },
    showNavigationModal: Boolean,
    databaseHelper: DatabaseHelper? = null,
    initialBookNumber: Int? = null,
    initialChapter: Int = 1,
    initialVerse: Int? = 1
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val oldTestamentBooks = remember { BibleData.oldTestamentBooks.map { it.toBookUi() } }
    val newTestamentBooks = remember { BibleData.newTestamentBooks.map { it.toBookUi() } }
    var selectedBook by remember { mutableStateOf<BookUi?>(null) }
    var chapterInput by remember { mutableStateOf("") }
    var verseInput by remember { mutableStateOf("") }
    var focusedInput by remember { mutableStateOf<String?>("chapter") }
    var maxVerse by remember { mutableIntStateOf(0) }
    var isLoadingVerseCount by remember { mutableStateOf(false) }
    var showChapterFlash by remember { mutableStateOf(false) }
    var showVerseFlash by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var pendingVerseDigit by remember { mutableStateOf<String?>(null) }

    val selectedBibleBook by remember(selectedBook) {
        derivedStateOf {
            selectedBook?.let { BibleData.getBookByCustomNumber(it.bookNumber) }
        }
    }
    val isChapterValid by remember(chapterInput, selectedBibleBook) {
        derivedStateOf {
            val chapter = chapterInput.toIntOrNull()
            val maxChapters = selectedBibleBook?.chapters ?: 0
            chapter != null && chapter in 1..maxChapters
        }
    }
    val isVerseValid by remember(verseInput, maxVerse) {
        derivedStateOf {
            if (verseInput.isEmpty()) return@derivedStateOf true
            val verse = verseInput.toIntOrNull()
            verse != null && verse in 1..maxVerse
        }
    }
    val isChapterError by remember(chapterInput, selectedBibleBook) {
        derivedStateOf {
            if (chapterInput.isEmpty()) return@derivedStateOf false
            val chapter = chapterInput.toIntOrNull()
            val maxChapters = selectedBibleBook?.chapters ?: 0
            chapter == null || chapter == 0 || chapter > maxChapters
        }
    }
    val isVerseError by remember(verseInput, maxVerse, isLoadingVerseCount) {
        derivedStateOf {
            if (verseInput.isEmpty()) return@derivedStateOf false
            if (isLoadingVerseCount || maxVerse == 0) return@derivedStateOf false
            val verse = verseInput.toIntOrNull()
            verse == null || verse == 0 || verse > maxVerse
        }
    }
    val isInputValid by remember(chapterInput, verseInput, isChapterValid, isVerseValid) {
        derivedStateOf { isChapterValid && (verseInput.isEmpty() || isVerseValid) }
    }
    val chapterHint by remember(selectedBook) {
        derivedStateOf { selectedBook?.let { "1-${it.totalChapters}" } ?: "" }
    }
    val verseHint by remember(maxVerse, chapterInput, isLoadingVerseCount, focusedInput) {
        derivedStateOf {
            if (focusedInput != "verse" && verseInput.isEmpty()) return@derivedStateOf ""
            when {
                isLoadingVerseCount && focusedInput == "verse" -> "Loading..."
                chapterInput.isEmpty() -> ""
                maxVerse > 0 -> "1-$maxVerse"
                else -> ""
            }
        }
    }
    val performConfirm = {
        if (isInputValid && selectedBook != null) {
            val chapter = chapterInput.toInt()
            val verse = verseInput.toIntOrNull() ?: 1
            val bibleBook = BibleData.getBookByCustomNumber(selectedBook!!.bookNumber)
            onPassageSelected(
                PassageSelection(
                    bookNumber = selectedBook!!.bookNumber,
                    bookName = bibleBook?.name ?: selectedBook!!.longName,
                    chapter = chapter,
                    verse = verse
                )
            )
            onDismissRequest()
        }
    }
    val initialBook = remember(initialBookNumber) {
        initialBookNumber?.let { number ->
            (oldTestamentBooks + newTestamentBooks).find { it.bookNumber == number }
        }
    }
    var initialSet by remember { mutableStateOf(false) }
    LaunchedEffect(initialBook) {
        if (!initialSet && initialBook != null) {
            selectedBook = initialBook
            chapterInput = initialChapter.toString()
            if (initialVerse != null && initialVerse > 0) {
                verseInput = initialVerse.toString()
            }
            focusedInput = "chapter"
            initialSet = true
        }
    }
    LaunchedEffect(showChapterFlash) {
        if (showChapterFlash) {
            delay(500)
            showChapterFlash = false
        }
    }
    LaunchedEffect(showVerseFlash) {
        if (showVerseFlash) {
            delay(500)
            showVerseFlash = false
        }
    }
    LaunchedEffect(selectedBook, chapterInput) {
        val chapter = chapterInput.toIntOrNull()
        val bookNumber = selectedBook?.bookNumber
        if (chapter != null && bookNumber != null && chapter in 1..(selectedBook?.totalChapters ?: 0)) {
            isLoadingVerseCount = true
            val count = if (databaseHelper != null) {
                try {
                    withContext(Dispatchers.IO) {
                        databaseHelper.getVerseCount(bookNumber, chapter)
                    }
                } catch (_: Exception) {
                    selectedBibleBook?.getVersesForChapter(chapter) ?: 0
                }
            } else {
                selectedBibleBook?.getVersesForChapter(chapter) ?: 0
            }
            maxVerse = count
            isLoadingVerseCount = false
        } else {
            maxVerse = 0
            isLoadingVerseCount = false
        }
    }
    LaunchedEffect(maxVerse, pendingVerseDigit, focusedInput) {
        if (pendingVerseDigit != null && focusedInput == "verse" && maxVerse > 0) {
            val verseNum = pendingVerseDigit!!.toIntOrNull()
            if (verseNum != null && verseNum in 1..maxVerse) {
                verseInput = pendingVerseDigit!!
                if (verseNum * 10 > maxVerse) {
                    coroutineScope.launch {
                        delay(300)
                        performConfirm()
                    }
                }
            } else {
                showVerseFlash = true
            }
            pendingVerseDigit = null
        }
    }
    val onDigit: (String) -> Unit = { digit ->
        if (focusedInput == "chapter") {
            val newValue = chapterInput + digit
            val num = newValue.toIntOrNull() ?: 0
            val maxChapters = selectedBook?.totalChapters ?: 0
            if (num in 1..maxChapters && num.toString() == newValue) {
                chapterInput = newValue
                if (num * 10 > maxChapters) {
                    focusedInput = "verse"
                }
            } else {
                val currentNum = chapterInput.toIntOrNull()
                if (chapterInput.isNotEmpty() && currentNum != null && currentNum in 1..maxChapters) {
                    pendingVerseDigit = digit
                    focusedInput = "verse"
                    verseInput = ""
                } else {
                    showChapterFlash = true
                }
            }
        } else {
            val newValue = verseInput + digit
            val num = newValue.toIntOrNull() ?: 0
            if (num in 1..maxVerse && num.toString() == newValue) {
                verseInput = newValue
                if (num * 10 > maxVerse) {
                    coroutineScope.launch {
                        delay(300)
                        performConfirm()
                    }
                }
            } else {
                val currentNum = verseInput.toIntOrNull()
                if (verseInput.isNotEmpty() && currentNum != null && currentNum in 1..maxVerse) {
                    coroutineScope.launch {
                        delay(300)
                        performConfirm()
                    }
                } else {
                    showVerseFlash = true
                }
            }
        }
    }

    val onBackspace = {
        val isChapter = focusedInput == "chapter"
        val current = if (isChapter) chapterInput else verseInput
        if (current.isNotEmpty()) {
            if (isChapter) chapterInput = current.dropLast(1) else verseInput = current.dropLast(1)
        }
    }

    val onClear = {
        chapterInput = ""
        verseInput = ""
        maxVerse = 0
        focusedInput = "chapter"
        pendingVerseDigit = null
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = showNavigationModal
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Select Passage",
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.size(45.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                        ),
                        modifier = Modifier.height(48.dp)
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(2f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 10.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                item {
                                    TestamentSection(
                                        title = "Old Testament",
                                        books = oldTestamentBooks,
                                        onBookSelected = { book ->
                                            selectedBook = book
                                            chapterInput = ""
                                            verseInput = ""
                                            maxVerse = 0
                                            focusedInput = "chapter"
                                            pendingVerseDigit = null
                                        },
                                        textColor = MaterialTheme.colorScheme.primary,
                                        selectedBook = selectedBook
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                                item {
                                    TestamentSection(
                                        title = "New Testament",
                                        books = newTestamentBooks,
                                        onBookSelected = { book ->
                                            selectedBook = book
                                            chapterInput = ""
                                            verseInput = ""
                                            maxVerse = 0
                                            focusedInput = "chapter"
                                            pendingVerseDigit = null
                                        },
                                        textColor = MaterialTheme.colorScheme.secondary,
                                        selectedBook = selectedBook
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (selectedBook != null) {
                                    BookInputsSection(
                                        book = selectedBook!!,
                                        chapterInput = chapterInput,
                                        verseInput = verseInput,
                                        focusedInput = focusedInput,
                                        onFocusChange = { focusedInput = it },
                                        isChapterError = isChapterError,
                                        isVerseError = isVerseError,
                                        chapterHint = chapterHint,
                                        verseHint = verseHint,
                                        isChapterValid = isChapterValid,
                                        isVerseValid = isVerseValid,
                                        isLoadingVerseCount = isLoadingVerseCount,
                                        onConfirm = performConfirm,
                                        onDigit = onDigit,
                                        onBackspace = onBackspace,
                                        onClear = onClear,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = "Select a book",
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 10.dp, top = 0.dp),
                            state = listState
                        ) {
                            item { Spacer(modifier = Modifier.height(10.dp)) }
                            item {
                                TestamentSection(
                                    title = "Old Testament",
                                    books = oldTestamentBooks,
                                    onBookSelected = { book ->
                                        selectedBook = book
                                        chapterInput = ""
                                        verseInput = ""
                                        maxVerse = 0
                                        focusedInput = "chapter"
                                        pendingVerseDigit = null
                                    },
                                    textColor = MaterialTheme.colorScheme.primary,
                                    selectedBook = selectedBook
                                )
                            }
                            item { Spacer(modifier = Modifier.height(10.dp)) }
                            item {
                                TestamentSection(
                                    title = "New Testament",
                                    books = newTestamentBooks,
                                    onBookSelected = { book ->
                                        selectedBook = book
                                        chapterInput = ""
                                        verseInput = ""
                                        maxVerse = 0
                                        focusedInput = "chapter"
                                        pendingVerseDigit = null
                                    },
                                    textColor = MaterialTheme.colorScheme.secondary,
                                    selectedBook = selectedBook
                                )
                            }
                            selectedBook?.let { book ->
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                                item {
                                    BookInputsSection(
                                        book = book,
                                        chapterInput = chapterInput,
                                        verseInput = verseInput,
                                        focusedInput = focusedInput,
                                        onFocusChange = { focusedInput = it },
                                        isChapterError = isChapterError,
                                        isVerseError = isVerseError,
                                        chapterHint = chapterHint,
                                        verseHint = verseHint,
                                        isChapterValid = isChapterValid,
                                        isVerseValid = isVerseValid,
                                        isLoadingVerseCount = isLoadingVerseCount,
                                        onConfirm = performConfirm,
                                        onDigit = onDigit,
                                        onBackspace = onBackspace,
                                        onClear = onClear,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookInputsSection(
    book: BookUi,
    chapterInput: String,
    verseInput: String,
    focusedInput: String?,
    onFocusChange: (String?) -> Unit,
    isChapterError: Boolean,
    isVerseError: Boolean,
    chapterHint: String,
    verseHint: String,
    isChapterValid: Boolean,
    isVerseValid: Boolean,
    isLoadingVerseCount: Boolean,
    onConfirm: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BookHeader(book = book)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chapter",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                CustomInputDisplay(
                    value = chapterInput,
                    hint = chapterHint,
                    isFocused = focusedInput == "chapter",
                    isError = isChapterError,
                    onClick = { onFocusChange("chapter") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Verse",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                CustomInputDisplay(
                    value = verseInput,
                    hint = verseHint,
                    isFocused = focusedInput == "verse",
                    isError = isVerseError,
                    onClick = {
                        if (chapterInput.isNotEmpty()) {
                            onFocusChange("verse")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(15.dp))
        NumPad(
            onDigit = onDigit,
            onBackspace = onBackspace,
            onClear = onClear,
            onConfirm = onConfirm,
            isEnabled = isChapterValid && (verseInput.isEmpty() || isVerseValid),
            selectedBook = book,
            chapterInput = chapterInput,
            verseInput = verseInput,
            isLoadingVerseCount = isLoadingVerseCount,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CustomInputDisplay(
    value: String,
    hint: String,
    isFocused: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error else if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = 2.dp
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFF5F5DC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NumPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    isEnabled: Boolean,
    selectedBook: BookUi?,
    chapterInput: String,
    verseInput: String,
    isLoadingVerseCount: Boolean = false,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            NumButton("1", onDigit, Modifier.weight(1f))
            NumButton("2", onDigit, Modifier.weight(1f))
            NumButton("3", onDigit, Modifier.weight(1f))
            NumButton("4", onDigit, Modifier.weight(1f))
            NumButton("5", onDigit, Modifier.weight(1f))
            ActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                text = null,
                contentDescription = "Backspace",
                onClick = onBackspace,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            NumButton("6", onDigit, Modifier.weight(1f))
            NumButton("7", onDigit, Modifier.weight(1f))
            NumButton("8", onDigit, Modifier.weight(1f))
            NumButton("9", onDigit, Modifier.weight(1f))
            NumButton("0", onDigit, Modifier.weight(1f))
            ActionButton(
                icon = Icons.Filled.Check,
                text = null,
                contentDescription = "Confirm",
                onClick = onConfirm,
                containerColor = if (isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                contentColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                icon = null,
                text = "Clear",
                contentDescription = "Clear",
                onClick = onClear,
                containerColor = Color(0xFFF44336),
                contentColor = Color.White,
                modifier = Modifier.weight(0.33f)
            )
            ActionButton(
                icon = null,
                text = if (isLoadingVerseCount) {
                    ""
                } else {
                    val verseText = if (verseInput.isNotEmpty()) ":$verseInput" else ""
                    "Go to ${selectedBook?.longName ?: ""} $chapterInput$verseText"
                },
                contentDescription = "Confirm",
                onClick = onConfirm,
                containerColor = if (isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                contentColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.weight(0.675f)
            )
        }
    }
}

@Composable
fun NumButton(
    digit: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onClick(digit) },
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = digit, fontSize = 24.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActionButton(
    icon: ImageVector? = null,
    text: String? = null,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(2.dp),
        enabled = enabled
    ) {
        if (text != null) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun BookHeader(book: BookUi) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .padding(9.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = book.longName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "${book.totalChapters} chapters",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.shortName,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TestamentSection(
    title: String,
    books: List<BookUi>,
    onBookSelected: (BookUi) -> Unit,
    textColor: Color,
    selectedBook: BookUi?
) {
    Column(modifier = Modifier.padding(bottom = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 5.dp, end = 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
            Text(
                text = "${books.size} books",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            books.forEach { book ->
                BookCard(
                    book = book,
                    isSelected = selectedBook?.bookNumber == book.bookNumber,
                    textColor = textColor,
                    onClick = { onBookSelected(book) }
                )
            }
        }
    }
}

@Composable
fun BookCard(
    book: BookUi,
    isSelected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val themeIsDark = MaterialTheme.colorScheme.background.red + MaterialTheme.colorScheme.background.green + MaterialTheme.colorScheme.background.blue < 1.5f
    val scope = getScopeForBookNumber(book.bookNumber)
    val hex = scope?.let { scopeColors[it] } ?: if (book.testament == Testament.OLD) "#DC2626" else "#059669"
    val baseColor = Color(hex.toColorInt())
    val bgColor = if (themeIsDark) baseColor else lerp(Color.White, baseColor, 0.85f)
    val borderModifier = if (isSelected) Modifier.border(4.dp, textColor, RoundedCornerShape(4.dp)) else Modifier
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(47.dp)
            .height(40.dp)
            .then(borderModifier)
            .padding(1.dp),

        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor,
            contentColor = Color(0xFF444c69)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = book.shortName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}