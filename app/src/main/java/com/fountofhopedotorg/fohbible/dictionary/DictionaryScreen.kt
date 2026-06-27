package com.fountofhopedotorg.fohbible.dictionary

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.modals.buildDefinitionContent
import com.fountofhopedotorg.fohbible.modals.getDefinitionOrClosest
import com.fountofhopedotorg.fohbible.modals.parseVerseLink
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.modals.InteractiveModal
import com.fountofhopedotorg.fohbible.utils.Fonts
import com.fountofhopedotorg.fohbible.utils.InteractiveModalUtils.dictionariesByLanguage
import com.fountofhopedotorg.fohbible.utils.InteractiveModalUtils.dictionaryDisplayNames
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DictionaryScreen(
    onNavigateToReader: (PassageSelection) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scope = rememberCoroutineScope()

    val keyboardController = LocalSoftwareKeyboardController.current
    var searchWord by rememberSaveable { mutableStateOf("") }
    var resultTitle by rememberSaveable { mutableStateOf("") }
    var resultContent by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var dictionaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }

    val selectedDictionary = viewModel.selectedPrimaryDictionary
    val selectedDictLanguage = viewModel.selectedPrimaryDictLanguage
    val dictionaries = remember(selectedDictLanguage) {
        dictionariesByLanguage[selectedDictLanguage] ?: emptyList()
    }

    var dictionaryDropdownExpanded by remember { mutableStateOf(false) }

    var showVerseModal by remember { mutableStateOf(false) }
    var modalBookNumber by remember { mutableIntStateOf(0) }
    var modalChapter by remember { mutableIntStateOf(0) }
    var modalVerse by remember { mutableIntStateOf(0) }
    var modalIsOldTestament by remember { mutableStateOf(false) }
    var mainBibleHelper by remember { mutableStateOf<DatabaseHelper?>(null) }

    LaunchedEffect(selectedDictionary) {
        dictionaryDbHelper?.close()
        dictionaryDbHelper = DatabaseHelper(context, "${selectedDictionary}.dictionary.sqlite3")
    }

    LaunchedEffect(viewModel.currentDbName) {
        mainBibleHelper?.close()
        mainBibleHelper = DatabaseHelper(context, viewModel.currentDbName)
    }

    DisposableEffect(Unit) {
        onDispose {
            dictionaryDbHelper?.close()
            mainBibleHelper?.close()
        }
    }

    fun performSearch(word: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        val helper = dictionaryDbHelper ?: return

        keyboardController?.hide()

        isLoading = true
        scope.launch {
            val pairs: List<Pair<String, String>> = getDefinitionOrClosest(helper, trimmed) ?: emptyList()
            val capitalizedWord = trimmed.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
            val isTopical = selectedDictionary == "topical"
            val newTitle = if (pairs.isNotEmpty()) {
                val isExact = pairs.size == 1 && pairs[0].first.equals(trimmed, ignoreCase = true)
                if (isTopical) "References for $capitalizedWord"
                else if (isExact) "Definition of ${
                    pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }
                }"
                else if (pairs.size == 1) "Closest match for ${
                    pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }
                }"
                else "Matches for \"$capitalizedWord\""
            } else "Definition of $capitalizedWord not found"

            val newContent = buildDefinitionContent(
                originalWord = trimmed,
                pairs = pairs,
                isOxford = selectedDictionary == "oxford",
                isTopical = isTopical
            )
            resultTitle = newTitle
            resultContent = newContent
            isLoading = false
        }
    }

    LaunchedEffect(selectedDictionary) {
        if (searchWord.isNotBlank()) {
            performSearch(searchWord)
        }
    }

    val onLinkClick: (String) -> Unit = { href ->
        if (href.startsWith("B:")) {
            val passage = parseVerseLink(href, href.removePrefix("B:"))
            if (passage != null) {
                val book = BibleData.getBookByCustomNumber(passage.bookNumber)
                val isOld = book?.testament == Testament.OLD

                modalBookNumber = passage.bookNumber
                modalChapter = passage.chapter
                modalVerse = passage.verse ?: 1
                modalIsOldTestament = isOld
                showVerseModal = true
            }
        }
    }

    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchWord,
                onValueChange = { searchWord = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Search word…") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { performSearch(searchWord) }) {
                Text("Search", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = { dictionaryDropdownExpanded = true }) {
                Text(
                    text = "Dictionary: ${dictionaryDisplayNames[selectedDictionary] ?: selectedDictionary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (dictionaries.size > 1) {
                Spacer(modifier = Modifier.size(2.dp))
                val currentDictIndex = dictionaries.indexOf(selectedDictionary)
                val nextDict = if (currentDictIndex != -1) {
                    dictionaries[(currentDictIndex + 1) % dictionaries.size]
                } else {
                    dictionaries.first()
                }
                IconButton(
                    onClick = {
                        viewModel.selectedPrimaryDictionary = nextDict
                    },
                    modifier = Modifier.size(28.dp).padding(start = 10.dp)
                ) {
                    Icon(
                        Icons.Filled.FastForward,
                        contentDescription = "Next Dictionary",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                DropdownMenu(
                    expanded = dictionaryDropdownExpanded,
                    onDismissRequest = { dictionaryDropdownExpanded = false }
                ) {
                    dictionaries.forEach { dictKey ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = dictionaryDisplayNames[dictKey] ?: dictKey,
                                    fontWeight = if (dictKey == selectedDictionary) FontWeight.Bold else FontWeight.Normal,
                                    color = if (dictKey == selectedDictionary) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                dictionaryDropdownExpanded = false
                                viewModel.selectedPrimaryDictionary = dictKey
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (resultContent.isNotEmpty()) {
            Text(
                text = resultTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        textSize = viewModel.fontSize.toFloat() * 0.85f
                        setLineSpacing(0f, 1.333f)
                        typeface = Fonts.getTypeface(ctx, viewModel.selectedFontFamily)
                        textDirection = View.TEXT_DIRECTION_LTR
                        gravity = Gravity.START
                    }
                },
                update = { textView ->
                    val spanned = HtmlCompat.fromHtml(resultContent, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    val spannable = SpannableString(spanned)
                    val urlSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
                    for (urlSpan in urlSpans) {
                        val start = spannable.getSpanStart(urlSpan)
                        val end = spannable.getSpanEnd(urlSpan)
                        val flags = spannable.getSpanFlags(urlSpan)
                        val href = urlSpan.url
                        spannable.removeSpan(urlSpan)

                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                onLinkClick(href)
                            }
                        }
                        spannable.setSpan(clickableSpan, start, end, flags)
                    }
                    textView.setTextColor(textColor.toArgb())
                    textView.text = spannable
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enter a word to look up its definition.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showVerseModal) {
        InteractiveModal(
            show = true,
            onDismiss = { showVerseModal = false },
            onNavigateToReader = { passage ->
                showVerseModal = false
                onNavigateToReader(passage)
            },
            databaseHelper = mainBibleHelper,
            initialType = "verses",
            bookNumber = modalBookNumber,
            chapter = modalChapter,
            verse = modalVerse,
            isOldTestament = modalIsOldTestament
        )
    }
}