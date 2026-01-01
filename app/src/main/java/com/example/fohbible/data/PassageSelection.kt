package com.example.fohbible.data

data class PassageSelection(
    val bookNumber: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int?
)

data class Verse(
    val verseNumber: Int,
    val text: String
)