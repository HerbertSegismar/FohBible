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
import com.example.fohbible.ui.theme.FohBibleTheme

// Data class for UI representation
data class BookUi(
    val bookNumber: Int,
    val longName: String,
    val shortName: String,
    val testament: Testament,
    val totalChapters: Int
)

// Conversion from BibleBook to BookUi
fun BibleBook.toBookUi(): BookUi {
    return BookUi(
        bookNumber = number,
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
    onPassageSelected: (bookName: String, chapter: Int, verse: Int?) -> Unit = { _, _, _ -> },
    showNavigationModal: Boolean
) {
    // Data from BibleData
    val oldTestamentBooks = remember { BibleData.oldTestamentBooks.map { it.toBookUi() } }
    val newTestamentBooks = remember { BibleData.newTestamentBooks.map { it.toBookUi() } }

    // State for book selection and input
    var selectedBook by remember { mutableStateOf<BookUi?>(null) }
    var chapterInput by remember { mutableStateOf("") }
    var verseInput by remember { mutableStateOf("") }
    var focusedInput by remember { mutableStateOf<String?>("chapter") }

    // Get the selected BibleBook for validation
    val selectedBibleBook by remember(selectedBook) {
        derivedStateOf { selectedBook?.let { BibleData.getBookByNumber(it.bookNumber) } }
    }

    val maxVerse by remember(chapterInput, selectedBibleBook) {
        derivedStateOf {
            val chapter = chapterInput.toIntOrNull()
            if (chapter != null && chapter in 1..(selectedBibleBook?.chapters ?: 0)) {
                selectedBibleBook?.getVersesForChapter(chapter) ?: 0
            } else {
                0
            }
        }
    }

    // Validate inputs
    val isInputValid by remember(chapterInput, verseInput, selectedBibleBook, maxVerse) {
        derivedStateOf {
            val chapter = chapterInput.toIntOrNull()
            val verse = verseInput.toIntOrNull()
            chapter != null && chapter in 1..(selectedBibleBook?.chapters ?: 0) &&
                    (verse == null || (verse in 1..maxVerse))
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
                                        // Deselect book and clear inputs
                                        selectedBook = null
                                        chapterInput = ""
                                        verseInput = ""
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
                        contentPadding = PaddingValues(bottom = 16.dp, top = 2.dp), // Added bottom padding
                        state = listState
                    ) {
                        // Always show book selection
                        item { Spacer(modifier = Modifier.height(2.dp)) }
                        item {
                            TestamentSection(
                                title = "Old Testament",
                                books = oldTestamentBooks,
                                onBookSelected = { book ->
                                    selectedBook = book
                                    chapterInput = ""
                                    verseInput = ""
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
                                },
                                defaultColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                textColor = MaterialTheme.colorScheme.secondary,
                                selectedBook = selectedBook
                            )
                        }

                        // Show chapter/verse selection if a book is selected
                        selectedBook?.let { book ->
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                            item { BookHeader(book = book) }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                            // Chapter and verse inputs in a row
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
                                            hint = "1-${book.totalChapters}",
                                            isFocused = focusedInput == "chapter",
                                            isError = chapterInput.isNotEmpty() && (chapterInput.toIntOrNull() ?: 0) !in 1..book.totalChapters,
                                            onClick = { focusedInput = "chapter" },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Verse (Optional)",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        CustomInputDisplay(
                                            value = verseInput,
                                            hint = if (maxVerse > 0) "1-$maxVerse" else "",
                                            isFocused = focusedInput == "verse",
                                            isError = verseInput.isNotEmpty() && chapterInput.isNotEmpty() && chapterInput.toIntOrNull()?.let { chapter ->
                                                val verse = verseInput.toIntOrNull()
                                                verse != null && verse !in 1..maxVerse
                                            } == true,
                                            onClick = { focusedInput = "verse" },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                            // Custom numpad
                            item {
                                NumPad(
                                    onDigit = { digit ->
                                        if (focusedInput == "chapter") {
                                            val newValueChapter = chapterInput + digit
                                            val numChapter = newValueChapter.toIntOrNull() ?: 0
                                            val maxChapter = book.totalChapters
                                            if (numChapter <= maxChapter && numChapter.toString() == newValueChapter) {
                                                chapterInput = newValueChapter
                                                // Check if no more inputs allowed
                                                if (numChapter * 10 > maxChapter) {
                                                    focusedInput = "verse"
                                                }
                                            } else if (chapterInput.isNotEmpty()) {
                                                val currentNum = chapterInput.toIntOrNull() ?: 0
                                                if (currentNum in 1..maxChapter) {
                                                    focusedInput = "verse"
                                                    // Add digit to verse
                                                    val newValueVerse = verseInput + digit
                                                    val numVerse = newValueVerse.toIntOrNull() ?: 0
                                                    val maxV = maxVerse
                                                    if (numVerse <= maxV && numVerse.toString() == newValueVerse) {
                                                        verseInput = newValueVerse
                                                    }
                                                }
                                            }
                                        } else {
                                            // For verse
                                            val newValue = verseInput + digit
                                            val num = newValue.toIntOrNull() ?: 0
                                            val maxV = maxVerse
                                            if (num <= maxV && num.toString() == newValue) {
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
                                        focusedInput = "chapter"
                                    },
                                    onConfirm = {
                                        if (isInputValid) {
                                            val chapter = chapterInput.toInt()
                                            val verse = verseInput.toIntOrNull()
                                            onPassageSelected(book.longName, chapter, verse)
                                            onDismissRequest()
                                        }
                                    },
                                    isEnabled = isInputValid,
                                    selectedBook = book,
                                    chapterInput = chapterInput,
                                    verseInput = verseInput
                                )
                            }
                        }
                    }

                    // Auto-focus chapter and scroll to inputs when book is selected
                    LaunchedEffect(selectedBook) {
                        if (selectedBook != null) {
                            focusedInput = "chapter"
                            listState.animateScrollToItem(7) // Index of the inputs Row
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
    modifier: Modifier = Modifier
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error
    else if (isFocused) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    modifier = Modifier.align(Alignment.CenterStart),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp, // Reduced hint font size
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
    modifier: Modifier = Modifier
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
                enabled = isEnabled,
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
                text = "Go to ${selectedBook?.longName ?: ""} $chapterInput" + if (verseInput.isNotEmpty()) ":$verseInput" else "",
                contentDescription = "Confirm",
                onClick = onConfirm,
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                contentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                enabled = isEnabled,
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
            modifier = Modifier.padding(8.dp),
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
            onPassageSelected = { _, _, _ -> },
            showNavigationModal = true
        )
    }
}

@Preview(showBackground = true, name = "Navigation Modal Dark")
@Composable
fun NavigationModalPreviewDark() {
    FohBibleTheme(darkTheme = true) {
        NavigationModal(
            onDismissRequest = {},
            onPassageSelected = { _, _, _ -> },
            showNavigationModal = true
        )
    }
}