package com.example.fohbible.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) {
    private var database: SQLiteDatabase? = null
    private val tag = "DatabaseHelper"

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
                Log.d(tag, "Database not found, copying from assets...")
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

            // List files in assets/databases
            val assetFiles = context.assets.list("databases")
            Log.d(tag, "Files in assets/databases: ${assetFiles?.joinToString(", ")}")

            context.assets.open("databases/$DATABASE_NAME").use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(tag, "Database copied from assets successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error copying database: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getVerseCount(bookNumber: Int, chapter: Int): Int {
        var count = 0
        Log.d(tag, "Getting verse count for book $bookNumber, chapter $chapter")

        try {
            if (database == null || !database!!.isOpen) {
                Log.e(tag, "Database is not open!")
                return 0
            }

            // Simplified query: just count how many times a chapter number appears for a book
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
                    Log.d(tag, "Verse count for book $bookNumber, chapter $chapter: $count")
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
        Log.d(tag, "Getting verses for book $bookNumber, chapter $chapter")

        try {
            if (database == null || !database!!.isOpen) {
                Log.e(tag, "Database is not open!")
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
                Log.d(tag, "Found ${it.count} verses")

                while (it.moveToNext()) {
                    try {
                        val verseNumber = it.getInt(it.getColumnIndexOrThrow(COLUMN_VERSE))
                        val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                        verses.add(Verse(verseNumber, text))
                        Log.d(tag, "Verse $verseNumber: ${text.take(50)}...")
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

    fun close() {
        database?.close()
        Log.d(tag, "Database closed")
    }
}