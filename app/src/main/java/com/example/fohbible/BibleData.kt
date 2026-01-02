package com.example.fohbible

import android.content.Context

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
            val dbHelper = com.example.fohbible.data.DatabaseHelper(context as MainActivity)
            dbHelper.getVerseCount(customNumber, chapter)
        } else {
            30
        }
    }
}

enum class Testament {
    OLD, NEW
}

object BibleData {
    private val allBooksList = listOf(
        BibleBook(10, "Genesis", 50, Testament.OLD, "Gen"),
        BibleBook(20, "Exodus", 40, Testament.OLD, "Exo"),
        BibleBook(30, "Leviticus", 27, Testament.OLD, "Lev"),
        BibleBook(40, "Numbers", 36, Testament.OLD, "Num"),
        BibleBook(50, "Deuteronomy", 34, Testament.OLD, "Deu"),
        BibleBook(60, "Joshua", 24, Testament.OLD, "Josh"),
        BibleBook(70, "Judges", 21, Testament.OLD, "Judg"),
        BibleBook(80, "Ruth", 4, Testament.OLD, "Ruth"),
        BibleBook(90, "1 Samuel", 31, Testament.OLD, "1Sam"),
        BibleBook(100, "2 Samuel", 24, Testament.OLD, "2Sam"),
        BibleBook(110, "1 Kings", 22, Testament.OLD, "1King"),
        BibleBook(120, "2 Kings", 25, Testament.OLD, "2King"),
        BibleBook(130, "1 Chronicles", 29, Testament.OLD, "1Chr"),
        BibleBook(140, "2 Chronicles", 36, Testament.OLD, "2Chr"),
        BibleBook(150, "Ezra", 10, Testament.OLD, "Ezra"),
        BibleBook(160, "Nehemiah", 13, Testament.OLD, "Neh"),
        BibleBook(190, "Esther", 10, Testament.OLD, "Esth"),
        BibleBook(220, "Job", 42, Testament.OLD, "Job"),
        BibleBook(230, "Psalms", 150, Testament.OLD, "Psalm"),
        BibleBook(240, "Proverbs", 31, Testament.OLD, "Prov"),
        BibleBook(250, "Ecclesiastes", 12, Testament.OLD, "Eccl"),
        BibleBook(260, "Song of Solomon", 8, Testament.OLD, "Song"),
        BibleBook(290, "Isaiah", 66, Testament.OLD, "Isa"),
        BibleBook(300, "Jeremiah", 52, Testament.OLD, "Jer"),
        BibleBook(310, "Lamentations", 5, Testament.OLD, "Lam"),
        BibleBook(330, "Ezekiel", 48, Testament.OLD, "Ezek"),
        BibleBook(340, "Daniel", 12, Testament.OLD, "Dan"),
        BibleBook(350, "Hosea", 14, Testament.OLD, "Hos"),
        BibleBook(360, "Joel", 3, Testament.OLD, "Joel"),
        BibleBook(370, "Amos", 9, Testament.OLD, "Amos"),
        BibleBook(380, "Obadiah", 1, Testament.OLD, "Obad"),
        BibleBook(390, "Jonah", 4, Testament.OLD, "Jonah"),
        BibleBook(400, "Micah", 7, Testament.OLD, "Mic"),
        BibleBook(410, "Nahum", 3, Testament.OLD, "Nah"),
        BibleBook(420, "Habakkuk", 3, Testament.OLD, "Hab"),
        BibleBook(430, "Zephaniah", 3, Testament.OLD, "Zeph"),
        BibleBook(440, "Haggai", 2, Testament.OLD, "Hag"),
        BibleBook(450, "Zechariah", 14, Testament.OLD, "Zech"),
        BibleBook(460, "Malachi", 4, Testament.OLD, "Mal"),

        BibleBook(470, "Matthew", 28, Testament.NEW, "Matt"),
        BibleBook(480, "Mark", 16, Testament.NEW, "Mark"),
        BibleBook(490, "Luke", 24, Testament.NEW, "Luke"),
        BibleBook(500, "John", 21, Testament.NEW, "John"),
        BibleBook(510, "Acts", 28, Testament.NEW, "Acts"),
        BibleBook(520, "Romans", 16, Testament.NEW, "Rom"),
        BibleBook(530, "1 Corinthians", 16, Testament.NEW, "1Cor"),
        BibleBook(540, "2 Corinthians", 13, Testament.NEW, "2Cor"),
        BibleBook(550, "Galatians", 6, Testament.NEW, "Gal"),
        BibleBook(560, "Ephesians", 6, Testament.NEW, "Eph"),
        BibleBook(570, "Philippians", 4, Testament.NEW, "Phil"),
        BibleBook(580, "Colossians", 4, Testament.NEW, "Col"),
        BibleBook(590, "1 Thessalonians", 5, Testament.NEW, "1Thes"),
        BibleBook(600, "2 Thessalonians", 3, Testament.NEW, "2Thes"),
        BibleBook(610, "1 Timothy", 6, Testament.NEW, "1Tim"),
        BibleBook(620, "2 Timothy", 4, Testament.NEW, "2Tim"),
        BibleBook(630, "Titus", 3, Testament.NEW, "Titus"),
        BibleBook(640, "Philemon", 1, Testament.NEW, "Phlm"),
        BibleBook(650, "Hebrews", 13, Testament.NEW, "Heb"),
        BibleBook(660, "James", 5, Testament.NEW, "James"),
        BibleBook(670, "1 Peter", 5, Testament.NEW, "1Pet"),
        BibleBook(680, "2 Peter", 3, Testament.NEW, "2Pet"),
        BibleBook(690, "1 John", 5, Testament.NEW, "1John"),
        BibleBook(700, "2 John", 1, Testament.NEW, "2John"),
        BibleBook(710, "3 John", 1, Testament.NEW, "3John"),
        BibleBook(720, "Jude", 1, Testament.NEW, "Jude"),
        BibleBook(730, "Revelation", 22, Testament.NEW, "Rev")
    )

    private val booksWithStandardNumbers = allBooksList.mapIndexed { index, book ->
        book.copy(standardNumber = index + 1)
    }

    val BIBLE_BOOKS_MAP = booksWithStandardNumbers.associateBy { it.customNumber }

    val allBooks: List<BibleBook> = booksWithStandardNumbers
    val oldTestamentBooks: List<BibleBook> = allBooks.filter { it.testament == Testament.OLD }
    val newTestamentBooks: List<BibleBook> = allBooks.filter { it.testament == Testament.NEW }

    fun getBookByCustomNumber(customNumber: Int): BibleBook? = BIBLE_BOOKS_MAP[customNumber]

}