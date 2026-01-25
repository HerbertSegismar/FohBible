package com.example.fohbible.data

data class PassageSelection(
    val bookNumber: Int,
    val bookName: String,
    val chapter: Int,          // Start chapter
    val verse: Int,            // Start verse
    val verseEnd: Int? = null, // End verse (same chapter)
    val endChapter: Int? = null // End chapter for multi-chapter ranges
) {
    // Helper property to check if this is a multi-chapter range
    val isMultiChapter: Boolean get() = endChapter != null && endChapter != chapter

    // Helper property to get the actual end chapter
    val actualEndChapter: Int get() = endChapter ?: chapter
}
data class Verse(
    val verseNumber: Int,
    val text: String,
    val bookName: String? = null,
    val chapter: Int? = null,
    val bookNumber: Int? = null
)