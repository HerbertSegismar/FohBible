package com.fountofhopedotorg.fohbible.data

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