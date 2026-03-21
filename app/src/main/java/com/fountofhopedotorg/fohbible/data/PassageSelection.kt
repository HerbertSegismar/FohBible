package com.fountofhopedotorg.fohbible.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.ui.theme.DefaultPrimaryColor

data class PassageSelection(
    val bookNumber: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int? = null,
    var verseEnd: Int? = null,
    var chapterEnd: Int? = null
)

data class ModalPage(
    val title: String,
    val type: String,
    val content: String? = null,
    val verses: List<Verse>? = null,
    val passage: PassageSelection? = null,
    val word: String? = null,
    val strongNumber: String? = null,
    val description: String? = null,
    val isOldTestament: Boolean,
    val bookNumber: Int? = null,
    val chapter: Int? = null,
    val verse: Int? = null
)

data class Verse(
    val verseNumber: Int,
    val text: String,
    val bookName: String? = null,
    val chapter: Int? = null,
    val bookNumber: Int? = null
)

data class CrossReference(
    val bookTo: Int,
    val chapterTo: Int,
    val verseToStart: Int,
    val verseToEnd: Int
)

data class Note(
    val bookName: String,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int,
    val note: String,
    val timestamp: Long
)

data class VerseCommentary(
    val text: String,
    val chapterFrom: Int,
    val verseFrom: Int,
    val chapterTo: Int?,
    val verseTo: Int?
)

data class ScopeRange(val start: Int, val end: Int)

data class BibleBook(
    val customNumber: Int,
    val name: String,
    val chapters: Int,
    val testament: Testament,
    val abbreviation: String,
    val standardNumber: Int = 0
) {
    fun getVersesForChapter(chapter: Int, context: Context? = null): Int {
        return if (context != null) {
            val dbHelper = DatabaseHelper(context as MainActivity, databaseName = "kj2.sqlite3")
            dbHelper.getVerseCount(customNumber, chapter)
        } else {
            30
        }
    }
}

data class ScopeConfig(
    val label: String,
    val description: String,
    val category: String
)

data class Subheading(val verse: Int, val text: String)

sealed class VerseContent {
    data class SubheadingVal(val subheading: Subheading) : VerseContent()
    data class VerseVal(val verse: Verse) : VerseContent()
}

data class AppThemeState(
    val darkTheme: Boolean = false,
    val primaryColor: Color = DefaultPrimaryColor,
    val isCustomColor: Boolean = false
)