package com.example.fohbible.data

data class PassageSelection(
    val bookNumber: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int? = null,
    var verseEnd: Int? = null,
    var chapterEnd: Int? = null
)

data class Verse(
    val verseNumber: Int,
    val text: String,
    val bookName: String? = null,
    val chapter: Int? = null,
    val bookNumber: Int? = null
)