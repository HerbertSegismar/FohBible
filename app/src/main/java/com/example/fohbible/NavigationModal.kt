package com.example.fohbible

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.ui.theme.FohBibleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BookUi(
    val bookNumber: Int,
    val longName: String,
    val shortName: String,
    val testament: Testament,
    val totalChapters: Int
)

fun BibleBook.toBookUi(): BookUi {
    return BookUi(
        bookNumber = customNumber,
        longName = name,
        shortName = abbreviation,
        testament = testament,
        totalChapters = chapters
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationModal(
    onDismissRequest: () -> Unit,
    onPassageSelected: (PassageSelection) -> Unit = { _ -> },
    showNavigationModal: Boolean,
    databaseHelper: DatabaseHelper? = null
) {
    val oldTestamentBooks = remember { BibleData.oldTestamentBooks.map { it.toBookUi() } }
    val newTestamentBooks = remember { BibleData.newTestamentBooks.map { it.toBookUi() } }

    var selectedBook by remember { mutableStateOf<BookUi?>(null) }
    var chapterInput by remember { mutableStateOf("") }
    var verseInput by remember { mutableStateOf("") }
    var focusedInput by remember { mutableStateOf<String?>("chapter") }

    var maxVerse by remember { mutableIntStateOf(0) }
    var isLoadingVerseCount by remember { mutableStateOf(false) }

    val selectedBibleBook by remember(selectedBook) {
        derivedStateOf {
            selectedBook?.let { BibleData.getBookByCustomNumber(it.bookNumber) }
        }
    }

    LaunchedEffect(focusedInput, chapterInput, selectedBook) {
        if (focusedInput == "verse") {
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

                val currentVerse = verseInput.toIntOrNull()
                if (currentVerse != null && currentVerse > maxVerse) {
                    verseInput = ""
                }
            } else {
                maxVerse = 0
                isLoadingVerseCount = false
            }
        } else {
            val chapter = chapterInput.toIntOrNull()
            val bookNumber = selectedBook?.bookNumber

            if (chapter == null || bookNumber == null || chapter !in 1..(selectedBook?.totalChapters ?: 0)) {
                maxVerse = 0
            }
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

    val isVerseError by remember(verseInput, maxVerse) {
        derivedStateOf {
            if (verseInput.isEmpty()) return@derivedStateOf false
            val verse = verseInput.toIntOrNull()
            verse == null || verse == 0 || verse > maxVerse
        }
    }

    val isInputValid by remember(chapterInput, verseInput, isChapterValid, isVerseValid) {
        derivedStateOf {
            isChapterValid && (verseInput.isEmpty() || isVerseValid)
        }
    }

    val chapterHint by remember(selectedBook) {
        derivedStateOf {
            selectedBook?.let { "1-${it.totalChapters}" } ?: ""
        }
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = showNavigationModal
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (selectedBook == null) "Select a Book" else "Select Passage for ${selectedBook?.longName}",
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (selectedBook != null) {
                                        selectedBook = null
                                        chapterInput = ""
                                        verseInput = ""
                                        maxVerse = 0
                                    } else {
                                        onDismissRequest()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, top = 2.dp),
                        state = listState
                    ) {
                        item { Spacer(modifier = Modifier.height(2.dp)) }
                        item {
                            TestamentSection(
                                title = "Old Testament",
                                books = oldTestamentBooks,
                                onBookSelected = { book ->
                                    selectedBook = book
                                    chapterInput = ""
                                    verseInput = ""
                                    maxVerse = 0
                                },
                                defaultColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                textColor = MaterialTheme.colorScheme.primary,
                                selectedBook = selectedBook
                            )
                        }
                        item { Spacer(modifier = Modifier.height(18.dp)) }
                        item {
                            TestamentSection(
                                title = "New Testament",
                                books = newTestamentBooks,
                                onBookSelected = { book ->
                                    selectedBook = book
                                    chapterInput = ""
                                    verseInput = ""
                                    maxVerse = 0
                                },
                                defaultColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                textColor = MaterialTheme.colorScheme.secondary,
                                selectedBook = selectedBook
                            )
                        }

                        selectedBook?.let { book ->
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                            item { BookHeader(book = book) }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Chapter",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        CustomInputDisplay(
                                            value = chapterInput,
                                            hint = chapterHint,
                                            isFocused = focusedInput == "chapter",
                                            isError = isChapterError,
                                            onClick = { focusedInput = "chapter" },
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
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        CustomInputDisplay(
                                            value = verseInput,
                                            hint = verseHint,
                                            isFocused = focusedInput == "verse",
                                            isError = isVerseError,
                                            onClick = {
                                                if (chapterInput.isNotEmpty()) {
                                                    focusedInput = "verse"
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                            item {
                                NumPad(
                                    onDigit = { digit ->
                                        if (focusedInput == "chapter") {
                                            val newValue = chapterInput + digit
                                            val num = newValue.toIntOrNull() ?: 0
                                            val maxChapters = book.totalChapters

                                            if (num in 1..maxChapters && num.toString() == newValue) {
                                                chapterInput = newValue
                                                if (num * 10 > maxChapters) {
                                                    focusedInput = "verse"
                                                }
                                            } else if (chapterInput.isNotEmpty() && chapterInput.toIntOrNull() in 1..maxChapters) {
                                                focusedInput = "verse"
                                                val newVerseValue = verseInput + digit
                                                val verseNum = newVerseValue.toIntOrNull() ?: 0
                                                if (verseNum in 1..maxVerse && verseNum.toString() == newVerseValue) {
                                                    verseInput = newVerseValue
                                                }
                                            }
                                        } else {
                                            val newValue = verseInput + digit
                                            val num = newValue.toIntOrNull() ?: 0
                                            if (num in 1..maxVerse && num.toString() == newValue) {
                                                verseInput = newValue
                                            }
                                        }
                                    },
                                    onBackspace = {
                                        val isChapter = focusedInput == "chapter"
                                        val current = if (isChapter) chapterInput else verseInput
                                        if (current.isNotEmpty()) {
                                            if (isChapter) chapterInput = current.dropLast(1) else verseInput = current.dropLast(1)
                                        }
                                    },
                                    onClear = {
                                        chapterInput = ""
                                        verseInput = ""
                                        maxVerse = 0
                                        focusedInput = "chapter"
                                    },
                                    onConfirm = {
                                        if (isInputValid) {
                                            val chapter = chapterInput.toInt()
                                            val verse = verseInput.toIntOrNull()
                                            selectedBook?.let { book ->
                                                val bibleBook = BibleData.getBookByCustomNumber(book.bookNumber)
                                                onPassageSelected(
                                                    PassageSelection(
                                                        bookNumber = book.bookNumber,
                                                        bookName = bibleBook?.name ?: book.longName,
                                                        chapter = chapter,
                                                        verse = verse
                                                    )
                                                )
                                            }
                                            onDismissRequest()
                                        }
                                    },
                                    isEnabled = isInputValid,
                                    selectedBook = book,
                                    chapterInput = chapterInput,
                                    verseInput = verseInput,
                                    isLoadingVerseCount = isLoadingVerseCount,
                                )
                            }
                        }
                    }

                    // Auto-focus chapter and scroll to inputs when book is selected
                    LaunchedEffect(selectedBook) {
                        if (selectedBook != null) {
                            focusedInput = "chapter"
                            listState.animateScrollToItem(7)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomInputDisplay(
    value: String,
    hint: String,
    isFocused: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error
    else if (isFocused) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    val borderWidth = if (isError) 2.dp else 1.dp

    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        color = if (isError) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceVariant
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
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 14.sp,
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
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
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
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                contentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                enabled = isEnabled && !isLoadingVerseCount,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                icon = null,
                text = "Clear",
                contentDescription = "Clear",
                onClick = onClear,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
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
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                contentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                enabled = isEnabled && !isLoadingVerseCount,
                isLoading = isLoadingVerseCount,
                modifier = Modifier.weight(1f)
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
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
    isLoading: Boolean = false,
    @SuppressLint("ModifierParameter")
    modifier: Modifier = Modifier
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
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
        } else if (text != null) {
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
            containerColor = when (book.testament) {
                Testament.OLD -> MaterialTheme.colorScheme.primaryContainer
                Testament.NEW -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = book.longName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    fontSize = 20.sp,
                    color = when (book.testament) {
                        Testament.OLD -> MaterialTheme.colorScheme.onPrimaryContainer
                        Testament.NEW -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                Text(
                    text = "${book.totalChapters} chapters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (book.testament) {
                            Testament.OLD -> MaterialTheme.colorScheme.primary
                            Testament.NEW -> MaterialTheme.colorScheme.secondary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.shortName,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestamentSection(
    title: String,
    books: List<BookUi>,
    onBookSelected: (BookUi) -> Unit,
    defaultColor: Color,
    textColor: Color,
    selectedBook: BookUi?
) {
    Column(modifier = Modifier.padding(bottom = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
            Text(
                text = "${books.size} books",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            books.forEach { book ->
                BookCard(
                    book = book,
                    backgroundColor = if (selectedBook?.bookNumber == book.bookNumber) {
                        textColor.copy(alpha = 0.2f)
                    } else {
                        defaultColor
                    },
                    textColor = textColor,
                    onClick = { onBookSelected(book) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(
    book: BookUi,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(50.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
    ) {
        Column(
            modifier = Modifier.padding(5.dp, 10.dp),
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
@Preview(showBackground = true, name = "Navigation Modal Light")
@Composable
fun NavigationModalPreviewLight() {
    FohBibleTheme(darkTheme = false) {
        NavigationModal(
            onDismissRequest = {},
            onPassageSelected = {},
            showNavigationModal = true,
            databaseHelper = null
        )
    }
}

@Preview(showBackground = true, name = "Navigation Modal Dark")
@Composable
fun NavigationModalPreviewDark() {
    FohBibleTheme(darkTheme = true) {
        NavigationModal(
            onDismissRequest = {},
            onPassageSelected = {},
            showNavigationModal = true,
            databaseHelper = null
        )
    }
}