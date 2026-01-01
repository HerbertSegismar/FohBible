package com.example.fohbible

import android.content.Context
import com.example.fohbible.data.DatabaseHelper

data class BibleBook(
    val name: String,
    val chapters: Int,
    val testament: Testament,
    val abbreviation: String,
    val number: Int,
    val standardNumber: Int
) {
    // Make this accept either Context or DatabaseHelper
    fun getVersesForChapter(chapter: Int, context: Context? = null): Int {
        return (if (context != null) {
            // Use DatabaseHelper to get verse count
            val dbHelper = DatabaseHelper(context)
            dbHelper.getVerseCount(number, chapter)
        } else {
            getDefaultVerseCount(chapter)
        })
    }

    private fun getDefaultVerseCount(chapter: Int): Int {
        // Simple defaults
        return when (name) {
            "Genesis" -> when (chapter) {
                1 -> 31
                2 -> 25
                else -> 30
            }
            "Matthew" -> when (chapter) {
                1 -> 25
                2 -> 23
                else -> 30
            }
            "Psalms" -> when (chapter) {
                1 -> 6
                23 -> 6
                119 -> 176
                else -> 20
            }
            "John" -> when (chapter) {
                1 -> 51
                else -> 30
            }
            else -> 30
        }
    }

    fun getTestament(): String = if (number in 10..460) "OT" else "NT"
}

enum class Testament {
    OLD, NEW
}

object BibleData {
    val BIBLE_BOOKS_MAP = mapOf(
        // Old Testament
        10 to BibleBook("Genesis", 50, Testament.OLD, "Gen", 10, 1),
        20 to BibleBook("Exodus", 40, Testament.OLD, "Exo", 20, 2),
        30 to BibleBook("Leviticus", 27, Testament.OLD, "Lev", 30, 3),
        40 to BibleBook("Numbers", 36, Testament.OLD, "Num", 40, 4),
        50 to BibleBook("Deuteronomy", 34, Testament.OLD, "Deu", 50, 5),
        60 to BibleBook("Joshua", 24, Testament.OLD, "Josh", 60, 6),
        70 to BibleBook("Judges", 21, Testament.OLD, "Judg", 70, 7),
        80 to BibleBook("Ruth", 4, Testament.OLD, "Ruth", 80, 8),
        90 to BibleBook("1 Samuel", 31, Testament.OLD, "1Sam", 90, 9),
        100 to BibleBook("2 Samuel", 24, Testament.OLD, "2Sam", 100, 10),
        110 to BibleBook("1 Kings", 22, Testament.OLD, "1Kin", 110, 11),
        120 to BibleBook("2 Kings", 25, Testament.OLD, "2Kin", 120, 12),
        130 to BibleBook("1 Chronicles", 29, Testament.OLD, "1Chr", 130, 13),
        140 to BibleBook("2 Chronicles", 36, Testament.OLD, "2Chr", 140, 14),
        150 to BibleBook("Ezra", 10, Testament.OLD, "Ezra", 150, 15),
        160 to BibleBook("Nehemiah", 13, Testament.OLD, "Neh", 160, 16),
        190 to BibleBook("Esther", 10, Testament.OLD, "Esth", 190, 17),
        220 to BibleBook("Job", 42, Testament.OLD, "Job", 220, 18),
        230 to BibleBook("Psalms", 150, Testament.OLD, "Ps", 230, 19),
        240 to BibleBook("Proverbs", 31, Testament.OLD, "Prov", 240, 20),
        250 to BibleBook("Ecclesiastes", 12, Testament.OLD, "Eccl", 250, 21),
        260 to BibleBook("Song of Solomon", 8, Testament.OLD, "Song", 260, 22),
        290 to BibleBook("Isaiah", 66, Testament.OLD, "Isa", 290, 23),
        300 to BibleBook("Jeremiah", 52, Testament.OLD, "Jer", 300, 24),
        310 to BibleBook("Lamentations", 5, Testament.OLD, "Lam", 310, 25),
        330 to BibleBook("Ezekiel", 48, Testament.OLD, "Ezek", 330, 26),
        340 to BibleBook("Daniel", 12, Testament.OLD, "Dan", 340, 27),
        350 to BibleBook("Hosea", 14, Testament.OLD, "Hos", 350, 28),
        360 to BibleBook("Joel", 3, Testament.OLD, "Joel", 360, 29),
        370 to BibleBook("Amos", 9, Testament.OLD, "Amos", 370, 30),
        380 to BibleBook("Obadiah", 1, Testament.OLD, "Obad", 380, 31),
        390 to BibleBook("Jonah", 4, Testament.OLD, "Jon", 390, 32),
        400 to BibleBook("Micah", 7, Testament.OLD, "Mic", 400, 33),
        410 to BibleBook("Nahum", 3, Testament.OLD, "Nah", 410, 34),
        420 to BibleBook("Habakkuk", 3, Testament.OLD, "Hab", 420, 35),
        430 to BibleBook("Zephaniah", 3, Testament.OLD, "Zeph", 430, 36),
        440 to BibleBook("Haggai", 2, Testament.OLD, "Hag", 440, 37),
        450 to BibleBook("Zechariah", 14, Testament.OLD, "Zech", 450, 38),
        460 to BibleBook("Malachi", 4, Testament.OLD, "Mal", 460, 39),

        // New Testament
        470 to BibleBook("Matthew", 28, Testament.NEW, "Matt", 470, 40),
        480 to BibleBook("Mark", 16, Testament.NEW, "Mark", 480, 41),
        490 to BibleBook("Luke", 24, Testament.NEW, "Luke", 490, 42),
        500 to BibleBook("John", 21, Testament.NEW, "John", 500, 43),
        510 to BibleBook("Acts", 28, Testament.NEW, "Acts", 510, 44),
        520 to BibleBook("Romans", 16, Testament.NEW, "Rom", 520, 45),
        530 to BibleBook("1 Corinthians", 16, Testament.NEW, "1Cor", 530, 46),
        540 to BibleBook("2 Corinthians", 13, Testament.NEW, "2Cor", 540, 47),
        550 to BibleBook("Galatians", 6, Testament.NEW, "Gal", 550, 48),
        560 to BibleBook("Ephesians", 6, Testament.NEW, "Eph", 560, 49),
        570 to BibleBook("Philippians", 4, Testament.NEW, "Phil", 570, 50),
        580 to BibleBook("Colossians", 4, Testament.NEW, "Col", 580, 51),
        590 to BibleBook("1 Thessalonians", 5, Testament.NEW, "1Thes", 590, 52),
        600 to BibleBook("2 Thessalonians", 3, Testament.NEW, "2Thes", 600, 53),
        610 to BibleBook("1 Timothy", 6, Testament.NEW, "1Tim", 610, 54),
        620 to BibleBook("2 Timothy", 4, Testament.NEW, "2Tim", 620, 55),
        630 to BibleBook("Titus", 3, Testament.NEW, "Titus", 630, 56),
        640 to BibleBook("Philemon", 1, Testament.NEW, "Phlm", 640, 57),
        650 to BibleBook("Hebrews", 13, Testament.NEW, "Heb", 650, 58),
        660 to BibleBook("James", 5, Testament.NEW, "James", 660, 59),
        670 to BibleBook("1 Peter", 5, Testament.NEW, "1Pet", 670, 60),
        680 to BibleBook("2 Peter", 3, Testament.NEW, "2Pet", 680, 61),
        690 to BibleBook("1 John", 5, Testament.NEW, "1John", 690, 62),
        700 to BibleBook("2 John", 1, Testament.NEW, "2John", 700, 63),
        710 to BibleBook("3 John", 1, Testament.NEW, "3John", 710, 64),
        720 to BibleBook("Jude", 1, Testament.NEW, "Jude", 720, 65),
        730 to BibleBook("Revelation", 22, Testament.NEW, "Rev", 730, 66)
    )

    val allBooks: List<BibleBook> = BIBLE_BOOKS_MAP.values.toList()

    val oldTestamentBooks: List<BibleBook> = allBooks.filter { it.testament == Testament.OLD }
    val newTestamentBooks: List<BibleBook> = allBooks.filter { it.testament == Testament.NEW }

    // Get book by SQLite book number
    fun getBookByNumber(bookNumber: Int): BibleBook? {
        return BIBLE_BOOKS_MAP[bookNumber]
    }
}

