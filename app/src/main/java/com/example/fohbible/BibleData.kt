package com.example.fohbible

data class BibleBook(
    val name: String,
    val chapters: Int,
    val testament: Testament,
    val abbreviation: String,
    val number: Int
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
    val oldTestamentBooks = listOf(
        BibleBook("Genesis", 50, Testament.OLD, "Gen", 1),
        BibleBook("Exodus", 40, Testament.OLD, "Ex", 2),
        BibleBook("Leviticus", 27, Testament.OLD, "Lev", 3),
        BibleBook("Numbers", 36, Testament.OLD, "Num", 4),
        BibleBook("Deuteronomy", 34, Testament.OLD, "Deut", 5),
        BibleBook("Joshua", 24, Testament.OLD, "Josh", 6),
        BibleBook("Judges", 21, Testament.OLD, "Judg", 7),
        BibleBook("Ruth", 4, Testament.OLD, "Ruth", 8),
        BibleBook("1 Samuel", 31, Testament.OLD, "1 Sam", 9),
        BibleBook("2 Samuel", 24, Testament.OLD, "2 Sam", 10),
        BibleBook("1 Kings", 22, Testament.OLD, "1 Kings", 11),
        BibleBook("2 Kings", 25, Testament.OLD, "2 Kings", 12),
        BibleBook("1 Chronicles", 29, Testament.OLD, "1 Chron", 13),
        BibleBook("2 Chronicles", 36, Testament.OLD, "2 Chron", 14),
        BibleBook("Ezra", 10, Testament.OLD, "Ezra", 15),
        BibleBook("Nehemiah", 13, Testament.OLD, "Neh", 16),
        BibleBook("Esther", 10, Testament.OLD, "Esth", 17),
        BibleBook("Job", 42, Testament.OLD, "Job", 18),
        BibleBook("Psalms", 150, Testament.OLD, "Ps", 19),
        BibleBook("Proverbs", 31, Testament.OLD, "Prov", 20),
        BibleBook("Ecclesiastes", 12, Testament.OLD, "Eccl", 21),
        BibleBook("Song of Solomon", 8, Testament.OLD, "Song", 22),
        BibleBook("Isaiah", 66, Testament.OLD, "Isa", 23),
        BibleBook("Jeremiah", 52, Testament.OLD, "Jer", 24),
        BibleBook("Lamentations", 5, Testament.OLD, "Lam", 25),
        BibleBook("Ezekiel", 48, Testament.OLD, "Ezek", 26),
        BibleBook("Daniel", 12, Testament.OLD, "Dan", 27),
        BibleBook("Hosea", 14, Testament.OLD, "Hos", 28),
        BibleBook("Joel", 3, Testament.OLD, "Joel", 29),
        BibleBook("Amos", 9, Testament.OLD, "Amos", 30),
        BibleBook("Obadiah", 1, Testament.OLD, "Obad", 31),
        BibleBook("Jonah", 4, Testament.OLD, "Jonah", 32),
        BibleBook("Micah", 7, Testament.OLD, "Micah", 33),
        BibleBook("Nahum", 3, Testament.OLD, "Nahum", 34),
        BibleBook("Habakkuk", 3, Testament.OLD, "Hab", 35),
        BibleBook("Zephaniah", 3, Testament.OLD, "Zeph", 36),
        BibleBook("Haggai", 2, Testament.OLD, "Hag", 37),
        BibleBook("Zechariah", 14, Testament.OLD, "Zech", 38),
        BibleBook("Malachi", 4, Testament.OLD, "Mal", 39)
    )

    val newTestamentBooks = listOf(
        BibleBook("Matthew", 28, Testament.NEW, "Matt", 40),
        BibleBook("Mark", 16, Testament.NEW, "Mark", 41),
        BibleBook("Luke", 24, Testament.NEW, "Luke", 42),
        BibleBook("John", 21, Testament.NEW, "John", 43),
        BibleBook("Acts", 28, Testament.NEW, "Acts", 44),
        BibleBook("Romans", 16, Testament.NEW, "Rom", 45),
        BibleBook("1 Corinthians", 16, Testament.NEW, "1 Cor", 46),
        BibleBook("2 Corinthians", 13, Testament.NEW, "2 Cor", 47),
        BibleBook("Galatians", 6, Testament.NEW, "Gal", 48),
        BibleBook("Ephesians", 6, Testament.NEW, "Eph", 49),
        BibleBook("Philippians", 4, Testament.NEW, "Phil", 50),
        BibleBook("Colossians", 4, Testament.NEW, "Col", 51),
        BibleBook("1 Thessalonians", 5, Testament.NEW, "1 Thess", 52),
        BibleBook("2 Thessalonians", 3, Testament.NEW, "2 Thess", 53),
        BibleBook("1 Timothy", 6, Testament.NEW, "1 Tim", 54),
        BibleBook("2 Timothy", 4, Testament.NEW, "2 Tim", 55),
        BibleBook("Titus", 3, Testament.NEW, "Titus", 56),
        BibleBook("Philemon", 1, Testament.NEW, "Philem", 57),
        BibleBook("Hebrews", 13, Testament.NEW, "Heb", 58),
        BibleBook("James", 5, Testament.NEW, "James", 59),
        BibleBook("1 Peter", 5, Testament.NEW, "1 Pet", 60),
        BibleBook("2 Peter", 3, Testament.NEW, "2 Pet", 61),
        BibleBook("1 John", 5, Testament.NEW, "1 John", 62),
        BibleBook("2 John", 1, Testament.NEW, "2 John", 63),
        BibleBook("3 John", 1, Testament.NEW, "3 John", 64),
        BibleBook("Jude", 1, Testament.NEW, "Jude", 65),
        BibleBook("Revelation", 22, Testament.NEW, "Rev", 66)
    )

    val allBooks = oldTestamentBooks + newTestamentBooks

    fun getBookByName(name: String): BibleBook? {
        return allBooks.find { it.name == name }
    }

    fun getBookByNumber(number: Int): BibleBook? {
        return allBooks.find { it.number == number }
    }
}