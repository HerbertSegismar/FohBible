package com.fountofhopedotorg.fohbible.modals

import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.VerseCommentary
import com.fountofhopedotorg.fohbible.utils.InteractiveModalUtils.dictionariesByLanguage
import kotlinx.coroutines.Dispatchers
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
        return sanitized.take(index) + "<br><br>" + sanitized.substring(index)
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

fun getLanguageForDictionary(dictKey: String): String? {
    return dictionariesByLanguage.entries.find { it.value.contains(dictKey) }?.key
}