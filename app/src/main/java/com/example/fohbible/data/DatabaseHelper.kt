package com.example.fohbible.data

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.fohbible.MainActivity
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class DatabaseHelper(private val context: MainActivity) {
    private var database: SQLiteDatabase? = null
    private val tag = "DatabaseHelper"
    private val random = Random()

    companion object {
        private const val DATABASE_NAME = "kj2.sqlite3"
        private const val VERSES_TABLE = "verses"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_BOOK_NUMBER = "book_number"
        private const val COLUMN_CHAPTER = "chapter"
        private const val COLUMN_VERSE = "verse"
    }

    init {
        openDatabase()
    }

    private fun openDatabase() {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)

            if (!dbFile.exists()) {
                copyDatabaseFromAssets(dbFile)
            } else {
                Log.d(tag, "Database exists at: ${dbFile.absolutePath}")
            }

            database = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            Log.d(tag, "Database opened successfully")

        } catch (e: Exception) {
            Log.e(tag, "Error opening database: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun copyDatabaseFromAssets(dbFile: File) {
        try {
            dbFile.parentFile?.mkdirs()

            context.assets.open("databases/$DATABASE_NAME").use { inputStream ->
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
                SELECT COUNT(*) 
                FROM $VERSES_TABLE 
                WHERE $COLUMN_BOOK_NUMBER = ? 
                AND $COLUMN_CHAPTER = ?
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
                null, null,
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

            if (verses.isEmpty()) {
                Log.w(tag, "No verses found for book $bookNumber, chapter $chapter")
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

            // Step 1: Get a random book from BibleData
            val allBooks = com.example.fohbible.BibleData.allBooks
            if (allBooks.isEmpty()) {
                Log.e(tag, "No books found in BibleData")
                return verses
            }

            // Select a random book
            val randomBook = allBooks[random.nextInt(allBooks.size)]

            // Step 2: Get a random chapter from that book
            val randomChapter = random.nextInt(randomBook.chapters) + 1

            // Step 3: Get the number of verses in that chapter
            val verseCount = getVerseCount(randomBook.number, randomChapter)
            if (verseCount == 0) {
                Log.w(tag, "No verses found for ${randomBook.name} chapter $randomChapter")
                // Try again with a different book/chapter
                return getRandomVerses()
            }

            // Step 4: Determine how many verses to get (1-5, but not more than available)
            val numberOfVerses = minOf(random.nextInt(5) + 1, verseCount)

            // Step 5: Get a random starting verse
            val startVerse = random.nextInt(verseCount - numberOfVerses + 1) + 1

            // Step 6: Query the verses
            val query = """
                SELECT $COLUMN_VERSE, $COLUMN_TEXT 
                FROM $VERSES_TABLE 
                WHERE $COLUMN_BOOK_NUMBER = ? 
                AND $COLUMN_CHAPTER = ? 
                AND $COLUMN_VERSE >= ? 
                AND $COLUMN_VERSE < ? + ?
                ORDER BY $COLUMN_VERSE ASC
            """.trimIndent()

            val cursor = database?.rawQuery(
                query,
                arrayOf(
                    randomBook.number.toString(),
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

            if (verses.isEmpty()) {
                Log.w(tag, "No verses retrieved for ${randomBook.name} $randomChapter:$startVerse-$numberOfVerses")
            } else {
                Log.d(tag, "Retrieved ${verses.size} random verses from ${randomBook.name} $randomChapter")
            }

        } catch (e: Exception) {
            Log.e(tag, "Error in getRandomVerses: ${e.message}")
            e.printStackTrace()
        }

        return verses
    }

    fun close() {
        database?.close()
        Log.d(tag, "Database closed")
    }
}