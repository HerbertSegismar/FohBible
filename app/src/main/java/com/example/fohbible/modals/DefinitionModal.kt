@file:Suppress("AssignedValueIsNeverRead")

package com.example.fohbible.modals

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.AppViewModel
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.utils.ProcessedVerse
import com.example.fohbible.utils.ThemeColors
import com.example.fohbible.utils.VerseTextProcessor
import java.util.Locale

@Composable
fun DefinitionModal(
    show: Boolean,
    onDismiss: () -> Unit,
    word: String,
    definition: String,
    selectedDictionary: String,
    onSwitch: () -> Unit,
    databaseHelper: DatabaseHelper?
) {
    if (show) {
        val viewModel = viewModel<AppViewModel>()
        val context = LocalContext.current
        val themeColors = ThemeColors(
            textColor = MaterialTheme.colorScheme.onBackground,
            verseNumber = MaterialTheme.colorScheme.primary,
            primary = MaterialTheme.colorScheme.primary,
            tagColor = MaterialTheme.colorScheme.secondary,
            tagBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            wordsOfJesus = androidx.compose.ui.graphics.Color(0xFFDA4227),
            searchHighlightBg = if (viewModel.darkTheme) androidx.compose.ui.graphics.Color(0xFF81D4FA).copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.3f),
            highlightIcon = MaterialTheme.colorScheme.primary
        )
        val systemFont = FontFamily.Default
        val oswaldFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Oswald.ttf")) }
        val poppinsFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Poppins.ttf")) }
        val rubikGlitchFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/RubikGlitch.ttf")) }
        val currentFontFamily = when (viewModel.selectedFontFamily) {
            "system" -> systemFont
            "oswald" -> oswaldFont
            "rubik-glitch" -> rubikGlitchFont
            "poppins" -> poppinsFont
            else -> systemFont
        }
        var dictionaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
        var strongDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
        var commentaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }

        LaunchedEffect(viewModel.selectedDictionary, databaseHelper?.databaseName) {
            dictionaryDbHelper?.close()
            dictionaryDbHelper = DatabaseHelper(context, "${viewModel.selectedDictionary}.dictionary.sqlite3")
            strongDbHelper?.close()
            strongDbHelper = DatabaseHelper(context, "secedictionary.sqlite3")
            commentaryDbHelper?.close()
            val name = databaseHelper?.databaseName ?: return@LaunchedEffect
            val comName = name.replace(".sqlite3", "com.sqlite3")
            commentaryDbHelper = if (comName.isNotEmpty()) DatabaseHelper(context, comName) else null
        }

        var showNestedWordModal by remember { mutableStateOf(false) }
        var currentNestedWord by remember { mutableStateOf("") }
        var nestedWordDefinition by remember { mutableStateOf("") }

        var showStrongsModal by remember { mutableStateOf(false) }
        var currentStrongNumber by remember { mutableStateOf("") }
        var strongDefinition by remember { mutableStateOf("") }

        val stack = remember { mutableStateListOf<ModalPage>() }
        LaunchedEffect(true, word, definition, selectedDictionary) {
            if (show) {
                stack.clear()
                val dbDisplayName = if (selectedDictionary == "noah") "Noah" else "ATSBD"
                val title = "Definition of " + word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } + " - $dbDisplayName"
                stack.add(ModalPage(title, "definition", content = definition, null, null))
            }
        }



        val onWordPress: (String) -> Unit = { w ->
            val trimmed = w.trim()
            val def = dictionaryDbHelper?.getWordDefinition(trimmed) ?: "Definition not found."
            currentNestedWord = trimmed
            nestedWordDefinition = def
            showNestedWordModal = true
        }

        val onStrongsPress: (String, Int) -> Unit = { strongNumber, bookNumber ->
            val trimmed = strongNumber.trim()
            val isOldTestament = bookNumber <= 39
            val prefixed = if (trimmed.firstOrNull()?.isLetter() ?: false) {
                trimmed
            } else {
                (if (isOldTestament) "H" else "G") + trimmed
            }
            val definition = strongDbHelper?.getStrongDefinition(prefixed) ?: "Strong's definition not found."
            currentStrongNumber = prefixed
            strongDefinition = definition
            showStrongsModal = true
        }

        val onTagPress: (String, PassageSelection) -> Unit = { marker, passage ->
            val bookNumber = passage.bookNumber
            val chapter = passage.chapter
            val bookName = passage.bookName
            val start = passage.verse
            val end = passage.verseEnd ?: start
            val commentaries = (start..end).mapNotNull { verseNum ->
                val text = commentaryDbHelper?.getCommentary(bookNumber, chapter, verseNum, marker)
                if (text?.isNotBlank() == true) "Verse $verseNum:\n$text" else null
            }
            val combined = if (commentaries.isNotEmpty()) {
                commentaries.joinToString("\n\n────────────────────────\n\n")
            } else {
                "No commentary found for marker \"$marker\" in this passage."
            }
            val rangeStr = if (end != start) "$start-$end" else "$start"
            val newTitle = "Notes on $bookName $chapter:$rangeStr – $marker"
            val sanitizedCombined = sanitizeCommentaryContent(combined)
            stack.add(ModalPage(newTitle, "commentary", sanitizedCombined, null, null))
        }

        if (stack.isEmpty()) return
        val currentPage = stack.last()
        val textColor = MaterialTheme.colorScheme.onBackground
        val linkColor = MaterialTheme.colorScheme.primary
        val other = if (selectedDictionary == "noah") "ATSBD" else "Noah"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = currentPage.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                if (currentPage.type == "definition" || currentPage.type == "commentary") {
                    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                movementMethod = LinkMovementMethod.getInstance()
                                textSize = viewModel.fontSize.toFloat() * 0.85f
                                setLineSpacing(0f, 1.333f)
                                typeface = when (viewModel.selectedFontFamily) {
                                    "system" -> Typeface.DEFAULT
                                    "oswald" -> Typeface.createFromAsset(ctx.assets, "fonts/Oswald.ttf")
                                    "rubik-glitch" -> Typeface.createFromAsset(ctx.assets, "fonts/RubikGlitch.ttf")
                                    "poppins" -> Typeface.createFromAsset(ctx.assets, "fonts/Poppins.ttf")
                                    else -> Typeface.DEFAULT
                                }
                            }
                        },
                        update = { textView ->
                            val spanned = HtmlCompat.fromHtml(currentPage.content ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT)
                            val spannable = SpannableString(spanned)
                            val urlSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
                            for (urlSpan in urlSpans) {
                                val start = spannable.getSpanStart(urlSpan)
                                val end = spannable.getSpanEnd(urlSpan)
                                val flags = spannable.getSpanFlags(urlSpan)
                                val href = urlSpan.url
                                val linkText = spannable.substring(start, end)
                                if (href.startsWith("B:")) {
                                    val passage = parseVerseLink(href, linkText)
                                    if (passage != null) {
                                        val clickableSpan = object : ClickableSpan() {
                                            override fun onClick(widget: View) {
                                                val verses = fetchVerses(passage, databaseHelper)
                                                val newTitle = "${passage.bookName} ${passage.chapter}:${passage.verse}" + if (passage.verseEnd != null) "-${passage.verseEnd}" else ""
                                                stack.add(ModalPage(newTitle, "verses", null, verses, passage))
                                            }
                                        }
                                        spannable.setSpan(clickableSpan, start, end, flags)
                                        spannable.removeSpan(urlSpan)
                                    }
                                } else if (href.startsWith("S:")) {
                                    val word = linkText.trim()
                                    val clickableSpan = object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            val definition = dictionaryDbHelper?.getWordDefinition(word) ?: "Definition not found."
                                            val dbDisplayName = if (viewModel.selectedDictionary == "noah") "Noah" else "ATSBD"
                                            val newTitle = "Definition of " + word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } + " - $dbDisplayName"
                                            stack.add(ModalPage(newTitle, "definition", definition, null, null))
                                        }
                                    }
                                    spannable.setSpan(clickableSpan, start, end, flags)
                                    spannable.removeSpan(urlSpan)
                                }
                            }
                            textView.text = spannable
                            textView.setTextColor(textColor.toArgb())
                            textView.setLinkTextColor(linkColor.toArgb())
                        },
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    )
                } else if (currentPage.type == "verses") {
                    val verses = currentPage.verses ?: emptyList()
                    val passage = currentPage.passage ?: return@AlertDialog
                    val processor = remember(verses) { VerseTextProcessor() }
                    val isKjvPlus = databaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false
                    val isOldTestament = passage.bookNumber <= 39
                    val processedVerses = remember(verses, themeColors, isKjvPlus) {
                        val result = mutableMapOf<Int, ProcessedVerse>()
                        for (verse in verses) {
                            val processed = processor.processVerse(
                                verseText = verse.text,
                                baseFontSize = (viewModel.fontSize * 0.85f).sp,
                                themeColors = themeColors,
                                textColor = themeColors.textColor,
                                onTagPress = { marker -> onTagPress(marker, passage) },
                                onWordPress = onWordPress,
                                onStrongsPress = { strong -> onStrongsPress(strong, passage.bookNumber) },
                                isHighlighted = false,
                                isKjvPlus = isKjvPlus,
                                isOldTestament = isOldTestament
                            )
                            result[verse.verseNumber] = processed
                        }
                        result
                    }
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    ) {
                        verses.forEach { verse ->
                            val processedVerse = processedVerses[verse.verseNumber]
                            if (processedVerse != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    processedVerse.header?.let { header ->
                                        if (header.text.isNotEmpty()) {
                                            Text(
                                                text = header,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = themeColors.tagColor,
                                                modifier = Modifier.padding(bottom = 4.dp),
                                                fontFamily = currentFontFamily
                                            )
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        val annotatedString = buildAnnotatedString {
                                            withStyle(
                                                style = SpanStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    color = themeColors.verseNumber,
                                                    fontSize = (viewModel.fontSize * 0.85f * 0.778f).sp
                                                )
                                            ) {
                                                append("${verse.verseNumber} ")
                                            }
                                            append(processedVerse.body)
                                        }
                                        var textLayoutResult: androidx.compose.ui.text.TextLayoutResult? by remember { mutableStateOf(null) }
                                        Text(
                                            text = annotatedString,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerInput(Unit) {
                                                    detectTapGestures { offset: Offset ->
                                                        textLayoutResult?.let { layout ->
                                                            val position = layout.getOffsetForPosition(offset)
                                                            val annotations = annotatedString.getStringAnnotations(start = position, end = position)
                                                            annotations.forEach { annotation ->
                                                                when (annotation.tag) {
                                                                    "word" -> onWordPress(annotation.item)
                                                                    "strong" -> onStrongsPress(annotation.item, passage.bookNumber)
                                                                    "tag" -> onTagPress(annotation.item, passage)
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                            fontSize = (viewModel.fontSize * 0.85f).sp,
                                            lineHeight = (viewModel.fontSize * 0.85f * 1.333f).sp,
                                            fontFamily = currentFontFamily,
                                            onTextLayout = { textLayoutResult = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    if (stack.size > 1) {
                        TextButton(onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            stack.removeLast()
                        }
                        }) {
                            Text("Back")
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            },
            dismissButton = if (currentPage.type == "definition") {
                {
                    TextButton(onClick = onSwitch) {
                        Text("Switch to $other")
                    }
                }
            } else null
        )

        DefinitionModal(
            show = showNestedWordModal,
            onDismiss = { showNestedWordModal = false },
            word = currentNestedWord,
            definition = nestedWordDefinition,
            selectedDictionary = viewModel.selectedDictionary,
            onSwitch = {
                viewModel.selectedDictionary = if (viewModel.selectedDictionary == "noah") "atsbd" else "noah"
                nestedWordDefinition = dictionaryDbHelper?.getWordDefinition(currentNestedWord) ?: "Definition not found."
            },
            databaseHelper = databaseHelper
        )

        StrongsModal(
            show = showStrongsModal,
            onDismiss = { showStrongsModal = false },
            strongNumber = currentStrongNumber,
            definition = strongDefinition
        )
    }
}
