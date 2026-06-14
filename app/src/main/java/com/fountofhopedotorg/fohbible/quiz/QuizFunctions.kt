package com.fountofhopedotorg.fohbible.quiz

import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.QuizItem
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor

enum class QuizType { FILL_IN_THE_BLANK, MULTIPLE_CHOICE }

fun generateQuizItems(
    dbHelper: DatabaseHelper,
    count: Int,
    type: QuizType = QuizType.FILL_IN_THE_BLANK
): List<QuizItem> {
    val items = mutableListOf<QuizItem>()
    val usedVerses = mutableSetOf<String>()

    while (items.size < count) {
        val verses = dbHelper.getRandomVerses()
        if (verses.isEmpty()) continue
        val verse = verses.first()
        val key = "${verse.bookName}:${verse.chapter}:${verse.verseNumber}"
        if (usedVerses.contains(key)) continue
        usedVerses.add(key)

        val cleanText = SimpleVerseProcessor.stripXmlTags(verse.text)
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
                verse = verse,
                missingWord = missingWord,
                displayText = displayText,
                options = options
            )
        )
    }
    return items
}