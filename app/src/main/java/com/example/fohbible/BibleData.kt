package com.example.fohbible

data class BibleBook(
    val name: String,
    val chapters: Int,
    val testament: Testament,
    val abbreviation: String,
    val number: Int,
    val sqliteNumber: Int? = null
) {
    fun getVersesForChapter(chapter: Int): Int {
        // This would ideally come from a database or JSON file
        // For now, return placeholder data - in real app, use actual verse counts
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
            else -> 30
        }
    }
}

enum class Testament {
    OLD, NEW
}

object BibleData {
    // Map from SQLite book numbers to standard book numbers
    private val sqliteToStandardMap: Map<Int, Int> = mapOf(
        10 to 1,
        20 to 2,
        30 to 3,
        40 to 4,
        50 to 5,
        60 to 6,
        70 to 7,
        80 to 8,
        90 to 9,
        100 to 10,
        110 to 11,
        120 to 12,
        130 to 13,
        140 to 14,
        150 to 15,
        160 to 16,
        190 to 17,
        220 to 18,
        230 to 19,
        240 to 20,
        250 to 21,
        260 to 22,
        290 to 23,
        300 to 24,
        310 to 25,
        330 to 26,
        340 to 27,
        350 to 28,
        360 to 29,
        370 to 30,
        380 to 31,
        390 to 32,
        400 to 33,
        410 to 34,
        420 to 35,
        430 to 36,
        440 to 37,
        450 to 38,
        460 to 39,
        470 to 40,
        480 to 41,
        490 to 42,
        500 to 43,
        510 to 44,
        520 to 45,
        530 to 46,
        540 to 47,
        550 to 48,
        560 to 49,
        570 to 50,
        580 to 51,
        590 to 52,
        600 to 53,
        610 to 54,
        620 to 55,
        630 to 56,
        640 to 57,
        650 to 58,
        660 to 59,
        670 to 60,
        680 to 61,
        690 to 62,
        700 to 63,
        710 to 64,
        720 to 65,
        730 to 66
    )

    // Map from standard book numbers to SQLite book numbers
    private val standardToSqliteMap: Map<Int, Int> = sqliteToStandardMap.entries.associate { (key, value) -> value to key }

    val oldTestamentBooks = listOf(
        BibleBook("Genesis", 50, Testament.OLD, "Gen", 1, sqliteNumber = 10),
        BibleBook("Exodus", 40, Testament.OLD, "Exo", 2, sqliteNumber = 20),
        BibleBook("Leviticus", 27, Testament.OLD, "Lev", 3, sqliteNumber = 30),
        BibleBook("Numbers", 36, Testament.OLD, "Num", 4, sqliteNumber = 40),
        BibleBook("Deuteronomy", 34, Testament.OLD, "Deu", 5, sqliteNumber = 50),
        BibleBook("Joshua", 24, Testament.OLD, "Josh", 6, sqliteNumber = 60),
        BibleBook("Judges", 21, Testament.OLD, "Judg", 7, sqliteNumber = 70),
        BibleBook("Ruth", 4, Testament.OLD, "Ruth", 8, sqliteNumber = 80),
        BibleBook("1 Samuel", 31, Testament.OLD, "1Sam", 9, sqliteNumber = 90),
        BibleBook("2 Samuel", 24, Testament.OLD, "2Sam", 10, sqliteNumber = 100),
        BibleBook("1 Kings", 22, Testament.OLD, "1Kin", 11, sqliteNumber = 110),
        BibleBook("2 Kings", 25, Testament.OLD, "2Kin", 12, sqliteNumber = 120),
        BibleBook("1 Chronicles", 29, Testament.OLD, "1Chr", 13, sqliteNumber = 130),
        BibleBook("2 Chronicles", 36, Testament.OLD, "2Chr", 14, sqliteNumber = 140),
        BibleBook("Ezra", 10, Testament.OLD, "Ezra", 15, sqliteNumber = 150),
        BibleBook("Nehemiah", 13, Testament.OLD, "Neh", 16, sqliteNumber = 160),
        BibleBook("Esther", 10, Testament.OLD, "Esth", 17, sqliteNumber = 190),
        BibleBook("Job", 42, Testament.OLD, "Job", 18, sqliteNumber = 220),
        BibleBook("Psalms", 150, Testament.OLD, "Ps", 19, sqliteNumber = 230),
        BibleBook("Proverbs", 31, Testament.OLD, "Prov", 20, sqliteNumber = 240),
        BibleBook("Ecclesiastes", 12, Testament.OLD, "Eccl", 21, sqliteNumber = 250),
        BibleBook("Song of Solomon", 8, Testament.OLD, "Song", 22, sqliteNumber = 260),
        BibleBook("Isaiah", 66, Testament.OLD, "Isa", 23, sqliteNumber = 290),
        BibleBook("Jeremiah", 52, Testament.OLD, "Jer", 24, sqliteNumber = 300),
        BibleBook("Lamentations", 5, Testament.OLD, "Lam", 25, sqliteNumber = 310),
        BibleBook("Ezekiel", 48, Testament.OLD, "Ezek", 26, sqliteNumber = 330),
        BibleBook("Daniel", 12, Testament.OLD, "Dan", 27, sqliteNumber = 340),
        BibleBook("Hosea", 14, Testament.OLD, "Hos", 28, sqliteNumber = 350),
        BibleBook("Joel", 3, Testament.OLD, "Joel", 29, sqliteNumber = 360),
        BibleBook("Amos", 9, Testament.OLD, "Amos", 30, sqliteNumber = 370),
        BibleBook("Obadiah", 1, Testament.OLD, "Obad", 31, sqliteNumber = 380),
        BibleBook("Jonah", 4, Testament.OLD, "Jon", 32, sqliteNumber = 390),
        BibleBook("Micah", 7, Testament.OLD, "Mic", 33, sqliteNumber = 400),
        BibleBook("Nahum", 3, Testament.OLD, "Nah", 34, sqliteNumber = 410),
        BibleBook("Habakkuk", 3, Testament.OLD, "Hab", 35, sqliteNumber = 420),
        BibleBook("Zephaniah", 3, Testament.OLD, "Zeph", 36, sqliteNumber = 430),
        BibleBook("Haggai", 2, Testament.OLD, "Hag", 37, sqliteNumber = 440),
        BibleBook("Zechariah", 14, Testament.OLD, "Zech", 38, sqliteNumber = 450),
        BibleBook("Malachi", 4, Testament.OLD, "Mal", 39, sqliteNumber = 460)
    )

    val newTestamentBooks = listOf(
        BibleBook("Matthew", 28, Testament.NEW, "Matt", 40, sqliteNumber = 470),
        BibleBook("Mark", 16, Testament.NEW, "Mark", 41, sqliteNumber = 480),
        BibleBook("Luke", 24, Testament.NEW, "Luke", 42, sqliteNumber = 490),
        BibleBook("John", 21, Testament.NEW, "John", 43, sqliteNumber = 500),
        BibleBook("Acts", 28, Testament.NEW, "Acts", 44, sqliteNumber = 510),
        BibleBook("Romans", 16, Testament.NEW, "Rom", 45, sqliteNumber = 520),
        BibleBook("1 Corinthians", 16, Testament.NEW, "1Cor", 46, sqliteNumber = 530),
        BibleBook("2 Corinthians", 13, Testament.NEW, "2Cor", 47, sqliteNumber = 540),
        BibleBook("Galatians", 6, Testament.NEW, "Gal", 48, sqliteNumber = 550),
        BibleBook("Ephesians", 6, Testament.NEW, "Eph", 49, sqliteNumber = 560),
        BibleBook("Philippians", 4, Testament.NEW, "Phil", 50, sqliteNumber = 570),
        BibleBook("Colossians", 4, Testament.NEW, "Col", 51, sqliteNumber = 580),
        BibleBook("1 Thessalonians", 5, Testament.NEW, "1Thes", 52, sqliteNumber = 590),
        BibleBook("2 Thessalonians", 3, Testament.NEW, "2Thes", 53, sqliteNumber = 600),
        BibleBook("1 Timothy", 6, Testament.NEW, "1Tim", 54, sqliteNumber = 610),
        BibleBook("2 Timothy", 4, Testament.NEW, "2Tim", 55, sqliteNumber = 620),
        BibleBook("Titus", 3, Testament.NEW, "Titus", 56, sqliteNumber = 630),
        BibleBook("Philemon", 1, Testament.NEW, "Phlm", 57, sqliteNumber = 640),
        BibleBook("Hebrews", 13, Testament.NEW, "Heb", 58, sqliteNumber = 650),
        BibleBook("James", 5, Testament.NEW, "James", 59, sqliteNumber = 660),
        BibleBook("1 Peter", 5, Testament.NEW, "1Pet", 60, sqliteNumber = 670),
        BibleBook("2 Peter", 3, Testament.NEW, "2Pet", 61, sqliteNumber = 680),
        BibleBook("1 John", 5, Testament.NEW, "1John", 62, sqliteNumber = 690),
        BibleBook("2 John", 1, Testament.NEW, "2John", 63, sqliteNumber = 700),
        BibleBook("3 John", 1, Testament.NEW, "3John", 64, sqliteNumber = 710),
        BibleBook("Jude", 1, Testament.NEW, "Jude", 65, sqliteNumber = 720),
        BibleBook("Revelation", 22, Testament.NEW, "Rev", 66, sqliteNumber = 730)
    )

    val allBooks = oldTestamentBooks + newTestamentBooks

    fun getBookByNumber(number: Int): BibleBook? {
        return allBooks.find { it.number == number }
    }

    // New method to get book by SQLite number
    fun getBookBySqliteNumber(sqliteNumber: Int): BibleBook? {
        val standardNumber = sqliteToStandardMap[sqliteNumber]
        return if (standardNumber != null) {
            getBookByNumber(standardNumber)
        } else {
            null
        }
    }

    // New method to get SQLite number from standard number
    fun getSqliteNumber(standardNumber: Int): Int? {
        return standardToSqliteMap[standardNumber]
    }

    // Get SQLite number from BibleBook
    fun getSqliteNumber(book: BibleBook): Int? {
        return book.sqliteNumber
    }
}