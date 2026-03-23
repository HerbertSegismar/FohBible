package com.fountofhopedotorg.fohbible.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.Locale.getDefault
import java.util.Random

class DatabaseHelper(private val context: Context, val databaseName: String) {
    var database: SQLiteDatabase? = null
    private val random = Random()

    companion object {
        private const val VERSES_TABLE = "verses"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_BOOK_NUMBER = "book_number"
        private const val COLUMN_CHAPTER = "chapter"
        private const val COLUMN_VERSE = "verse"
        const val BOOKMARKS_TABLE = "bookmarks"
        const val HIGHLIGHTS_TABLE = "highlights"
        const val NOTES_TABLE = "notes"
        const val COLUMN_BOOK_NAME = "book_name"
        const val COLUMN_VERSE_NUMBER = "verse_number"
        const val COLUMN_START_VERSE = "start_verse"
        const val COLUMN_END_VERSE = "end_verse"
        const val COLUMN_NOTE = "note"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    init {
        openDatabase()
    }

    private fun openDatabase() {
        try {
            val dbFile = context.getDatabasePath(databaseName)
            if (!dbFile.exists()) {
                copyDatabaseFromAssets(dbFile)
            }
            database = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            createBookmarksTable()
            createHighlightsTable()
            createNotesTable()
        } catch (_: Exception) {
        }
    }

    fun refreshDatabase(): Boolean {
        return try {
            database?.close()
            val dbFile = context.getDatabasePath(databaseName)
            if (dbFile.exists()) {
                dbFile.delete()
            }
            Thread.sleep(100)
            copyDatabaseFromAssets(dbFile)
            database = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            createBookmarksTable()
            createHighlightsTable()
            createNotesTable()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun createBookmarksTable() {
        database?.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $BOOKMARKS_TABLE (
                $COLUMN_BOOK_NAME TEXT,
                $COLUMN_CHAPTER INTEGER,
                $COLUMN_VERSE_NUMBER INTEGER,
                $COLUMN_TEXT TEXT,
                PRIMARY KEY ($COLUMN_BOOK_NAME, $COLUMN_CHAPTER, $COLUMN_VERSE_NUMBER)
            )
            """.trimIndent()
        )
    }

    private fun createHighlightsTable() {
        database?.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $HIGHLIGHTS_TABLE (
                $COLUMN_BOOK_NAME TEXT,
                $COLUMN_CHAPTER INTEGER,
                $COLUMN_VERSE_NUMBER INTEGER,
                $COLUMN_TEXT TEXT,
                PRIMARY KEY ($COLUMN_BOOK_NAME, $COLUMN_CHAPTER, $COLUMN_VERSE_NUMBER)
            )
            """.trimIndent()
        )
    }
    private fun createNotesTable() {
        database?.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $NOTES_TABLE (
                $COLUMN_BOOK_NAME TEXT,
                $COLUMN_CHAPTER INTEGER,
                $COLUMN_START_VERSE INTEGER,
                $COLUMN_END_VERSE INTEGER,
                $COLUMN_NOTE TEXT,
                $COLUMN_TIMESTAMP INTEGER DEFAULT (strftime('%s','now')),
                PRIMARY KEY ($COLUMN_BOOK_NAME, $COLUMN_CHAPTER, $COLUMN_START_VERSE, $COLUMN_END_VERSE)
            )
            """.trimIndent()
        )
    }

    private fun copyDatabaseFromAssets(dbFile: File) {
        try {
            dbFile.parentFile?.mkdirs()
            val assetPath = when {
                databaseName.endsWith("dictionary.sqlite3") -> "dictionaries/$databaseName"
                databaseName.endsWith("kjvsubheadings.sqlite3") -> "subheadings/$databaseName"
                databaseName.endsWith("crossreferences.sqlite3") -> "cross-references/$databaseName"
                databaseName.endsWith("commentaries.sqlite3") -> "commentaries/$databaseName"
                else -> "databases/$databaseName"
            }
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (_: Exception) {}
    }

    fun getCrossReferenceCountsForChapter(book: Int, chapter: Int?): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        try {
            database?.rawQuery(
                "SELECT verse, COUNT(*) FROM cross_references WHERE book = ? AND chapter = ? GROUP BY verse",
                arrayOf(book.toString(), chapter.toString())
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val verse = cursor.getInt(0)
                    val count = cursor.getInt(1)
                    map[verse] = count
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    fun getCrossReferences(book: Int, chapter: Int, verse: Int): List<CrossReference> {
        val refs = mutableListOf<CrossReference>()
        try {
            database?.query(
                "cross_references",
                arrayOf("book_to", "chapter_to", "verse_to_start", "verse_to_end"),
                "book = ? AND chapter = ? AND verse = ?",
                arrayOf(book.toString(), chapter.toString(), verse.toString()),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val verseToStart = cursor.getInt(cursor.getColumnIndexOrThrow("verse_to_start"))
                    var verseToEnd = cursor.getInt(cursor.getColumnIndexOrThrow("verse_to_end"))
                    if (verseToEnd == 0) {
                        verseToEnd = verseToStart
                    }
                    refs.add(
                        CrossReference(
                            bookTo = cursor.getInt(cursor.getColumnIndexOrThrow("book_to")),
                            chapterTo = cursor.getInt(cursor.getColumnIndexOrThrow("chapter_to")),
                            verseToStart = verseToStart,
                            verseToEnd = verseToEnd
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return refs
    }

    fun getCommentariesForVerse(bookNumber: Int, chapter: Int, verse: Int): List<VerseCommentary> {
        val commentaries = mutableListOf<VerseCommentary>()
        try {
            val cursor = database?.rawQuery(
                """
            SELECT text, chapter_number_from, verse_number_from, chapter_number_to, verse_number_to 
            FROM commentaries 
            WHERE book_number = ? 
            AND chapter_number_from <= ? 
            AND (chapter_number_to IS NULL OR chapter_number_to >= ?)
            """.trimIndent(),
                arrayOf(bookNumber.toString(), chapter.toString(), chapter.toString())
            )

            cursor?.use { it ->
                val textIdx = it.getColumnIndexOrThrow("text")
                val chFromIdx = it.getColumnIndexOrThrow("chapter_number_from")
                val vFromIdx = it.getColumnIndexOrThrow("verse_number_from")
                val chToIdx = it.getColumnIndexOrThrow("chapter_number_to")
                val vToIdx = it.getColumnIndexOrThrow("verse_number_to")

                while (it.moveToNext()) {
                    val chapterFrom = it.getInt(chFromIdx)
                    val safeChapterFrom = if (chapterFrom <= 0) 1 else chapterFrom

                    val verseFrom = if (it.isNull(vFromIdx) || it.getInt(vFromIdx) <= 0) 1 else it.getInt(vFromIdx)
                    val chapterToRaw = if (it.isNull(chToIdx)) null else it.getInt(chToIdx)
                    val verseToRaw = if (it.isNull(vToIdx)) null else it.getInt(vToIdx)

                    val chapterTo = if (chapterToRaw != null && chapterToRaw <= 0) null else chapterToRaw
                    val verseTo = if (verseToRaw != null && verseToRaw <= 0) null else verseToRaw
                    if (chapterTo == null && verseTo == null) {
                        if (chapter == safeChapterFrom && verse == verseFrom) {
                            var text = it.getString(textIdx)
                            text = text.replace(Regex("<script[\\s\\S]*?</script>"), "")
                            commentaries.add(VerseCommentary(text, safeChapterFrom, verseFrom, null, null))
                        }
                        continue
                    }
                    val inChapterRange = chapter >= safeChapterFrom && (chapterTo?.let { chapter <= it } ?: true)
                    if (!inChapterRange) continue

                    val lowerMet = chapter != safeChapterFrom || verse >= verseFrom
                    val upperMet = chapterTo == null || chapter != chapterTo || (verseTo?.let { verse <= it } ?: true)

                    if (lowerMet && upperMet) {
                        var text = it.getString(textIdx)
                        text = text.replace(Regex("<script[\\s\\S]*?</script>"), "")
                        commentaries.add(VerseCommentary(text, safeChapterFrom, verseFrom, chapterTo, verseTo))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return commentaries
    }

    fun getVerseCount(bookNumber: Int, chapter: Int): Int {
        var count = 0
        try {
            val query = """
                SELECT COUNT(*) FROM $VERSES_TABLE 
                WHERE $COLUMN_BOOK_NUMBER = ? AND $COLUMN_CHAPTER = ?
            """.trimIndent()
            val cursor = database?.rawQuery(query, arrayOf(bookNumber.toString(), chapter.toString()))
            cursor?.use {
                if (it.moveToFirst()) {
                    count = it.getInt(0)
                }
            }
        } catch (_: Exception) {
        }
        return count
    }

    fun getVerses(bookNumber: Int, chapter: Int): List<Verse> {
        val verses = mutableListOf<Verse>()
        try {
            if (database == null || !database!!.isOpen) {
                return verses
            }
            val cursor = database?.query(
                VERSES_TABLE,
                arrayOf(COLUMN_VERSE, COLUMN_TEXT),
                "$COLUMN_BOOK_NUMBER = ? AND $COLUMN_CHAPTER = ?",
                arrayOf(bookNumber.toString(), chapter.toString()),
                null,
                null,
                "$COLUMN_VERSE ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    try {
                        val verseNumber = it.getInt(it.getColumnIndexOrThrow(COLUMN_VERSE))
                        val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                        verses.add(Verse(verseNumber, text))
                    } catch (_: Exception) {
                    }
                }
            }
        }catch (_: Exception) {
        }
        return verses
    }

    fun getSubheadings(bookNumber: Int, chapter: Int): List<Subheading> {
        val subheadings = mutableListOf<Subheading>()
        try {
            if (database == null || !database!!.isOpen) {
                return subheadings
            }
            val query = """
                SELECT verse, subheading FROM subheadings 
                WHERE book_number = ? AND chapter = ? 
                ORDER BY verse ASC
            """.trimIndent()
            val cursor = database?.rawQuery(query, arrayOf(bookNumber.toString(), chapter.toString()))
            cursor?.use {
                while (it.moveToNext()) {
                    try {
                        val verse = it.getInt(it.getColumnIndexOrThrow("verse"))
                        val text = it.getString(it.getColumnIndexOrThrow("subheading"))
                        subheadings.add(Subheading(verse, text))
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
        return subheadings
    }

    fun getRandomVerses(): List<Verse> {
        val verses = mutableListOf<Verse>()
        try {
            if (database == null || !database!!.isOpen) {
                return verses
            }
            val allBooks = BibleData.allBooks
            if (allBooks.isEmpty()) {
                return verses
            }
            val randomBook = allBooks[random.nextInt(allBooks.size)]
            val randomChapter = random.nextInt(randomBook.chapters) + 1
            val verseCount = getVerseCount(randomBook.customNumber, randomChapter)
            if (verseCount == 0) {
                return getRandomVerses()
            }
            val numberOfVerses = minOf(random.nextInt(5) + 1, verseCount)
            val startVerse = random.nextInt(verseCount - numberOfVerses + 1) + 1
            val query = """
                SELECT $COLUMN_VERSE, $COLUMN_TEXT 
                FROM $VERSES_TABLE 
                WHERE $COLUMN_BOOK_NUMBER = ? AND $COLUMN_CHAPTER = ? 
                AND $COLUMN_VERSE >= ? AND $COLUMN_VERSE < ? + ? 
                ORDER BY $COLUMN_VERSE ASC
            """.trimIndent()
            val cursor = database?.rawQuery(
                query,
                arrayOf(
                    randomBook.customNumber.toString(),
                    randomChapter.toString(),
                    startVerse.toString(),
                    startVerse.toString(),
                    numberOfVerses.toString()
                )
            )
            cursor?.use {
                while (it.moveToNext()) {
                    try {
                        val verseNumber = it.getInt(it.getColumnIndexOrThrow(COLUMN_VERSE))
                        val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                        verses.add(Verse(verseNumber, text, randomBook.name, randomChapter))
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
        return verses
    }

    fun addBookmark(verse: Verse) {
        try {
            val values = ContentValues().apply {
                put(COLUMN_BOOK_NAME, verse.bookName)
                put(COLUMN_CHAPTER, verse.chapter)
                put(COLUMN_VERSE_NUMBER, verse.verseNumber)
                put(COLUMN_TEXT, verse.text)
            }
            database?.insertWithOnConflict(BOOKMARKS_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        } catch (_: Exception) {
        }
    }

    fun removeBookmark(verse: Verse) {
        try {
            database?.delete(
                BOOKMARKS_TABLE,
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_VERSE_NUMBER = ?",
                arrayOf(verse.bookName, verse.chapter.toString(), verse.verseNumber.toString())
            )
        } catch (_: Exception) {
        }
    }

    fun isBookmarked(verse: Verse): Boolean {
        var exists = false
        try {
            val cursor = database?.query(
                BOOKMARKS_TABLE,
                arrayOf(COLUMN_VERSE_NUMBER),
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_VERSE_NUMBER = ?",
                arrayOf(verse.bookName, verse.chapter.toString(), verse.verseNumber.toString()),
                null,
                null,
                null
            )
            cursor?.use {
                exists = it.count > 0
            }
        } catch (_: Exception) {
        }
        return exists
    }

    fun getBookmarks(): List<Verse> {
        val verses = mutableListOf<Verse>()
        try {
            val cursor = database?.query(BOOKMARKS_TABLE, null, null, null, null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val bookName = it.getString(it.getColumnIndexOrThrow(COLUMN_BOOK_NAME))
                    val chapter = it.getInt(it.getColumnIndexOrThrow(COLUMN_CHAPTER))
                    val verseNumber = it.getInt(it.getColumnIndexOrThrow(COLUMN_VERSE_NUMBER))
                    val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                    verses.add(Verse(verseNumber, text, bookName, chapter))
                }
            }
        } catch (_: Exception) {
        }
        return verses
    }

    fun addHighlight(verse: Verse) {
        try {
            val values = ContentValues().apply {
                put(COLUMN_BOOK_NAME, verse.bookName)
                put(COLUMN_CHAPTER, verse.chapter)
                put(COLUMN_VERSE_NUMBER, verse.verseNumber)
                put(COLUMN_TEXT, verse.text)
            }
            database?.insertWithOnConflict(HIGHLIGHTS_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        } catch (_: Exception) {
        }
    }

    fun removeHighlight(verse: Verse) {
        try {
            database?.delete(
                HIGHLIGHTS_TABLE,
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_VERSE_NUMBER = ?",
                arrayOf(verse.bookName, verse.chapter.toString(), verse.verseNumber.toString())
            )
        } catch (_: Exception) {
        }
    }

    fun isHighlighted(verse: Verse): Boolean {
        var exists = false
        try {
            val cursor = database?.query(
                HIGHLIGHTS_TABLE,
                arrayOf(COLUMN_VERSE_NUMBER),
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_VERSE_NUMBER = ?",
                arrayOf(verse.bookName, verse.chapter.toString(), verse.verseNumber.toString()),
                null,
                null,
                null
            )
            cursor?.use {
                exists = it.count > 0
            }
        } catch (_: Exception) {
        }
        return exists
    }
    fun addOrUpdateNote(book: String, chapter: Int, startVerse: Int, endVerse: Int, note: String) {
        try {
            val values = ContentValues().apply {
                put(COLUMN_BOOK_NAME, book)
                put(COLUMN_CHAPTER, chapter)
                put(COLUMN_START_VERSE, startVerse)
                put(COLUMN_END_VERSE, endVerse)
                put(COLUMN_NOTE, note)
                put(COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000)
            }
            database?.insertWithOnConflict(
                NOTES_TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        } catch (_: Exception) {
        }
    }

    fun getNote(book: String, chapter: Int, startVerse: Int, endVerse: Int): String? {
        var note: String? = null
        try {
            val cursor = database?.query(
                NOTES_TABLE,
                arrayOf(COLUMN_NOTE),
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_START_VERSE = ? AND $COLUMN_END_VERSE = ?",
                arrayOf(book, chapter.toString(), startVerse.toString(), endVerse.toString()),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    note = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE))
                }
            }
        } catch (_: Exception) {
        }
        return note
    }

    fun deleteNote(book: String, chapter: Int, startVerse: Int, endVerse: Int) {
        try {
            database?.delete(
                NOTES_TABLE,
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_START_VERSE = ? AND $COLUMN_END_VERSE = ?",
                arrayOf(book, chapter.toString(), startVerse.toString(), endVerse.toString())
            )
        } catch (_: Exception) {
        }
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        try {
            database?.query(
                NOTES_TABLE,
                arrayOf(COLUMN_BOOK_NAME, COLUMN_CHAPTER, COLUMN_START_VERSE, COLUMN_END_VERSE, COLUMN_NOTE, COLUMN_TIMESTAMP),
                null, null, null, null, "$COLUMN_TIMESTAMP DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val bookName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOK_NAME))
                    val chapter = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHAPTER))
                    val startVerse = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_START_VERSE))
                    val endVerse = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_END_VERSE))
                    val note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE))
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    notes.add(Note(bookName, chapter, startVerse, endVerse, note, timestamp))
                }
            }
        } catch (_: Exception) {
        }
        return notes
    }
    fun hasNote(verse: Verse): Boolean {
        var exists = false
        try {
            val cursor = database?.query(
                NOTES_TABLE,
                arrayOf(COLUMN_NOTE),
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_START_VERSE <= ? AND $COLUMN_END_VERSE >= ?",
                arrayOf(
                    verse.bookName,
                    verse.chapter.toString(),
                    verse.verseNumber.toString(),
                    verse.verseNumber.toString()
                ),
                null,
                null,
                null
            )
            cursor?.use {
                exists = it.count > 0
            }
        } catch (_: Exception) {
        }
        return exists
    }

    fun getWordDefinition(word: String): String? {
        var definition: String? = null
        try {
            val cursor: Cursor? = database?.rawQuery(
                "SELECT definition FROM dictionary WHERE LOWER(topic) = ?",
                arrayOf(word.lowercase(getDefault()))
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    definition = it.getString(it.getColumnIndexOrThrow("definition"))
                }
            }
        } catch (_: Exception) {
        }
        return definition
    }

    fun getStrongDefinition(word: String): String? {
        var result: String? = null
        try {
            val cursor: Cursor? = database?.rawQuery(
                "SELECT definition FROM dictionary WHERE topic = ?",
                arrayOf(word)
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow("definition"))
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun getCommentary(bookNumber: Int, chapterNumber: Int, verseNumber: Int, marker: String): String? {
        var commentary: String? = null
        try {
            val cursor: Cursor? = database?.rawQuery(
                "SELECT text FROM commentaries WHERE book_number = ? AND chapter_number_from = ? AND verse_number_from = ? AND marker = ?",
                arrayOf(bookNumber.toString(), chapterNumber.toString(), verseNumber.toString(), marker)
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    commentary = it.getString(it.getColumnIndexOrThrow("text"))
                    commentary = commentary?.replace(Regex("<script[\\s\\S]*?</script>"), "")
                }
            }
        } catch (_: Exception) {
        }
        return commentary
    }

    fun close() {
        database?.close()
    }
}

fun getVersesWithSubheadings(
    versesHelper: DatabaseHelper,
    subheadingsHelper: DatabaseHelper,
    bookNumber: Int,
    chapter: Int
): List<VerseContent> {
    val verses = versesHelper.getVerses(bookNumber, chapter)
    val subheadings = subheadingsHelper.getSubheadings(bookNumber, chapter)
    val contentMap: MutableMap<Int, MutableList<VerseContent>> = mutableMapOf()
    verses.forEach { verse ->
        contentMap.getOrPut(verse.verseNumber) { mutableListOf() }.add(VerseContent.VerseVal(verse))
    }
    subheadings.forEach { subheading ->
        contentMap.getOrPut(subheading.verse) { mutableListOf() }.add(0, VerseContent.SubheadingVal(subheading))
    }
    return contentMap.toSortedMap().flatMap { it.value }
}