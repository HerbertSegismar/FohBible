package com.example.fohbible.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class DatabaseHelper(private val context: Context, val databaseName: String) {
    var database: SQLiteDatabase? = null
    private val tag = "DatabaseHelper"
    private val random = Random()

    companion object {
        private const val VERSES_TABLE = "verses"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_BOOK_NUMBER = "book_number"
        private const val COLUMN_CHAPTER = "chapter"
        private const val COLUMN_VERSE = "verse"
        const val BOOKMARKS_TABLE = "bookmarks"
        const val COLUMN_BOOK_NAME = "book_name"
        const val COLUMN_VERSE_NUMBER = "verse_number"
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
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun copyDatabaseFromAssets(dbFile: File) {
        try {
            dbFile.parentFile?.mkdirs()
            val assetPath = if (databaseName.endsWith(".dictionary.sqlite3")) "dictionaries/$databaseName" else "databases/$databaseName"
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getVerseCount(bookNumber: Int, chapter: Int): Int {
        var count = 0
        try {
            val query = """
                SELECT COUNT(*) FROM $VERSES_TABLE WHERE $COLUMN_BOOK_NUMBER = ? AND $COLUMN_CHAPTER = ?
            """.trimIndent()
            val cursor = database?.rawQuery(query, arrayOf(bookNumber.toString(), chapter.toString()))
            cursor?.use {
                if (it.moveToFirst()) {
                    count = it.getInt(0)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in getVerseCount: ${e.message}")
            e.printStackTrace()
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
                    } catch (e: Exception) {
                        Log.e(tag, "Error reading verse: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in getVerses: ${e.message}")
            e.printStackTrace()
        }
        return verses
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
                SELECT $COLUMN_VERSE, $COLUMN_TEXT FROM $VERSES_TABLE 
                WHERE $COLUMN_BOOK_NUMBER = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_VERSE >= ? AND $COLUMN_VERSE < ? + ? 
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
                    } catch (e: Exception) {
                        Log.e(tag, "Error reading verse: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in getRandomVerses: ${e.message}")
            e.printStackTrace()
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
        } catch (e: Exception) {
            Log.e(tag, "Error adding bookmark: ${e.message}")
        }
    }

    fun removeBookmark(verse: Verse) {
        try {
            database?.delete(
                BOOKMARKS_TABLE,
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_VERSE_NUMBER = ?",
                arrayOf(verse.bookName, verse.chapter.toString(), verse.verseNumber.toString())
            )
        } catch (e: Exception) {
            Log.e(tag, "Error removing bookmark: ${e.message}")
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
        } catch (e: Exception) {
            Log.e(tag, "Error checking bookmark: ${e.message}")
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
        } catch (e: Exception) {
            Log.e(tag, "Error getting bookmarks: ${e.message}")
        }
        return verses
    }

    fun getWordDefinition(word: String): String? {
        var definition: String? = null
        try {
            val cursor: Cursor? = database?.rawQuery(
                "SELECT definition FROM dictionary WHERE topic = ?",
                arrayOf(word)
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    definition = it.getString(it.getColumnIndexOrThrow("definition"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching word definition: ${e.message}")
        }
        return definition
    }

    fun close() {
        database?.close()
        Log.d(tag, "Database closed")
    }
}