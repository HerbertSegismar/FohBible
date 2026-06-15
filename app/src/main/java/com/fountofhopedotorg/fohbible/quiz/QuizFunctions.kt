package com.fountofhopedotorg.fohbible.quiz

import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.QuizItem
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import kotlin.random.Random

enum class QuizType { FILL_IN_THE_BLANK, MULTIPLE_CHOICE }

fun generateQuizItems(
    dbHelper: DatabaseHelper,
    count: Int,
    type: QuizType = QuizType.FILL_IN_THE_BLANK,
    bookRange: Pair<Int, Int>? = null
): List<QuizItem> {
    val items = mutableListOf<QuizItem>()
    val usedVerses = mutableSetOf<String>()
    val random = Random
    var maxAttempts = count * 30
    val eligibleBooks = if (bookRange != null) {
        BibleData.allBooks.filter { it.customNumber in bookRange.first..bookRange.second }
    } else {
        BibleData.allBooks
    }
    if (eligibleBooks.isEmpty()) return emptyList()

    while (items.size < count && maxAttempts > 0) {
        val book = eligibleBooks[random.nextInt(eligibleBooks.size)]
        val chapter = random.nextInt(book.chapters) + 1
        val verseCount = dbHelper.getVerseCount(book.customNumber, chapter)
        if (verseCount == 0) continue

        val verseNumber = random.nextInt(verseCount) + 1
        val verses = dbHelper.getVerses(book.customNumber, chapter)
        val verse = verses.find { it.verseNumber == verseNumber } ?: continue
        val enrichedVerse = verse.copy(
            bookName = book.name,
            chapter = chapter
        )

        val key = "${book.name}:${chapter}:${verseNumber}"
        if (usedVerses.contains(key)) continue
        usedVerses.add(key)

        val cleanText = SimpleVerseProcessor.stripXmlTags(enrichedVerse.text)
        if (cleanText.isBlank()) continue
        val words = cleanText.split(" ")
        if (words.isEmpty()) continue

        val eligibleIndices = words.indices.filter {
            val w = words[it].replace(Regex("[^\\p{L}]"), "")
            w.length >= 5
        }
        if (eligibleIndices.isEmpty()) continue
        val hideIndex = eligibleIndices.random()
        val missingWord = words[hideIndex].replace(Regex("[^\\p{L}]"), "")
        val displayText = words.mapIndexed { i, w ->
            if (i == hideIndex) {
                val clean = w.replace(Regex("[^\\p{L}]"), "")
                val leading = w.substringBefore(clean)
                val trailing = w.substringAfterLast(clean)
                "${leading}${"_".repeat(clean.length)}$trailing"
            } else w
        }.joinToString(" ")

        val adjacentWords = listOfNotNull(
            words.getOrNull(hideIndex - 1)?.replace(Regex("[^\\p{L}]"), ""),
            words.getOrNull(hideIndex + 1)?.replace(Regex("[^\\p{L}]"), "")
        ).filter { it.isNotEmpty() }

        val options = if (type == QuizType.MULTIPLE_CHOICE) {
            val distractors = dbHelper.getRandomDistractors(
                excludeWord = missingWord,
                count = 3,
                additionalExcludeWords = adjacentWords.toSet()
            )
            (listOf(missingWord) + distractors.take(3)).shuffled()
        } else {
            emptyList()
        }

        items.add(
            QuizItem(
                verse = enrichedVerse,
                missingWord = missingWord,
                displayText = displayText,
                options = options
            )
        )
        maxAttempts--
    }
    return items
}