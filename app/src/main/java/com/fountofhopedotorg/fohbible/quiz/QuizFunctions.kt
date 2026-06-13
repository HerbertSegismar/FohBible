package com.fountofhopedotorg.fohbible.quiz

import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.QuizItem
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor

fun generateQuizItems(dbHelper: DatabaseHelper, count: Int): List<QuizItem> {
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
            val w = words[it].replace(Regex("[^A-Za-z]"), "")
            w.length >= 3
        }
        if (eligibleIndices.isEmpty()) continue
        val hideIndex = eligibleIndices.random()
        val missingWord = words[hideIndex].replace(Regex("[^A-Za-z]"), "")
        val displayText = words.mapIndexed { i, w ->
            if (i == hideIndex) "_".repeat(w.replace(Regex("[^A-Za-z]"), "").length)
            else w
        }.joinToString(" ")

        items.add(
            QuizItem(
                verse = verse,
                missingWord = missingWord,
                displayText = displayText
            )
        )
    }
    return items
}