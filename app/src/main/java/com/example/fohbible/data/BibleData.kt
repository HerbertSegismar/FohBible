package com.example.fohbible.data

import android.content.Context
import com.example.fohbible.MainActivity
import com.example.fohbible.data.BibleData.BIBLE_BOOKS_MAP

// Data classes and enums from BibleData.kt
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

enum class Testament { OLD, NEW }

// Utility functions from testamentUtils
fun getBookInfo(bookNumber: Int): BibleBook? {
    return BIBLE_BOOKS_MAP[bookNumber]
}

// Search Scope types
typealias SearchScope = String

// Search scope constants
const val SCOPE_WHOLE = "whole"
const val SCOPE_OLD_TESTAMENT = "old-testament"
const val SCOPE_NEW_TESTAMENT = "new-testament"
const val SCOPE_LAW = "law"
const val SCOPE_HISTORICAL = "historical"
const val SCOPE_POETIC = "poetic"
const val SCOPE_MAJOR_PROPHETS = "major-prophets"
const val SCOPE_MINOR_PROPHETS = "minor-prophets"
const val SCOPE_GOSPELS = "gospels"
const val SCOPE_HISTORICAL_NT = "historical-nt"
const val SCOPE_PAULINE_LETTERS = "pauline-letters"
const val SCOPE_LETTERS = "letters"
const val SCOPE_VISION = "vision"

fun createBookScope(bookNumber: Int): SearchScope = "book-$bookNumber"

// Scope ranges
data class ScopeRange(val start: Int, val end: Int)

val SCOPE_RANGES: Map<String, ScopeRange?> = mapOf(
    SCOPE_WHOLE to null,
    SCOPE_OLD_TESTAMENT to ScopeRange(10, 460),
    SCOPE_NEW_TESTAMENT to ScopeRange(470, 730),
    SCOPE_LAW to ScopeRange(10, 50),
    SCOPE_HISTORICAL to ScopeRange(60, 190),
    SCOPE_POETIC to ScopeRange(220, 260),
    SCOPE_MAJOR_PROPHETS to ScopeRange(290, 340),
    SCOPE_MINOR_PROPHETS to ScopeRange(350, 460),
    SCOPE_GOSPELS to ScopeRange(470, 500),
    SCOPE_HISTORICAL_NT to ScopeRange(510, 510),
    SCOPE_PAULINE_LETTERS to ScopeRange(520, 640),
    SCOPE_LETTERS to ScopeRange(520, 720),
    SCOPE_VISION to ScopeRange(730, 730)
)

// Scope configuration
data class ScopeConfig(
    val label: String,
    val description: String,
    val category: String
)

val scopeColors = mapOf(
    SCOPE_LAW to "#e88054",
    SCOPE_HISTORICAL to "#548fe8",
    SCOPE_POETIC to "#E3DA57",
    SCOPE_MAJOR_PROPHETS to "#6DF3CE",
    SCOPE_MINOR_PROPHETS to "#fa6e6e",
    SCOPE_GOSPELS to "#45F34A",
    SCOPE_HISTORICAL_NT to "#b17df5",
    SCOPE_PAULINE_LETTERS to "#f5ab7d",
    SCOPE_LETTERS to "#46E0F3",
    SCOPE_VISION to "#F3EA92"
)

val SCOPE_CONFIG: Map<String, ScopeConfig> = mapOf(
    SCOPE_WHOLE to ScopeConfig(
        label = "Whole Bible",
        description = "Search all books (Genesis - Revelation)",
        category = "All"
    ),
    SCOPE_OLD_TESTAMENT to ScopeConfig(
        label = "Old Testament",
        description = "Genesis - Malachi",
        category = "Old Testament"
    ),
    SCOPE_NEW_TESTAMENT to ScopeConfig(
        label = "New Testament",
        description = "Matthew - Revelation",
        category = "New Testament"
    ),
    SCOPE_LAW to ScopeConfig(
        label = "The Law",
        description = "Genesis, Exodus, Leviticus, Numbers, Deuteronomy",
        category = "Old Testament"
    ),
    SCOPE_HISTORICAL to ScopeConfig(
        label = "Historical Books",
        description = "Joshua, Judges, Ruth, Samuel, Kings, Chronicles, Ezra, Nehemiah, Esther",
        category = "Old Testament"
    ),
    SCOPE_POETIC to ScopeConfig(
        label = "Poetic Books",
        description = "Job, Psalms, Proverbs, Ecclesiastes, Song of Solomon",
        category = "Old Testament"
    ),
    SCOPE_MAJOR_PROPHETS to ScopeConfig(
        label = "Major Prophets",
        description = "Isaiah, Jeremiah, Lamentations, Ezekiel, Daniel",
        category = "Old Testament"
    ),
    SCOPE_MINOR_PROPHETS to ScopeConfig(
        label = "Minor Prophets",
        description = "Hosea, Joel, Amos, Obadiah, Jonah, Micah, Nahum, Habakkuk, Zephaniah, Haggai, Zechariah, Malachi",
        category = "Old Testament"
    ),
    SCOPE_GOSPELS to ScopeConfig(
        label = "The Gospels",
        description = "Matthew, Mark, Luke, John",
        category = "New Testament"
    ),
    SCOPE_HISTORICAL_NT to ScopeConfig(
        label = "Historical Book",
        description = "Acts",
        category = "New Testament"
    ),
    SCOPE_PAULINE_LETTERS to ScopeConfig(
        label = "Pauline Letters",
        description = "Romans, 1 & 2 Corinthians, Galatians, Ephesians, Philippians, Colossians, 1 & 2 Thessalonians, 1 & 2 Timothy, Titus, Philemon",
        category = "New Testament"
    ),
    SCOPE_LETTERS to ScopeConfig(
        label = "The Letters",
        description = "Romans to Jude",
        category = "New Testament"
    ),
    SCOPE_VISION to ScopeConfig(
        label = "The Book of Vision",
        description = "Revelation",
        category = "New Testament"
    )
)

// Utility functions
fun isBookScope(scope: SearchScope): Boolean {
    return scope.startsWith("book-")
}

fun getBookNumberFromScope(scope: SearchScope): Int? {
    if (isBookScope(scope)) {
        return scope.removePrefix("book-").toIntOrNull()
    }
    return null
}

fun getScopeConfig(scope: SearchScope): ScopeConfig {
    if (isBookScope(scope)) {
        val bookNumber = getBookNumberFromScope(scope)
        if (bookNumber != null) {
            val bookInfo = getBookInfo(bookNumber)
            if (bookInfo != null) {
                return ScopeConfig(
                    label = bookInfo.name,
                    description = "Search only ${bookInfo.name}",
                    category = "Individual Books"
                )
            }
        }
        return ScopeConfig(
            label = "Unknown Book",
            description = "Search this book",
            category = "Individual Books"
        )
    }
    return SCOPE_CONFIG[scope] ?: ScopeConfig("Unknown", "Unknown scope", "Unknown")
}

// Individual book scopes
val INDIVIDUAL_BOOK_SCOPES: List<SearchScope> = BIBLE_BOOKS_MAP.keys.map { bookNumber ->
    createBookScope(bookNumber)
}

// Scope categories
val SCOPE_CATEGORIES: Map<String, List<SearchScope>> = mapOf(
    "All" to listOf(SCOPE_WHOLE),
    "Old Testament" to listOf(
        SCOPE_OLD_TESTAMENT,
        SCOPE_LAW,
        SCOPE_HISTORICAL,
        SCOPE_POETIC,
        SCOPE_MAJOR_PROPHETS,
        SCOPE_MINOR_PROPHETS
    ),
    "New Testament" to listOf(
        SCOPE_NEW_TESTAMENT,
        SCOPE_GOSPELS,
        SCOPE_HISTORICAL_NT,
        SCOPE_PAULINE_LETTERS,
        SCOPE_LETTERS,
        SCOPE_VISION
    ),
    "Individual Books" to INDIVIDUAL_BOOK_SCOPES
)

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