package com.example.fohbible.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) {
    private var database: SQLiteDatabase? = null
    private val TAG = "DatabaseHelper"

    companion object {
        private const val DATABASE_NAME = "kj2.sqlite3"
        private const val VERSES_TABLE = "verses"

        // Update column names based on your actual database schema
        private const val COLUMN_TEXT = "text"  // This might be "verse_text" or something else
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
                Log.d(TAG, "Database not found, copying from assets...")
                copyDatabaseFromAssets(dbFile)
            } else {
                Log.d(TAG, "Database exists at: ${dbFile.absolutePath}")
            }

            database = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            Log.d(TAG, "Database opened successfully")

            // Test the connection
            testDatabaseConnection()

        } catch (e: Exception) {
            Log.e(TAG, "Error opening database: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun copyDatabaseFromAssets(dbFile: File) {
        try {
            dbFile.parentFile?.mkdirs()

            // List files in assets/databases
            val assetFiles = context.assets.list("databases")
            Log.d(TAG, "Files in assets/databases: ${assetFiles?.joinToString(", ")}")

            context.assets.open("databases/$DATABASE_NAME").use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Database copied from assets successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying database: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun testDatabaseConnection() {
        try {
            val cursor = database?.rawQuery("SELECT COUNT(*) FROM $VERSES_TABLE", null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val count = it.getInt(0)
                    Log.d(TAG, "Database test: $count verses in table")
                }
            }

            // Get column names
            val tableInfoCursor = database?.rawQuery("PRAGMA table_info($VERSES_TABLE)", null)
            tableInfoCursor?.use {
                Log.d(TAG, "Columns in $VERSES_TABLE:")
                while (it.moveToNext()) {
                    val columnName = it.getString(1)
                    val columnType = it.getString(2)
                    Log.d(TAG, "  $columnName ($columnType)")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error testing database connection: ${e.message}")
        }
    }

    fun getVerses(bookNumber: Int, chapter: Int): List<Verse> {
        val verses = mutableListOf<Verse>()
        Log.d(TAG, "Getting verses for book $bookNumber, chapter $chapter")

        try {
            if (database == null || !database!!.isOpen) {
                Log.e(TAG, "Database is not open!")
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
                Log.d(TAG, "Found ${it.count} verses")

                while (it.moveToNext()) {
                    try {
                        val verseNumber = it.getInt(it.getColumnIndexOrThrow(COLUMN_VERSE))
                        val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                        verses.add(Verse(verseNumber, text))
                        Log.d(TAG, "Verse $verseNumber: ${text.take(50)}...")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading verse: ${e.message}")
                    }
                }
            }

            if (verses.isEmpty()) {
                Log.w(TAG, "No verses found for book $bookNumber, chapter $chapter")

                // Try alternative column names
                tryAlternativeColumnNames(bookNumber, chapter)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in getVerses: ${e.message}")
            e.printStackTrace()
        }

        return verses
    }

    private fun tryAlternativeColumnNames(bookNumber: Int, chapter: Int) {
        Log.d(TAG, "Trying alternative column names...")

        // Common alternative column names
        val possibleTextColumns = listOf("verse_text", "text", "content", "scripture")
        val possibleBookColumns = listOf("book_number", "book", "book_id")
        val possibleVerseColumns = listOf("verse", "verse_number", "verse_num")

        for (bookCol in possibleBookColumns) {
            for (verseCol in possibleVerseColumns) {
                for (textCol in possibleTextColumns) {
                    try {
                        val cursor = database?.rawQuery(
                            "SELECT $verseCol, $textCol FROM $VERSES_TABLE WHERE $bookCol = ? AND chapter = ? ORDER BY $verseCol ASC",
                            arrayOf(bookNumber.toString(), chapter.toString())
                        )

                        cursor?.use {
                            if (it.count > 0) {
                                Log.d(TAG, "Found using columns: $bookCol, $verseCol, $textCol")
                                return
                            }
                        }
                    } catch (e: Exception) {
                        // Continue trying
                    }
                }
            }
        }
    }

    fun getVerse(bookNumber: Int, chapter: Int, verse: Int): Verse? {
        Log.d(TAG, "Getting specific verse: book $bookNumber, chapter $chapter, verse $verse")

        return try {
            database?.let { db ->
                val cursor = db.query(
                    VERSES_TABLE,
                    arrayOf(COLUMN_VERSE, COLUMN_TEXT),
                    "$COLUMN_BOOK_NUMBER = ? AND $COLUMN_CHAPTER = ? AND $COLUMN_VERSE = ?",
                    arrayOf(bookNumber.toString(), chapter.toString(), verse.toString()),
                    null, null, null
                )

                cursor.use {
                    if (it.moveToFirst()) {
                        val verseNumber = it.getInt(it.getColumnIndexOrThrow(COLUMN_VERSE))
                        val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                        Verse(verseNumber, text)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getVerse: ${e.message}")
            null
        }
    }

    fun close() {
        database?.close()
        Log.d(TAG, "Database closed")
    }
}