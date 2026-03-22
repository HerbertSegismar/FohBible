package com.fountofhopedotorg.fohbible.modals

import android.content.res.Configuration
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ModalPage
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.ProcessedVerse
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.VerseCommentary
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.Fonts
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

fun sanitizeHtmlContent(content: String?): String {
    if (content.isNullOrEmpty()) return ""
    var sanitized = content
    val ppEndIndex = sanitized.indexOf("</pp>")
    if (ppEndIndex != -1) {
        sanitized = sanitized.take(ppEndIndex + "</pp>".length)
    }
    sanitized = sanitized.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.DOT_MATCHES_ALL), "")
    sanitized = sanitized.replace(Regex("\\s+on\\w+\\s*=\\s*\"[^\"]*\""), "")
    sanitized = sanitized.replace(Regex("\\s+on\\w+\\s*=\\s*'[^']*'"), "")
    sanitized = sanitized.replace(Regex("\\s+on\\w+\\s*=[^\\s>]+"), "")
    sanitized = sanitized.replace(Regex("javascript:[^\"'>]+"), "#")
    sanitized = sanitized.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
    sanitized = sanitized.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
    sanitized = sanitized.replace(Regex("<meta[^>]*http-equiv\\s*=\\s*['\"]?refresh['\"]?[^>]*>", RegexOption.IGNORE_CASE), "")
    return sanitized.trim()
}

fun prepareStrongContent(rawDefinition: String): String {
    val sanitized = sanitizeHtmlContent(rawDefinition)
    val searchTerm = "Derivation"
    val index = sanitized.indexOf(searchTerm, ignoreCase = true)
    if (index != -1) {
        return sanitized.take(index) + " " + sanitized.substring(index)
    }
    return sanitized
}

fun cleanDefinition(topic: String, rawDef: String): String {
    val sanitized = sanitizeHtmlContent(rawDef)
    val upperTopic = topic.uppercase(Locale.ROOT)
    val capTopic = topic.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    val possibleStarts = listOf(upperTopic, capTopic)
    var cleaned = sanitized
    for (start in possibleStarts) {
        if (cleaned.startsWith(start, ignoreCase = true)) {
            cleaned = cleaned.substring(start.length).replace(Regex("^[,.:;-]+"), "").trim()
            break
        }
    }
    if (cleaned.startsWith("<")) {
        val tagPattern = Regex("^<(\\w+)>(\\s*([\\w ]+)\\s*)</\\1>", RegexOption.IGNORE_CASE)
        val match = tagPattern.find(cleaned)
        if (match != null) {
            val content = match.groups[2]?.value?.trim() ?: ""
            if (possibleStarts.any { content.equals(it, ignoreCase = true) }) {
                cleaned = cleaned.substring(match.value.length).replace(Regex("^[,.:;-]+"), "").trim()
            }
        }
    }
    return cleaned
}

fun formatDefinitionContent(word: String, content: String, isOxford: Boolean): String {
    return if (isOxford) {
        "<b>${word.uppercase(Locale.ROOT)}</b><br>$content"
    } else {
        content
    }
}

fun buildDefinitionContent(
    originalWord: String,
    pairs: List<Pair<String, String>>,
    isOxford: Boolean,
    isTopical: Boolean
): String {
    if (pairs.isEmpty()) return "No definition found."
    val isExact = pairs.size == 1 && pairs[0].first.equals(originalWord, ignoreCase = true)
    return if (isOxford && !isTopical) {
        if (pairs.size == 1) {
            val topic = pairs[0].first
            val rawContent = if (isExact) sanitizeHtmlContent(pairs[0].second) else cleanDefinition(topic, pairs[0].second)
            "<b>${topic.uppercase(Locale.ROOT)}</b><br>$rawContent"
        } else {
            pairs.joinToString("<br><hr><br>") { (topic, def) ->
                val cleanedDef = sanitizeHtmlContent(def)
                "<b>${topic.uppercase(Locale.ROOT)}</b><br>$cleanedDef"
            }
        }
    } else {
        val rawContent = if (isExact) {
            sanitizeHtmlContent(pairs[0].second)
        } else if (pairs.size == 1) {
            cleanDefinition(pairs[0].first, pairs[0].second)
        } else {
            pairs.joinToString("<br><hr><br>") { p -> sanitizeHtmlContent(p.second) }
        }
        if (isTopical) rawContent else formatDefinitionContent(originalWord, rawContent, false)
    }
}

fun parseVerseLink(href: String, linkText: String): PassageSelection? {
    try {
        val parts = href.substringAfter("B:").trim().split(" ")
        if (parts.size != 2) return null
        val bookNumber = parts[0].toInt()
        val chapterVersePart = parts[1]
        val chapterVerseSplit = chapterVersePart.split(":")
        if (chapterVerseSplit.size != 2) return null
        val chapter = chapterVerseSplit[0].toInt()
        val versePart = chapterVerseSplit[1]
        val verseStart = versePart.substringBefore("-").toInt()
        var verseEnd: Int? = if (versePart.contains("-")) versePart.substringAfter("-").toInt() else null
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
        val book = BibleData.getBookByCustomNumber(bookNumber)
        return PassageSelection(
            bookNumber = bookNumber,
            bookName = book?.name ?: "",
            chapter = chapter,
            verse = verseStart,
            verseEnd = verseEnd
        )
    } catch (_: Exception) {
        return null
    }
}

fun fetchVerses(passage: PassageSelection, db: DatabaseHelper?): List<Verse> {
    if (db == null) return emptyList()
    val verses = mutableListOf<Verse>()
    val startChapter = passage.chapter
    val endChapter = passage.chapterEnd ?: passage.chapter
    val maxChapters = BibleData.getBookByCustomNumber(passage.bookNumber)?.chapters ?: endChapter
    val cappedEndChapter = minOf(endChapter, maxChapters)
    val startVerse = passage.verse ?: 1
    for (ch in startChapter..cappedEndChapter) {
        val chapterStartVerse = if (ch == startChapter) startVerse else 1
        val chapterEndVerse = if (ch == cappedEndChapter) {
            passage.verseEnd ?: if (passage.chapterEnd == null) chapterStartVerse else db.getVerseCount(passage.bookNumber, ch)
        } else {
            db.getVerseCount(passage.bookNumber, ch)
        }
        val chVerses = db.getVerses(passage.bookNumber, ch).filter { it.verseNumber in chapterStartVerse..chapterEndVerse }
        val updatedVerses = chVerses.map { Verse(it.verseNumber, it.text, passage.bookName, ch) }
        verses.addAll(updatedVerses)
    }
    return verses
}

fun levenshteinDistance(s1: String, s2: String): Int {
    val len1 = s1.length
    val len2 = s2.length
    val cost = Array(len1 + 1) { IntArray(len2 + 1) }
    for (i in 0..len1) cost[i][0] = i
    for (j in 0..len2) cost[0][j] = j
    for (i in 1..len1) {
        for (j in 1..len2) {
            val match = if (s1[i - 1] == s2[j - 1]) 0 else 1
            cost[i][j] = minOf(
                cost[i - 1][j] + 1,
                cost[i][j - 1] + 1,
                cost[i - 1][j - 1] + match
            )
        }
    }
    return cost[len1][len2]
}

fun customDistance(word: String, topic: String): Int {
    if (topic.equals(word, ignoreCase = true)) return 0
    val w = word.lowercase(Locale.ROOT)
    val t = topic.lowercase(Locale.ROOT)
    val words = t.split(Regex("\\s+"))
    if (words.contains(w)) {
        return 10 + words.size
    }
    val commonPrefixLen = w.commonPrefixWith(t).length
    if (commonPrefixLen >= w.length - 1) {
        return 12 + (t.length - w.length).coerceAtLeast(0)
    }
    val prefixLen = if (t.startsWith(w)) t.length - w.length else Int.MAX_VALUE / 2
    val suffixLen = if (t.endsWith(w)) t.length - w.length else Int.MAX_VALUE / 2
    val minAffix = minOf(prefixLen, suffixLen)
    if (minAffix < Int.MAX_VALUE / 2) {
        return 20 + minAffix
    }
    if (t.contains(w)) {
        return 30 + (t.length - w.length)
    }
    return 100 + levenshteinDistance(w, t)
}

suspend fun getDefinitionOrClosest(dbHelper: DatabaseHelper?, originalWord: String): List<Pair<String, String>>? {
    return withContext(Dispatchers.IO) {
        if (dbHelper == null) return@withContext null
        val db = dbHelper.database ?: return@withContext null
        val lowerWord = originalWord.trim().lowercase(Locale.ROOT)
        val likeParam = "%$lowerWord%"
        var cursor = db.query("dictionary", arrayOf("topic", "definition"), "LOWER(topic) LIKE ?", arrayOf(likeParam), null, null, "LENGTH(topic) ASC LIMIT 50")
        val candidates = mutableListOf<Pair<String, String>>()
        while (cursor.moveToNext()) {
            val topic = cursor.getString(0)
            val def = cursor.getString(1)
            candidates.add(Pair(topic, def))
        }
        cursor.close()
        if (candidates.isNotEmpty()) {
            val sorted = candidates.sortedBy { customDistance(lowerWord, it.first.lowercase(Locale.ROOT)) * 1000 + levenshteinDistance(lowerWord, it.first.lowercase(Locale.ROOT)) }
            return@withContext sorted.take(5)
        }
        val result = mutableListOf<Pair<String, String>>()
        cursor = db.query("dictionary", arrayOf("topic", "definition"), "LOWER(topic) > ?", arrayOf(lowerWord), null, null, "LOWER(topic) ASC", "1")
        if (cursor.moveToFirst()) {
            val topic = cursor.getString(0)
            val def = cursor.getString(1)
            result.add(Pair(topic, def))
        }
        cursor.close()
        cursor = db.query("dictionary", arrayOf("topic", "definition"), "LOWER(topic) < ?", arrayOf(lowerWord), null, null, "LOWER(topic) DESC", "1")
        if (cursor.moveToFirst()) {
            val topic = cursor.getString(0)
            val def = cursor.getString(1)
            result.add(Pair(topic, def))
        }
        cursor.close()
        if (result.isEmpty()) return@withContext null
        result.sortedBy { levenshteinDistance(lowerWord, it.first.lowercase(Locale.ROOT)) }
    }
}

suspend fun getVerseCommentaries(
    dbHelper: DatabaseHelper?,
    bookNumber: Int,
    chapter: Int,
    verse: Int
): List<VerseCommentary>? = withContext(Dispatchers.IO) {
    if (dbHelper == null) return@withContext null
    try {
        dbHelper.getCommentariesForVerse(bookNumber, chapter, verse)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun InteractiveModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onNavigateToReader: (PassageSelection) -> Unit,
    databaseHelper: DatabaseHelper?,
    initialType: String,
    initialTitle: String = "",
    initialContent: String = "",
    word: String = "",
    definition: String = "",
    strongNumber: String = "",
    strongDefinition: String = "",
    initialDescription: String = "",
    isOldTestament: Boolean,
    bookNumber: Int? = null,
    chapter: Int? = null,
    verse: Int? = null
) {
    val viewModel = viewModel<AppViewModel>()
    val context = LocalContext.current
    val themeColors = ThemeColors(
        textColor = MaterialTheme.colorScheme.onBackground,
        verseNumber = MaterialTheme.colorScheme.primary,
        primary = MaterialTheme.colorScheme.primary,
        tagColor = MaterialTheme.colorScheme.secondary,
        tagBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        wordsOfJesus = Color(0xFFDA4227),
        searchHighlightBg = if (viewModel.darkTheme) Color(0xFF81D4FA).copy(alpha = 0.3f) else Color.Yellow.copy(alpha = 0.3f),
        highlightIcon = MaterialTheme.colorScheme.primary
    )
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val currentFontFamily = getFontFamily(viewModel.selectedFontFamily)

    var dictionaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var strongDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var commentaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var verseCommentaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    var crossRefDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }

    val dictionaries = listOf("atsbd", "noah", "cbtel", "isbe", "oxford", "topical")
    val dictionaryDisplayNames = mapOf(
        "atsbd" to "ATSBD",
        "noah" to "Noah Webster's Dictionary",
        "cbtel" to "CBTEL",
        "isbe" to "Int'l Standard Bible Encyclopedia",
        "oxford" to "Oxford Dictionary",
        "topical" to "Topical Bible Dictionary"
    )
    val verseCommentaries = listOf("cbsc", "ebc", "fairbairn", "hawker", "mhwbc", "spurgeon", "scofield")
    val verseCommentaryDisplayNames = mapOf(
        "cbsc" to "Cambridge Bible Commentary",
        "ebc" to "Expositor's Bible Commentary",
        "fairbairn" to "Fairbairn's Typology of Scripture",
        "hawker" to "Hawker's Poor Man's Commentary",
        "mhwbc" to "Matthew Henry's Commentary",
        "spurgeon" to "Charles Spurgeon's Commentary",
        "scofield" to "Scofield Reference Bible"
    )
    val crossReferenceDatabases = listOf("esv", "niv11", "obx")
    val crossReferenceDatabaseDisplayNames = mapOf(
        "esv" to "English Standard Version",
        "niv11" to "New International Version",
        "obx" to "OpenBible Project"
    )

    DisposableEffect(Unit) {
        onDispose {
            crossRefDbHelper?.close()
            dictionaryDbHelper?.close()
            strongDbHelper?.close()
            commentaryDbHelper?.close()
            verseCommentaryDbHelper?.close()
        }
    }

    LaunchedEffect(show, viewModel.selectedDictionary, viewModel.selectedVerseCommentary, viewModel.selectedCrossReferenceDatabase, databaseHelper?.databaseName) {
        dictionaryDbHelper?.close()
        dictionaryDbHelper = DatabaseHelper(context, "${viewModel.selectedDictionary}.dictionary.sqlite3")
        strongDbHelper?.close()
        strongDbHelper = DatabaseHelper(context, "secedictionary.sqlite3")
        commentaryDbHelper?.close()
        val name = databaseHelper?.databaseName ?: return@LaunchedEffect
        val comName = name.replace(".sqlite3", "com.sqlite3")
        commentaryDbHelper = if (comName.isNotEmpty()) DatabaseHelper(context, comName) else null
        verseCommentaryDbHelper?.close()
        verseCommentaryDbHelper = DatabaseHelper(context, "${viewModel.selectedVerseCommentary}.commentaries.sqlite3")
        crossRefDbHelper?.close()
        crossRefDbHelper = DatabaseHelper(context, "${viewModel.selectedCrossReferenceDatabase}.crossreferences.sqlite3")
    }

    val stack = remember { mutableStateListOf<ModalPage>() }
    val scrollStates = remember { mutableStateMapOf<ModalPage, ScrollState>() }

    LaunchedEffect(show) {
        if (show) {
            stack.clear()
            when (initialType) {
                "commentary" -> {
                    val sanitizedContent = sanitizeHtmlContent(initialContent)
                    stack.add(ModalPage(initialTitle, "commentary", sanitizedContent, description = initialDescription, isOldTestament = isOldTestament))
                }
                "strong" -> {
                    val title = "Strong's Definition for $strongNumber"
                    val preparedDefinition = prepareStrongContent(strongDefinition)
                    stack.add(ModalPage(title, "strong", preparedDefinition, strongNumber = strongNumber, description = initialDescription, isOldTestament = isOldTestament))
                }
                "definition" -> {
                    val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
                    val capitalizedWord = word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    if (definition.isNotBlank() && !definition.contains("not found", ignoreCase = true)) {
                        val title = "Definition of $capitalizedWord"
                        val sanitizedDefinition = sanitizeHtmlContent(definition)
                        stack.add(ModalPage(title, "definition", sanitizedDefinition, word = word, description = dbDisplayName, isOldTestament = isOldTestament))
                    } else {
                        val loadingPage = ModalPage("Searching for $capitalizedWord...", "definition", "Loading...", word = word, description = dbDisplayName, isOldTestament = isOldTestament)
                        stack.add(loadingPage)
                        val pairs = getDefinitionOrClosest(dictionaryDbHelper, word) ?: emptyList()
                        if (pairs.isNotEmpty()) {
                            val isExact = pairs.size == 1 && pairs[0].first.equals(word, ignoreCase = true)
                            val isTopical = viewModel.selectedDictionary == "topical"
                            val newTitle = if (isTopical) {
                                "References for $capitalizedWord"
                            } else if (isExact) {
                                "Definition of ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                            } else if (pairs.size == 1) {
                                "Closest match for $capitalizedWord"
                            } else {
                                "Matches for $capitalizedWord"
                            }
                            val newContent = buildDefinitionContent(
                                originalWord = word,
                                pairs = pairs,
                                isOxford = viewModel.selectedDictionary == "oxford",
                                isTopical = viewModel.selectedDictionary == "topical"
                            )
                            stack[0] = loadingPage.copy(title = newTitle, content = newContent)
                        } else {
                            stack[0] = loadingPage.copy(title = "Definition not found", content = "No results for \"$word\".")
                        }
                    }
                }
                "versecommentary" -> {
                    val bookNum = bookNumber ?: 1
                    val chap = chapter ?: 1
                    val vers = verse ?: 1
                    val displayName = verseCommentaryDisplayNames[viewModel.selectedVerseCommentary] ?: viewModel.selectedVerseCommentary
                    val bookName = BibleData.getBookByCustomNumber(bookNum)?.name ?: "Book"
                    val title = "Notes on $bookName $chap:$vers"
                    val loadingPage = ModalPage(
                        title = title,
                        type = "versecommentary",
                        content = "Loading...",
                        description = displayName,
                        isOldTestament = isOldTestament,
                        bookNumber = bookNum,
                        chapter = chap,
                        verse = vers
                    )
                    stack.add(loadingPage)
                    val commentaries = if (verseCommentaryDbHelper != null) {
                        getVerseCommentaries(verseCommentaryDbHelper, bookNum, chap, vers)
                    } else {
                        null
                    }
                    val newContent = if (commentaries.isNullOrEmpty()) {
                        "No commentaries available for this verse."
                    } else {
                        commentaries.joinToString("<br><br>──────────<br><br>") { commentary ->
                            commentary.text
                        }
                    }
                    val index = stack.indexOf(loadingPage)
                    if (index != -1) {
                        stack[index] = loadingPage.copy(content = newContent)
                    }
                }
                "crossreference" -> {
                    val sanitized = sanitizeHtmlContent(initialContent)
                    stack.add(
                        ModalPage(
                            title = initialTitle,
                            type = "crossreference",
                            content = sanitized,
                            description = initialDescription.ifBlank { crossReferenceDatabaseDisplayNames[viewModel.selectedCrossReferenceDatabase] ?: viewModel.selectedCrossReferenceDatabase },
                            isOldTestament = isOldTestament,
                            bookNumber = bookNumber,
                            chapter = chapter,
                            verse = verse
                        )
                    )
                }
            }
        }
    }

    val scope = rememberCoroutineScope()

    val onWordPress: (String) -> Unit = Unit@{ w ->
        val trimmed = w.trim()
        if (trimmed.isEmpty() || trimmed.matches(Regex(".*\\d.*"))) {
            return@Unit
        }
        val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
        val capitalizedWord = trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val currentIsOld = stack.last().isOldTestament
        val loadingTitle = "Loading Definition of $capitalizedWord"
        val loadingPage = ModalPage(loadingTitle, "definition", "Loading...", word = trimmed, description = dbDisplayName, isOldTestament = currentIsOld)
        stack.add(loadingPage)
        scope.launch {
            val pairs: List<Pair<String, String>> = getDefinitionOrClosest(dictionaryDbHelper, trimmed) ?: emptyList()
            val newContent: String
            val newTitle: String
            if (pairs.isNotEmpty()) {
                val isExact = pairs.size == 1 && pairs[0].first.equals(trimmed, ignoreCase = true)
                val isTopical = viewModel.selectedDictionary == "topical"
                newTitle = if (isTopical) {
                    "References for $capitalizedWord"
                } else if (isExact) {
                    "Definition of ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                } else if (pairs.size == 1) {
                    "Closest match for ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                } else {
                    "Matches for \"$capitalizedWord\""
                }
                newContent = if (isExact) {
                    sanitizeHtmlContent(pairs[0].second)
                } else if (pairs.size == 1) {
                    cleanDefinition(pairs[0].first, pairs[0].second)
                } else {
                    pairs.joinToString("<br><hr><br>") { p -> sanitizeHtmlContent(p.second) }
                }
            } else {
                newTitle = "Definition of $capitalizedWord not found"
                newContent = "No definition found."
            }
            val index = stack.indexOf(loadingPage)
            if (index != -1) {
                stack[index] = loadingPage.copy(title = newTitle, content = newContent)
            }
        }
    }

    val onStrongsPress: (String, Int) -> Unit = Unit@{ strongNumber, _ ->
        val trimmed = strongNumber.trim()
        if (trimmed.isEmpty()) return@Unit
        val currentIsOld = stack.last().isOldTestament
        val prefixed = if (trimmed.firstOrNull()?.isLetter() ?: false) {
            trimmed.uppercase()
        } else {
            (if (currentIsOld) "H" else "G") + trimmed
        }
        if (!prefixed.matches(Regex("^[HG]\\d+"))) {
            return@Unit
        }
        val title = "Strong's Definition for $prefixed"
        val loadingPage = ModalPage(title, "strong", "Loading...", strongNumber = prefixed, isOldTestament = currentIsOld)
        stack.add(loadingPage)
        scope.launch {
            val definition = withContext(Dispatchers.IO) {
                strongDbHelper?.getStrongDefinition(prefixed) ?: "Strong's definition not found."
            }
            val prepared = prepareStrongContent(definition)
            val index = stack.indexOf(loadingPage)
            if (index != -1) {
                stack[index] = loadingPage.copy(content = prepared)
            }
        }
    }

    val onTagPress: (String, PassageSelection) -> Unit = Unit@{ marker, passage ->
        val bookNumber = passage.bookNumber
        val chapter = passage.chapter
        val bookName = passage.bookName
        val start = passage.verse ?: return@Unit
        val end = passage.verseEnd ?: start
        val rangeStr = if (end != start) "$start-$end" else "$start"
        val newTitle = "Notes on $bookName $chapter:$rangeStr$marker"
        val loadingPage = ModalPage(newTitle, "commentary", "Loading...", isOldTestament = stack.last().isOldTestament)
        stack.add(loadingPage)
        scope.launch {
            val commentaries = (start..end).mapNotNull { verseNum ->
                val text = withContext(Dispatchers.IO) {
                    commentaryDbHelper?.getCommentary(bookNumber, chapter, verseNum, marker)
                }
                if (text?.isNotBlank() == true) "Verse $verseNum: \n$text" else null
            }
            val combined = if (commentaries.isNotEmpty()) {
                commentaries.joinToString("\n\n────────────────────────\n\n")
            } else {
                "No commentary found for marker \"$marker\" in this passage."
            }
            val sanitizedCombined = sanitizeHtmlContent(combined)
            val index = stack.indexOf(loadingPage)
            if (index != -1) {
                val book = BibleData.getBookByCustomNumber(passage.bookNumber)
                val isOld = book?.testament == Testament.OLD
                stack[index] = loadingPage.copy(content = sanitizedCombined, isOldTestament = isOld)
            }
        }
    }

    val onCrossRefClick: (Int, Int, Int, Boolean) -> Unit = { book, chap, verseNum, isOld ->
        scope.launch {
            val refs = withContext(Dispatchers.IO) {
                crossRefDbHelper?.getCrossReferences(book, chap, verseNum) ?: emptyList()
            }
            val bookName = BibleData.getBookByCustomNumber(book)?.name ?: book.toString()
            val source = "References for $bookName $chap:$verseNum"
            val htmlItems = refs.joinToString("<br>") { ref ->
                val toBook = BibleData.getBookByCustomNumber(ref.bookTo)?.name ?: ref.bookTo.toString()
                val verseRange = if (ref.verseToStart == ref.verseToEnd) ref.verseToStart.toString() else "${ref.verseToStart}-${ref.verseToEnd}"
                val href = "B:${ref.bookTo} ${ref.chapterTo}:$verseRange"
                "<a href=\"$href\">$toBook ${ref.chapterTo}:$verseRange</a>"
            }
            val newPage = ModalPage(
                title = source,
                type = "crossreference",
                content = htmlItems,
                description = crossReferenceDatabaseDisplayNames[viewModel.selectedCrossReferenceDatabase] ?: viewModel.selectedCrossReferenceDatabase,
                isOldTestament = isOld,
                bookNumber = book,
                chapter = chap,
                verse = verseNum
            )
            stack.add(newPage)
        }
    }

    val onVerseCommentaryClick: (bookNumber: Int, chapter: Int, verseNumber: Int, isOldTestament: Boolean) -> Unit = { book, chap, verseNum, isOld ->
        val displayName = verseCommentaryDisplayNames[viewModel.selectedVerseCommentary] ?: viewModel.selectedVerseCommentary
        val bookName = BibleData.getBookByCustomNumber(book)?.name ?: "Book"
        val title = "Notes on $bookName $chap:$verseNum"
        val loadingPage = ModalPage(
            title = title,
            type = "versecommentary",
            content = "Loading...",
            description = displayName,
            isOldTestament = isOld,
            bookNumber = book,
            chapter = chap,
            verse = verseNum
        )
        stack.add(loadingPage)
        scope.launch {
            val commentaries = withContext(Dispatchers.IO) {
                verseCommentaryDbHelper?.getCommentariesForVerse(book, chap, verseNum)
            }
            val newContent = if (commentaries.isNullOrEmpty()) {
                "No commentaries available for this verse."
            } else {
                commentaries.joinToString("\n\n──────────\n\n") { it.text }
            }
            val index = stack.indexOf(loadingPage)
            if (index != -1) {
                stack[index] = loadingPage.copy(content = newContent)
            }
        }
    }

    val lightModalColor = if (viewModel.lightModalBackgroundColor != Color.Unspecified) {
        viewModel.lightModalBackgroundColor
    } else {
        MaterialTheme.colorScheme.surface
    }
    val darkModalColor = if (viewModel.darkModalBackgroundColor != Color.Unspecified) {
        viewModel.darkModalBackgroundColor
    } else {
        MaterialTheme.colorScheme.surface
    }
    val modalBackgroundColor = if (isDark) { darkModalColor } else { lightModalColor }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var crossRefDropdownExpanded by remember { mutableStateOf(false) }

    if (show) {
        if (stack.isEmpty()) return
        val currentPage = stack.last()
        val scrollState = scrollStates.getOrPut(currentPage) { ScrollState(0) }

        val textColor = MaterialTheme.colorScheme.onBackground
        val linkColor = MaterialTheme.colorScheme.primary

        var showModalColorWheel by remember { mutableStateOf(false) }
        var dictionaryDropdownExpanded by remember { mutableStateOf(false) }
        var commentaryDropdownExpanded by remember { mutableStateOf(false) }
        var showEditWordDialog by remember { mutableStateOf(false) }

        AlertDialog(
            modifier = if (isLandscape) Modifier.fillMaxWidth(0.9f) else Modifier,
            properties = if (isLandscape) DialogProperties(usePlatformDefaultWidth = false) else DialogProperties(),
            onDismissRequest = onDismiss,
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (currentPage.type == "definition" && currentPage.word != null) {
                            val capitalizedWord = currentPage.word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                            val isTopical = viewModel.selectedDictionary == "topical"
                            val prefix = when {
                                isTopical -> "References for "
                                currentPage.title.startsWith("Definition of ") -> "Definition of "
                                currentPage.title.startsWith("Closest match for ") -> "Closest match for "
                                currentPage.title.startsWith("Matches for ") -> "Matches for "
                                else -> "Definition of "
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = prefix,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = capitalizedWord,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { showEditWordDialog = true }
                                )
                            }
                        } else {
                            Text(text = currentPage.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (currentPage.type == "verses" && currentPage.passage != null) {
                            IconButton(onClick = {
                                onNavigateToReader(currentPage.passage.copy(verseEnd = null, chapterEnd = null))
                                onDismiss()
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Read in Reader", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(lightModalColor, darkModalColor)
                                    ),
                                    shape = CircleShape
                                )
                                .border(0.2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showModalColorWheel = true }
                        )
                    }
                    currentPage.description?.let { description ->
                        if (description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            when (currentPage.type) {
                                "definition" -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Box {
                                            IconButton(
                                                onClick = { dictionaryDropdownExpanded = true },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "Select Dictionary",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = dictionaryDropdownExpanded,
                                                onDismissRequest = { dictionaryDropdownExpanded = false }
                                            ) {
                                                dictionaries.forEach { dictKey ->
                                                    DropdownMenuItem(
                                                        text = { Text(dictionaryDisplayNames[dictKey] ?: dictKey) },
                                                        onClick = {
                                                            dictionaryDropdownExpanded = false
                                                            val previous = viewModel.selectedDictionary
                                                            if (previous != dictKey) {
                                                                viewModel.selectedDictionary = dictKey
                                                                val currentWord = currentPage.word ?: return@DropdownMenuItem
                                                                val loadingTitle = "Switching to ${dictKey.uppercase()}"
                                                                val loadingPage = currentPage.copy(
                                                                    title = loadingTitle,
                                                                    content = "Loading...",
                                                                    description = dictionaryDisplayNames[dictKey] ?: dictKey
                                                                )
                                                                val index = stack.lastIndex
                                                                stack[index] = loadingPage
                                                                scope.launch {
                                                                    val tempDbHelper = withContext(Dispatchers.IO) {
                                                                        DatabaseHelper(context, "${dictKey}.dictionary.sqlite3")
                                                                    }
                                                                    val pairs: List<Pair<String, String>> = getDefinitionOrClosest(tempDbHelper, currentWord) ?: emptyList()
                                                                    val capitalizedWord = currentWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                                                    val isExact = pairs.size == 1 && pairs[0].first.equals(currentWord, ignoreCase = true)
                                                                    val isTopical = dictKey == "topical"
                                                                    val newTitle = if (isTopical) {
                                                                        "References for $capitalizedWord"
                                                                    } else if (isExact) {
                                                                        "Definition of ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                                                    } else if (pairs.size == 1) {
                                                                        "Closest match for ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                                                    } else {
                                                                        "Matches for \"$capitalizedWord\""
                                                                    }
                                                                    val newContent = buildDefinitionContent(
                                                                        originalWord = word,
                                                                        pairs = pairs,
                                                                        isOxford = viewModel.selectedDictionary == "oxford",
                                                                        isTopical = viewModel.selectedDictionary == "topical"
                                                                    )
                                                                    val updateIndex = stack.indexOf(loadingPage)
                                                                    if (updateIndex != -1) {
                                                                        stack[updateIndex] = loadingPage.copy(title = newTitle, content = newContent)
                                                                    }
                                                                    withContext(Dispatchers.IO) { tempDbHelper.close() }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "versecommentary" -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Box {
                                            IconButton(
                                                onClick = { commentaryDropdownExpanded = true },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "Select Commentary",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = commentaryDropdownExpanded,
                                                onDismissRequest = { commentaryDropdownExpanded = false }
                                            ) {
                                                verseCommentaries.forEach { comKey ->
                                                    DropdownMenuItem(
                                                        text = { Text(verseCommentaryDisplayNames[comKey] ?: comKey) },
                                                        onClick = {
                                                            commentaryDropdownExpanded = false
                                                            val previous = viewModel.selectedVerseCommentary
                                                            if (previous != comKey) {
                                                                viewModel.selectedVerseCommentary = comKey
                                                                val bookNum = currentPage.bookNumber ?: return@DropdownMenuItem
                                                                val chap = currentPage.chapter ?: return@DropdownMenuItem
                                                                val vers = currentPage.verse ?: return@DropdownMenuItem
                                                                val newDisplayName = verseCommentaryDisplayNames[comKey] ?: comKey
                                                                val loadingPage = currentPage.copy(
                                                                    description = newDisplayName,
                                                                    content = "Loading..."
                                                                )
                                                                val index = stack.lastIndex
                                                                stack[index] = loadingPage
                                                                scope.launch {
                                                                    val tempDbHelper = withContext(Dispatchers.IO) {
                                                                        DatabaseHelper(context, "${comKey}.commentaries.sqlite3")
                                                                    }
                                                                    val commentaries = getVerseCommentaries(tempDbHelper, bookNum, chap, vers)
                                                                    val newContent = if (commentaries.isNullOrEmpty()) {
                                                                        "No commentaries found."
                                                                    } else {
                                                                        commentaries.joinToString("<br><br>──────────<br><br>") { commentary ->
                                                                            commentary.text
                                                                        }
                                                                    }
                                                                    val updateIndex = stack.indexOf(loadingPage)
                                                                    if (updateIndex != -1) {
                                                                        stack[updateIndex] = loadingPage.copy(content = newContent)
                                                                    }
                                                                    withContext(Dispatchers.IO) { tempDbHelper.close() }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "crossreference" -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Box {
                                            IconButton(
                                                onClick = { crossRefDropdownExpanded = true },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDropDown, "Select Cross-Reference DB")
                                            }
                                            DropdownMenu(
                                                expanded = crossRefDropdownExpanded,
                                                onDismissRequest = { crossRefDropdownExpanded = false }
                                            ) {
                                                crossReferenceDatabases.forEach { dbKey ->
                                                    DropdownMenuItem(
                                                        text = { Text(crossReferenceDatabaseDisplayNames[dbKey] ?: dbKey) },
                                                        onClick = {
                                                            crossRefDropdownExpanded = false
                                                            if (viewModel.selectedCrossReferenceDatabase != dbKey) {
                                                                viewModel.selectedCrossReferenceDatabase = dbKey
                                                                val loadingPage = currentPage.copy(
                                                                    description = crossReferenceDatabaseDisplayNames[dbKey] ?: dbKey,
                                                                    content = "Loading..."
                                                                )
                                                                stack[stack.lastIndex] = loadingPage
                                                                scope.launch {
                                                                    val temp = DatabaseHelper(context, "${dbKey}.crossreferences.sqlite3")
                                                                    val b = currentPage.bookNumber ?: return@launch
                                                                    val c = currentPage.chapter ?: return@launch
                                                                    val v = currentPage.verse ?: return@launch
                                                                    val refs = withContext(Dispatchers.IO) {
                                                                        temp.getCrossReferences(b, c, v)
                                                                    }
                                                                    val html = sanitizeHtmlContent(
                                                                        refs.joinToString("<br>") { ref ->
                                                                            val toBook = BibleData.getBookByCustomNumber(ref.bookTo)?.name ?: ref.bookTo.toString()
                                                                            val verseRange = if (ref.verseToStart == ref.verseToEnd) {
                                                                                ref.verseToStart.toString()
                                                                            } else {
                                                                                "${ref.verseToStart}-${ref.verseToEnd}"
                                                                            }
                                                                            val href = "B:${ref.bookTo} ${ref.chapterTo}:$verseRange"
                                                                            "<a href=\"$href\">$toBook ${ref.chapterTo}:$verseRange</a>"
                                                                        }
                                                                    )
                                                                    val idx = stack.indexOf(loadingPage)
                                                                    if (idx != -1) {
                                                                        stack[idx] = loadingPage.copy(content = html)
                                                                    }
                                                                    temp.close()
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                                isOldTestament = currentPage.isOldTestament
                            )
                            result[verse.verseNumber] = processed
                        }
                        result
                    }
                    val chapters = remember(verses) { verses.map { it.chapter }.distinct() }
                    val crossRefCounts = remember { mutableStateMapOf<Int, Int>() }
                    LaunchedEffect(chapters, crossRefDbHelper) {
                        crossRefCounts.clear()
                        chapters.forEach { chap ->
                            val counts = withContext(Dispatchers.IO) {
                                crossRefDbHelper?.getCrossReferenceCountsForChapter(passage.bookNumber, chap) ?: emptyMap()
                            }
                            crossRefCounts.putAll(counts)
                        }
                    }
                    var currentBatch by remember(verses) { mutableIntStateOf(50) }
                    val showChapterHeaders = remember(verses) { verses.mapNotNull { it.chapter }.distinct().size > 1 }

                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                    ) {
                        var lastChapter: Int? = null
                        verses.take(currentBatch).forEach { verse ->
                            if (showChapterHeaders && verse.chapter != lastChapter) {
                                Text(
                                    text = "Chapter ${verse.chapter}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontFamily = currentFontFamily
                                )
                                lastChapter = verse.chapter
                            }
                            val processedVerse = processedVerses[verse.verseNumber]
                            if (processedVerse != null) {
                                val refCount = crossRefCounts[verse.verseNumber] ?: 0
                                val annotatedString = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.verseNumber,
                                            fontSize = (viewModel.fontSize * 0.85f * 0.778f).sp
                                        )
                                    ) {
                                        append(" ${verse.verseNumber} ")
                                    }
                                    append(processedVerse.body)
                                    if (refCount > 0) {
                                        append(" ")
                                        appendInlineContent("crossref_${verse.verseNumber}", "[$refCount]")
                                    }
                                    appendInlineContent("commentary_${verse.verseNumber}", "[C]")
                                }
                                val inlineContentMap = remember(verse.verseNumber, refCount) {
                                    buildMap {
                                        if (refCount > 0) {
                                            put("crossref_${verse.verseNumber}", InlineTextContent(
                                                Placeholder(
                                                    width = (viewModel.fontSize * 1.2f).sp,
                                                    height = (viewModel.fontSize).sp,
                                                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clickable {
                                                            onCrossRefClick(
                                                                passage.bookNumber,
                                                                verse.chapter ?: passage.chapter,
                                                                verse.verseNumber,
                                                                currentPage.isOldTestament
                                                            )
                                                        }
                                                        .background(themeColors.primary.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                                        .fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$refCount",
                                                        fontSize = (viewModel.fontSize * 0.6f).sp,
                                                        color = themeColors.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        style = TextStyle(
                                                            lineHeight = (viewModel.fontSize).sp
                                                        )
                                                    )
                                                }
                                            })
                                        }
                                        put("commentary_${verse.verseNumber}", InlineTextContent(
                                            Placeholder(
                                                width = (viewModel.fontSize * 1.5f).sp,
                                                height = (viewModel.fontSize).sp,
                                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        onVerseCommentaryClick(
                                                            passage.bookNumber,
                                                            verse.chapter ?: passage.chapter,
                                                            verse.verseNumber,
                                                            currentPage.isOldTestament
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ChevronRight,
                                                    contentDescription = "View Verse Commentaries",
                                                    tint = themeColors.verseNumber,
                                                    modifier = Modifier.size((viewModel.fontSize).dp)
                                                )
                                            }
                                        })
                                    }
                                }
                                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    processedVerse.header?.let { header ->
                                        if (header.text.isNotEmpty()) {
                                            Text(
                                                text = header,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = themeColors.tagColor,
                                                fontFamily = currentFontFamily
                                            )
                                        }
                                    }
                                    Text(
                                        text = annotatedString,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(Unit) {
                                                detectTapGestures { offset ->
                                                    textLayoutResult?.let { layout ->
                                                        val position = layout.getOffsetForPosition(offset)
                                                        val annotations = annotatedString.getStringAnnotations(
                                                            start = position,
                                                            end = position
                                                        )
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
                                        inlineContent = inlineContentMap,
                                        onTextLayout = { textLayoutResult = it }
                                    )
                                }
                            }
                        }
                        if (currentBatch < verses.size) {
                            TextButton(
                                onClick = { currentBatch += 50 },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Load More")
                            }
                        }
                    }
                } else {
                    val content = currentPage.content
                    if (content == "Loading...") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading...")
                        }
                    } else {
                        key(currentPage.content ?: "") {
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
                                    val spanned = HtmlCompat.fromHtml(content ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT)
                                    val spannable = SpannableString(spanned)
                                    val urlSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
                                    for (urlSpan in urlSpans) {
                                        val start = spannable.getSpanStart(urlSpan)
                                        val end = spannable.getSpanEnd(urlSpan)
                                        val flags = spannable.getSpanFlags(urlSpan)
                                        val href = urlSpan.url
                                        val linkText = spannable.substring(start, end)
                                        spannable.removeSpan(urlSpan)
                                        var clickableSpan: ClickableSpan? = null
                                        var addClickable = false
                                        if (href.startsWith("B:")) {
                                            val passage = parseVerseLink(href, linkText)
                                            if (passage != null) {
                                                val book = BibleData.getBookByCustomNumber(passage.bookNumber)
                                                val isOld = book?.testament == Testament.OLD
                                                clickableSpan = object : ClickableSpan() {
                                                    override fun onClick(widget: View) {
                                                        val verseStart = passage.verse ?: return
                                                        val verseEndTemp = passage.verseEnd
                                                        if (verseEndTemp != null) {
                                                            val maxVerse = databaseHelper?.getVerseCount(passage.bookNumber, passage.chapter) ?: 0
                                                            if (verseEndTemp !in verseStart..maxVerse) {
                                                                passage.chapterEnd = verseEndTemp
                                                            }
                                                        }
                                                        val verses = fetchVerses(passage, databaseHelper)
                                                        val rangeStr = if (passage.chapterEnd != null) {
                                                            " ${passage.verse}-Ch ${passage.chapterEnd}"
                                                        } else {
                                                            " ${passage.verse}" + (passage.verseEnd?.let { "-$it" } ?: "")
                                                        }
                                                        val newTitle = " ${passage.bookName} ${passage.chapter}:$rangeStr"
                                                        stack.add(ModalPage(newTitle, "verses", verses = verses, passage = passage, isOldTestament = isOld))
                                                    }
                                                }
                                                addClickable = true
                                            }
                                        } else if (href.startsWith("S:")) {
                                            val seeContent = href.substringAfter("S:").trim()
                                            val cleanedLinkText = linkText.replace(Regex("^See\\s+", RegexOption.IGNORE_CASE), "").trim()
                                            when {
                                                seeContent.startsWith("B:") -> {
                                                    val verseHref = "B:" + seeContent.substringAfter("B:")
                                                    val passage = parseVerseLink(verseHref, linkText)
                                                    if (passage != null) {
                                                        val book = BibleData.getBookByCustomNumber(passage.bookNumber)
                                                        val isOld = book?.testament == Testament.OLD
                                                        clickableSpan = object : ClickableSpan() {
                                                            override fun onClick(widget: View) {
                                                                val verseStart = passage.verse ?: return
                                                                val verseEndTemp = passage.verseEnd
                                                                if (verseEndTemp != null) {
                                                                    val maxVerse = databaseHelper?.getVerseCount(passage.bookNumber, passage.chapter) ?: 0
                                                                    if (verseEndTemp !in verseStart..maxVerse) {
                                                                        passage.chapterEnd = verseEndTemp
                                                                        passage.verseEnd = null
                                                                    }
                                                                }
                                                                val verses = fetchVerses(passage, databaseHelper)
                                                                val rangeStr = if (passage.chapterEnd != null) {
                                                                    " ${passage.verse}-Ch ${passage.chapterEnd}"
                                                                } else {
                                                                    " ${passage.verse}" + (passage.verseEnd?.let { "-$it" } ?: "")
                                                                }
                                                                val newTitle = " ${passage.bookName} ${passage.chapter}:$rangeStr"
                                                                stack.add(ModalPage(newTitle, "verses", verses = verses, passage = passage, isOldTestament = isOld))
                                                            }
                                                        }
                                                        addClickable = true
                                                    }
                                                }
                                                seeContent.matches(Regex("^[GH]\\d+")) -> {
                                                    val isOld = seeContent.startsWith("H")
                                                    clickableSpan = object : ClickableSpan() {
                                                        override fun onClick(widget: View) {
                                                            val loadingPage = ModalPage("Strong's Definition for $seeContent", "strong", "Loading...", strongNumber = seeContent, isOldTestament = isOld)
                                                            stack.add(loadingPage)
                                                            scope.launch {
                                                                val definition = withContext(Dispatchers.IO) {
                                                                    strongDbHelper?.getStrongDefinition(seeContent) ?: "Strong's definition not found."
                                                                }
                                                                val prepared = prepareStrongContent(definition)
                                                                val index = stack.indexOf(loadingPage)
                                                                if (index != -1) {
                                                                    stack[index] = loadingPage.copy(content = prepared)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    addClickable = true
                                                }
                                                seeContent.matches(Regex("^\\d+")) -> {
                                                    val currentIsOld = stack.last().isOldTestament
                                                    val hNum = "H$seeContent"
                                                    val gNum = "G$seeContent"
                                                    clickableSpan = object : ClickableSpan() {
                                                        override fun onClick(widget: View) {
                                                            val loadingPage = ModalPage("Loading Strong's Definition", "strong", "Loading...", strongNumber = "$hNum,$gNum", isOldTestament = currentIsOld)
                                                            stack.add(loadingPage)
                                                            scope.launch {
                                                                val hDef = withContext(Dispatchers.IO) {
                                                                    strongDbHelper?.getStrongDefinition(hNum) ?: ""
                                                                }
                                                                val gDef = withContext(Dispatchers.IO) {
                                                                    strongDbHelper?.getStrongDefinition(gNum) ?: ""
                                                                }
                                                                var combinedDef = ""
                                                                var combinedTitle = "Strong's Definition"
                                                                var combinedStrongNum = ""
                                                                val preparedH = if (hDef.isNotBlank()) prepareStrongContent(hDef) else ""
                                                                val preparedG = if (gDef.isNotBlank()) prepareStrongContent(gDef) else ""
                                                                when {
                                                                    preparedH.isNotBlank() && preparedG.isNotBlank() -> {
                                                                        combinedTitle += " for $hNum and $gNum"
                                                                        combinedDef = "$hNum:\n$preparedH\n\n$gNum:\n$preparedG"
                                                                        combinedStrongNum = "$hNum,$gNum"
                                                                    }
                                                                    preparedH.isNotBlank() -> {
                                                                        combinedTitle += " for $hNum"
                                                                        combinedDef = preparedH
                                                                        combinedStrongNum = hNum
                                                                    }
                                                                    preparedG.isNotBlank() -> {
                                                                        combinedTitle += " for $gNum"
                                                                        combinedDef = preparedG
                                                                        combinedStrongNum = gNum
                                                                    }
                                                                    else -> {
                                                                        combinedTitle = "Strong's Definition not found"
                                                                        combinedDef = "No definition found."
                                                                    }
                                                                }
                                                                val index = stack.indexOf(loadingPage)
                                                                if (index != -1) {
                                                                    stack[index] = loadingPage.copy(title = combinedTitle, content = combinedDef, strongNumber = combinedStrongNum)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    addClickable = true
                                                }
                                                else -> {
                                                    val wordToFetch = cleanedLinkText.ifEmpty { seeContent }
                                                    clickableSpan = object : ClickableSpan() {
                                                        override fun onClick(widget: View) {
                                                            val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
                                                            val capitalizedWordToFetch = wordToFetch.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                                            val loadingTitle = "Loading Definition of $capitalizedWordToFetch"
                                                            val loadingPage = ModalPage(
                                                                loadingTitle, "definition", "Loading...",
                                                                word = wordToFetch,
                                                                description = dbDisplayName,
                                                                isOldTestament = stack.last().isOldTestament
                                                            )
                                                            stack.add(loadingPage)
                                                            scope.launch {
                                                                val pairs: List<Pair<String, String>> = getDefinitionOrClosest(dictionaryDbHelper, wordToFetch) ?: emptyList()
                                                                val newContentInner: String
                                                                val newTitleInner: String
                                                                if (pairs.isNotEmpty()) {
                                                                    val isExact = pairs.size == 1 && pairs[0].first.equals(wordToFetch, ignoreCase = true)
                                                                    val isTopical = viewModel.selectedDictionary == "topical"
                                                                    newTitleInner = if (isTopical) {
                                                                        "References for $capitalizedWordToFetch"
                                                                    } else if (isExact) {
                                                                        "Definition of ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                                                    } else if (pairs.size == 1) {
                                                                        "Closest match for ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                                                    } else {
                                                                        "Matches for \"$capitalizedWordToFetch\""
                                                                    }
                                                                    newContentInner = if (isExact) {
                                                                        sanitizeHtmlContent(pairs[0].second)
                                                                    } else if (pairs.size == 1) {
                                                                        cleanDefinition(pairs[0].first, pairs[0].second)
                                                                    } else {
                                                                        pairs.joinToString("<br><hr><br>") { p -> sanitizeHtmlContent(p.second) }
                                                                    }
                                                                } else {
                                                                    newTitleInner = "Definition of $capitalizedWordToFetch not found"
                                                                    newContentInner = "No definition found."
                                                                }
                                                                val index = stack.indexOf(loadingPage)
                                                                if (index != -1) {
                                                                    stack[index] = loadingPage.copy(title = newTitleInner, content = newContentInner)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    addClickable = true
                                                }
                                            }
                                        }
                                        if (addClickable) {
                                            spannable.setSpan(clickableSpan, start, end, flags)
                                        }
                                    }
                                    if (isDark) {
                                        val colorSpans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
                                        for (span in colorSpans) {
                                            spannable.removeSpan(span)
                                        }
                                        textView.setTextColor(Color.White.toArgb())
                                    } else {
                                        textView.setTextColor(textColor.toArgb())
                                    }
                                    textView.setLinkTextColor(linkColor.toArgb())
                                    textView.text = spannable
                                },
                                modifier = Modifier
                                    .verticalScroll(scrollState)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    if (stack.size > 1) {
                        TextButton(onClick = { stack.removeAt(stack.lastIndex) }) {
                            Text("Back")
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            },
            dismissButton = when (currentPage.type) {
                "definition" -> {
                    {
                        val currentIndex = dictionaries.indexOf(viewModel.selectedDictionary)
                        val nextIndex = (currentIndex + 1) % dictionaries.size
                        val nextDictionary = dictionaries[nextIndex]
                        val nextDisplayName = dictionaryDisplayNames[nextDictionary] ?: nextDictionary
                        TextButton(onClick = {
                            val currentWord = currentPage.word ?: return@TextButton
                            val loadingTitle = "Switching to ${nextDictionary.uppercase()}"
                            val loadingPage = currentPage.copy(
                                title = loadingTitle,
                                content = "Loading...",
                                description = nextDisplayName
                            )
                            val index = stack.lastIndex
                            stack[index] = loadingPage
                            scope.launch {
                                val tempDbHelper = withContext(Dispatchers.IO) {
                                    DatabaseHelper(context, "${nextDictionary}.dictionary.sqlite3")
                                }
                                val pairs: List<Pair<String, String>> = getDefinitionOrClosest(tempDbHelper, currentWord) ?: emptyList()
                                val capitalizedWord = currentWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                val isExact = pairs.size == 1 && pairs[0].first.equals(currentWord, ignoreCase = true)
                                val isTopical = nextDictionary == "topical"
                                val newTitle = if (isTopical) {
                                    "References for $capitalizedWord"
                                } else if (isExact) {
                                    "Definition of ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                } else if (pairs.size == 1) {
                                    "Closest match for ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                } else {
                                    "Matches for \"$capitalizedWord\""
                                }
                                val newContent = buildDefinitionContent(
                                    originalWord = word,
                                    pairs = pairs,
                                    isOxford = viewModel.selectedDictionary == "oxford",
                                    isTopical = viewModel.selectedDictionary == "topical"
                                )
                                val updateIndex = stack.indexOf(loadingPage)
                                if (updateIndex != -1) {
                                    stack[updateIndex] = loadingPage.copy(title = newTitle, content = newContent)
                                }
                                withContext(Dispatchers.IO) { tempDbHelper.close() }
                            }
                            viewModel.selectedDictionary = nextDictionary
                        }) {
                            Text("Switch to ${nextDictionary.uppercase()}")
                        }
                    }
                }
                "versecommentary" -> {
                    {
                        val currentIndex = verseCommentaries.indexOf(viewModel.selectedVerseCommentary)
                        val nextIndex = (currentIndex + 1) % verseCommentaries.size
                        val nextKey = verseCommentaries[nextIndex]
                        val nextDisplayName = verseCommentaryDisplayNames[nextKey] ?: nextKey
                        TextButton(onClick = {
                            val bookNum = currentPage.bookNumber ?: return@TextButton
                            val chap = currentPage.chapter ?: return@TextButton
                            val vers = currentPage.verse ?: return@TextButton
                            val loadingPage = currentPage.copy(
                                description = nextDisplayName,
                                content = "Loading..."
                            )
                            val index = stack.lastIndex
                            stack[index] = loadingPage
                            scope.launch {
                                val tempDbHelper = withContext(Dispatchers.IO) {
                                    DatabaseHelper(context, "${nextKey}.commentaries.sqlite3")
                                }
                                val commentaries = getVerseCommentaries(tempDbHelper, bookNum, chap, vers)
                                val newContent = if (commentaries.isNullOrEmpty()) {
                                    "No commentaries found."
                                } else {
                                    commentaries.joinToString("<br><br>──────────<br><br>") { commentary ->
                                        commentary.text
                                    }
                                }
                                val updateIndex = stack.indexOf(loadingPage)
                                if (updateIndex != -1) {
                                    stack[updateIndex] = loadingPage.copy(content = newContent)
                                }
                                withContext(Dispatchers.IO) { tempDbHelper.close() }
                            }
                            viewModel.selectedVerseCommentary = nextKey
                        }) {
                            Text("Switch to ${nextKey.uppercase()}")
                        }
                    }
                }
                "crossreference" -> {
                    {
                        val idx = crossReferenceDatabases.indexOf(viewModel.selectedCrossReferenceDatabase)
                        val nextKey = crossReferenceDatabases[(idx + 1) % crossReferenceDatabases.size]
                        TextButton(onClick = {
                            viewModel.selectedCrossReferenceDatabase = nextKey
                            val loadingPage = currentPage.copy(
                                description = crossReferenceDatabaseDisplayNames[nextKey] ?: nextKey,
                                content = "Loading..."
                            )
                            stack[stack.lastIndex] = loadingPage
                            scope.launch {
                                val temp = DatabaseHelper(context, "${nextKey}.crossreferences.sqlite3")
                                val b = currentPage.bookNumber ?: return@launch
                                val c = currentPage.chapter ?: return@launch
                                val v = currentPage.verse ?: return@launch
                                val refs = withContext(Dispatchers.IO) {
                                    temp.getCrossReferences(b, c, v)
                                }
                                val html = sanitizeHtmlContent(
                                    refs.joinToString("<br>") { ref ->
                                        val toBook = BibleData.getBookByCustomNumber(ref.bookTo)?.name ?: ref.bookTo.toString()
                                        val verseRange = if (ref.verseToStart == ref.verseToEnd) {
                                            ref.verseToStart.toString()
                                        } else {
                                            "${ref.verseToStart}-${ref.verseToEnd}"
                                        }
                                        val href = "B:${ref.bookTo} ${ref.chapterTo}:$verseRange"
                                        "<a href=\"$href\">$toBook ${ref.chapterTo}:$verseRange</a>"
                                    }
                                )
                                val idx2 = stack.indexOf(loadingPage)
                                if (idx2 != -1) {
                                    stack[idx2] = loadingPage.copy(content = html)
                                }
                                temp.close()
                            }
                        }) {
                            Text("Switch to ${nextKey.uppercase()}")
                        }
                    }
                }
                else -> null
            },
            containerColor = modalBackgroundColor
        )

        if (showModalColorWheel) {
            ColorWheelDialog(
                onDismissRequest = { showModalColorWheel = false },
                onColorSelected = { selectedColor ->
                    if (isDark) {
                        viewModel.darkModalBackgroundColor = selectedColor
                    } else {
                        viewModel.lightModalBackgroundColor = selectedColor
                    }
                    showModalColorWheel = false
                },
                initialColor = modalBackgroundColor
            )
        }

        if (showEditWordDialog) {
            var newWord by remember { mutableStateOf(currentPage.word ?: "") }
            AlertDialog(
                onDismissRequest = { showEditWordDialog = false },
                title = { Text("Edit Word") },
                text = {
                    TextField(
                        value = newWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        onValueChange = { newWord = it },
                        singleLine = true,
                        label = { Text("Enter word") }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEditWordDialog = false
                            val trimmedWord = newWord.trim()
                            if (trimmedWord.isNotEmpty() && trimmedWord != currentPage.word) {
                                val capitalizedWord = trimmedWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                val loadingTitle = "Searching for $capitalizedWord..."
                                val loadingPage = currentPage.copy(
                                    title = loadingTitle,
                                    content = "Loading...",
                                    word = trimmedWord
                                )
                                val index = stack.lastIndex
                                stack[index] = loadingPage
                                scope.launch {
                                    val pairs = getDefinitionOrClosest(dictionaryDbHelper, trimmedWord) ?: emptyList()
                                    val isExact = pairs.size == 1 && pairs[0].first.equals(trimmedWord, ignoreCase = true)
                                    val isTopical = viewModel.selectedDictionary == "topical"
                                    val newTitle = if (isTopical) {
                                        "References for $capitalizedWord"
                                    } else if (isExact) {
                                        "Definition of ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                    } else if (pairs.size == 1) {
                                        "Closest match for ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                    } else {
                                        "Matches for \"$capitalizedWord\""
                                    }
                                    val newContent = buildDefinitionContent(
                                        originalWord = word,
                                        pairs = pairs,
                                        isOxford = viewModel.selectedDictionary == "oxford",
                                        isTopical = viewModel.selectedDictionary == "topical"
                                    )
                                    val updateIndex = stack.indexOf(loadingPage)
                                    if (updateIndex != -1) {
                                        stack[updateIndex] = loadingPage.copy(title = newTitle, content = newContent)
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditWordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}