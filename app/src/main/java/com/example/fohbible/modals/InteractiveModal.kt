@file:Suppress("AssignedValueIsNeverRead")

package com.example.fohbible.modals

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.luminance
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
import com.example.fohbible.data.BibleData
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.data.Verse
import com.example.fohbible.utils.ProcessedVerse
import com.example.fohbible.utils.ThemeColors
import com.example.fohbible.utils.VerseTextProcessor
import java.util.Locale

data class ModalPage(
    val title: String,
    val type: String,
    val content: String? = null,
    val verses: List<Verse>? = null,
    val passage: PassageSelection? = null,
    val word: String? = null,
    val strongNumber: String? = null,
    val description: String? = null
)

// Helper function to sanitize HTML content
fun sanitizeHtmlContent(content: String?): String {
    if (content.isNullOrEmpty()) return ""
    var sanitized = content

    val ppEndIndex = sanitized.indexOf("</pp>")
    if (ppEndIndex != -1) {
        sanitized = sanitized.take(ppEndIndex + "</pp>".length)
    }

    // 2. Remove all JavaScript code (script tags and inline event handlers)
    sanitized = sanitized.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")

    // 3. Remove all on* event handlers
    sanitized = sanitized.replace(Regex("\\s+on\\w+\\s*=\\s*\"[^\"]*\""), "")
    sanitized = sanitized.replace(Regex("\\s+on\\w+\\s*=\\s*'[^']*'"), "")
    sanitized = sanitized.replace(Regex("\\s+on\\w+\\s*=\\s*[^\\s>]+"), "")

    // 4. Remove javascript: links
    sanitized = sanitized.replace(Regex("javascript:[^\"'>]+"), "#")

    // 5. Remove any remaining HTML comments that might contain JS
    sanitized = sanitized.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    // 6. Remove any style tags that might contain JS
    sanitized = sanitized.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")

    // 7. Remove any meta refresh tags
    sanitized = sanitized.replace(Regex("<meta[^>]*http-equiv\\s*=\\s*['\"]?refresh['\"]?[^>]*>", RegexOption.IGNORE_CASE), "")

    return sanitized.trim()
}

fun parseVerseLink(href: String, linkText: String): PassageSelection? {
    try {
        // Parse href: B:220 38:4 or B:220 38:4-7
        val parts = href.substringAfter("B:").trim().split(" ")
        if (parts.size != 2) return null

        val bookNumber = parts[0].toInt()
        val chapterVersePart = parts[1]

        // Handle verse ranges like 38:4 or 38:4-7 from href
        val chapterVerseSplit = chapterVersePart.split(":")
        if (chapterVerseSplit.size != 2) return null

        val chapter = chapterVerseSplit[0].toInt()
        val versePart = chapterVerseSplit[1]
        val verseStart = versePart.substringBefore("-").toInt()
        var verseEnd: Int? = if (versePart.contains("-")) versePart.substringAfter("-").toInt() else null

        // If no range in href, check linkText for range (e.g., "Ex. 4:5-7")
        if (verseEnd == null) {
            val textParts = linkText.split(":")
            if (textParts.size >= 2) {
                val textVersePart = textParts.last().trim()
                    .replace("–", "-")
                    .replace("—", "-")
                if (textVersePart.contains("-")) {
                    val rangeParts = textVersePart.split("-")
                    if (rangeParts.size == 2) {
                        val startFromText = rangeParts[0].trim().toIntOrNull()
                        val endFromText = rangeParts[1].trim().toIntOrNull()
                        if (startFromText == verseStart && endFromText != null) {
                            verseEnd = endFromText
                        }
                    }
                }
            }
        }

        // Get the book name from BibleData using the book number
        val book = BibleData.getBookByCustomNumber(bookNumber)
        return PassageSelection(
            bookNumber = bookNumber,
            bookName = book?.name ?: "",
            chapter = chapter,
            verse = verseStart,
            verseEnd = verseEnd
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun fetchVerses(passage: PassageSelection, db: DatabaseHelper?): List<Verse> {
    if (db == null) return emptyList()
    val verses = db.getVerses(passage.bookNumber, passage.chapter)
    val start = passage.verse
    val end = passage.verseEnd ?: start
    val selectedVerses = verses.filter { it.verseNumber in start..end }
    return selectedVerses
}

@Composable
fun InteractiveModal(
    show: Boolean,
    onDismiss: () -> Unit,
    databaseHelper: DatabaseHelper?,
    initialType: String,
    initialTitle: String = "",
    initialContent: String = "",
    word: String = "",
    definition: String = "",
    strongNumber: String = "",
    strongDefinition: String = "",
    initialDescription: String = ""
) {
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
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

    val stack = remember { mutableStateListOf<ModalPage>() }
    val dictionaries = listOf("noah", "cbtel", "isbe", "atsbd")
    val dictionaryDisplayNames = mapOf(
        "noah" to "Noah Webster's Dictionary",
        "cbtel" to "Cyclopedia of Biblical, Theological and Ecclesiastical Literature",
        "isbe" to "International Standard Bible Encyclopedia",
        "atsbd" to "American Tract Society Bible Dictionary"
    )

    LaunchedEffect(show) {
        if (show) {
            stack.clear()
            when (initialType) {
                "commentary" -> {
                    val sanitizedContent = sanitizeHtmlContent(initialContent)
                    stack.add(ModalPage(initialTitle, "commentary", sanitizedContent, description = initialDescription))
                }
                "definition" -> {
                    val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
                    val capitalizedWord = word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    val title = "Definition of $capitalizedWord"
                    val sanitizedDefinition = sanitizeHtmlContent(definition)
                    stack.add(ModalPage(title, "definition", sanitizedDefinition, word = word, description = dbDisplayName))
                }
                "strong" -> {
                    val title = "Strong's Definition for $strongNumber"
                    val sanitizedDefinition = sanitizeHtmlContent(strongDefinition)
                    stack.add(ModalPage(title, "strong", sanitizedDefinition, strongNumber = strongNumber, description = initialDescription))
                }
            }
        }
    }

    val onWordPress: (String) -> Unit = Unit@{ w ->
        val trimmed = w.trim()
        if (trimmed.isEmpty() || trimmed.matches(Regex(".*\\d.*"))) {
            // Skip if empty or contains numbers (not a typical word)
            return@Unit
        }
        val def = dictionaryDbHelper?.getWordDefinition(trimmed) ?: "Definition not found."
        val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
        val capitalized = trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val title = "Definition of $capitalized"
        val sanitized = sanitizeHtmlContent(def)
        stack.add(ModalPage(title, "definition", sanitized, word = trimmed, description = dbDisplayName))
    }

    val onStrongsPress: (String, Int) -> Unit = Unit@{ strongNumber, _ ->
        val trimmed = strongNumber.trim()
        if (trimmed.isEmpty()) return@Unit
        val isOldTestament = viewModel.isOldTestament
        val prefixed = if (trimmed.firstOrNull()?.isLetter() ?: false) {
            trimmed.uppercase()
        } else {
            (if (isOldTestament) "H" else "G") + trimmed
        }
        if (!prefixed.matches(Regex("^[HG]\\d+$"))) {
            // Skip if not a valid Strong's format
            return@Unit
        }
        val definition = strongDbHelper?.getStrongDefinition(prefixed) ?: "Strong's definition not found."
        val title = "Strong's Definition for $prefixed"
        val sanitized = sanitizeHtmlContent(definition)
        stack.add(ModalPage(title, "strong", sanitized, strongNumber = prefixed))
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
        val sanitizedCombined = sanitizeHtmlContent(combined)
        stack.add(ModalPage(newTitle, "commentary", sanitizedCombined))
    }

    if (show) {
        if (stack.isEmpty()) return
        val currentPage = stack.last()
        val textColor = MaterialTheme.colorScheme.onBackground
        val linkColor = MaterialTheme.colorScheme.primary

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Column {
                    Text(
                        text = currentPage.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    currentPage.description?.let { description ->
                        if (description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            },
            text = {
                if (currentPage.type == "verses") {
                    val verses = currentPage.verses ?: emptyList()
                    val passage = currentPage.passage ?: return@AlertDialog
                    val processor = remember(verses) { VerseTextProcessor() }
                    val isKjvPlus = databaseHelper?.databaseName?.contains("kjv+", ignoreCase = true) ?: false
                    val isOldTestament = viewModel.isOldTestament
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
                } else {
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
                                textDirection = View.TEXT_DIRECTION_LTR
                                gravity = Gravity.START
                            }
                        },
                        update = { textView ->
                            val content = currentPage.content ?: ""
                            val spanned = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT)
                            val spannable = SpannableString(spanned)
                            val urlSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
                            for (urlSpan in urlSpans) {
                                val start = spannable.getSpanStart(urlSpan)
                                val end = spannable.getSpanEnd(urlSpan)
                                val flags = spannable.getSpanFlags(urlSpan)
                                val href = urlSpan.url
                                val linkText = spannable.substring(start, end)
                                var handled = false

                                if (href.startsWith("B:")) {
                                    val passage = parseVerseLink(href, linkText)
                                    if (passage != null) {
                                        val clickableSpan = object : ClickableSpan() {
                                            override fun onClick(widget: View) {
                                                val verses = fetchVerses(passage, databaseHelper)
                                                val newTitle = "${passage.bookName} ${passage.chapter}:${passage.verse}" + if (passage.verseEnd != null) "-${passage.verseEnd}" else ""
                                                stack.add(ModalPage(newTitle, "verses", verses = verses, passage = passage))
                                            }
                                        }
                                        spannable.setSpan(clickableSpan, start, end, flags)
                                        handled = true
                                    }
                                } else if (href.startsWith("S:")) {
                                    val seeContent = href.substringAfter("S:").trim()
                                    val cleanedLinkText = linkText.replace(Regex("^See\\s+", RegexOption.IGNORE_CASE), "").trim()
                                    val clickableSpan = when {
                                        seeContent.startsWith("B:") -> {
                                            // Handle verse reference under "See"
                                            val verseHref = "B:" + seeContent.substringAfter("B:")
                                            val passage = parseVerseLink(verseHref, linkText)
                                            if (passage != null) {
                                                object : ClickableSpan() {
                                                    override fun onClick(widget: View) {
                                                        val verses = fetchVerses(passage, databaseHelper)
                                                        val newTitle = "${passage.bookName} ${passage.chapter}:${passage.verse}" + if (passage.verseEnd != null) "-${passage.verseEnd}" else ""
                                                        stack.add(ModalPage(newTitle, "verses", verses = verses, passage = passage))
                                                    }
                                                }
                                            } else null
                                        }
                                        seeContent.matches(Regex("^[GH]\\d+$")) -> {
                                            // Handle prefixed Strong's number
                                            object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    val definition = strongDbHelper?.getStrongDefinition(seeContent) ?: "Strong's definition not found."
                                                    val newTitle = "Strong's Definition for $seeContent"
                                                    val sanitized = sanitizeHtmlContent(definition)
                                                    stack.add(ModalPage(newTitle, "strong", sanitized, strongNumber = seeContent))
                                                }
                                            }
                                        }
                                        seeContent.matches(Regex("^\\d+$")) -> {
                                            // Handle unprefixed Strong's number (try H and G)
                                            object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    val hNum = "H$seeContent"
                                                    val gNum = "G$seeContent"
                                                    val hDef = strongDbHelper?.getStrongDefinition(hNum) ?: ""
                                                    val gDef = strongDbHelper?.getStrongDefinition(gNum) ?: ""
                                                    var combinedDef = ""
                                                    var combinedTitle = "Strong's Definition"
                                                    var combinedStrongNum = ""
                                                    if (hDef.isNotBlank() && gDef.isNotBlank()) {
                                                        combinedTitle += " for $hNum and $gNum"
                                                        combinedDef = "$hNum:\n$hDef\n\n$gNum:\n$gDef"
                                                        combinedStrongNum = "$hNum,$gNum"
                                                    } else if (hDef.isNotBlank()) {
                                                        combinedTitle += " for $hNum"
                                                        combinedDef = hDef
                                                        combinedStrongNum = hNum
                                                    } else if (gDef.isNotBlank()) {
                                                        combinedTitle += " for $gNum"
                                                        combinedDef = gDef
                                                        combinedStrongNum = gNum
                                                    } else {
                                                        // Fallback to word definition
                                                        val wordFallback = cleanedLinkText
                                                        val definition = dictionaryDbHelper?.getWordDefinition(wordFallback) ?: "Definition not found."
                                                        val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
                                                        val capitalized = wordFallback.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                                        val newTitle = "Definition of $capitalized"
                                                        val sanitizedDef = sanitizeHtmlContent(definition)
                                                        stack.add(ModalPage(newTitle, "definition", sanitizedDef, word = wordFallback, description = dbDisplayName))
                                                        return
                                                    }
                                                    val sanitized = sanitizeHtmlContent(combinedDef)
                                                    stack.add(ModalPage(combinedTitle, "strong", sanitized, strongNumber = combinedStrongNum))
                                                }
                                            }
                                        }
                                        else -> {
                                            // Handle word definition (e.g., S:God for word "God")
                                            object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    val wordToFetch = cleanedLinkText.ifEmpty { seeContent }
                                                    val definition = dictionaryDbHelper?.getWordDefinition(wordToFetch) ?: "Definition not found."
                                                    val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
                                                    val capitalized = wordToFetch.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                                    val newTitle = "Definition of $capitalized"
                                                    val sanitizedDef = sanitizeHtmlContent(definition)
                                                    stack.add(ModalPage(newTitle, "definition", sanitizedDef, word = wordToFetch, description = dbDisplayName))
                                                }
                                            }
                                        }
                                    }
                                    if (clickableSpan != null) {
                                        spannable.setSpan(clickableSpan, start, end, flags)
                                        handled = true
                                    }
                                }

                                if (handled) {
                                    spannable.removeSpan(urlSpan)
                                }
                            }

                            // Handle dark mode text colors by removing all custom colors and forcing white
                            if (isDark) {
                                val colorSpans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
                                for (span in colorSpans) {
                                    spannable.removeSpan(span)
                                }
                                textView.setTextColor(Color.WHITE)
                            } else {
                                textView.setTextColor(textColor.toArgb())
                            }
                            textView.setLinkTextColor(linkColor.toArgb())
                            textView.text = spannable
                        },
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    if (stack.size > 1) {
                        TextButton(onClick = {
                            stack.removeAt(stack.lastIndex)
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
                    val currentIndex = dictionaries.indexOf(viewModel.selectedDictionary)
                    val nextIndex = (currentIndex + 1) % dictionaries.size
                    val nextDictionary = dictionaries[nextIndex]
                    val nextDisplayName = dictionaryDisplayNames[nextDictionary] ?: nextDictionary

                    TextButton(onClick = {
                        val currentWord = currentPage.word ?: return@TextButton
                        val tempDbHelper = DatabaseHelper(context, "${nextDictionary}.dictionary.sqlite3")
                        val newDef = tempDbHelper.getWordDefinition(currentWord) ?: "Definition not found."
                        val capitalized = currentWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                        val newTitle = "Definition of $capitalized"
                        val sanitizedNewDef = sanitizeHtmlContent(newDef)
                        stack[stack.lastIndex] = currentPage.copy(
                            title = newTitle,
                            content = sanitizedNewDef,
                            description = nextDisplayName
                        )
                        viewModel.selectedDictionary = nextDictionary
                        tempDbHelper.close()
                    }) {
                        Text("Switch to ${nextDictionary.uppercase()}")
                    }
                }
            } else null
        )
    }
}