package com.example.fohbible.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BibleRepository(context: Context) {
    private val databaseHelper = DatabaseHelper(context)

    fun getVerses(bookNumber: Int, chapter: Int): List<Verse> {
        return databaseHelper.getVerses(bookNumber, chapter)
    }

    fun close() {
        databaseHelper.close()
    }
}

class BibleViewModel(private val repository: BibleRepository) : ViewModel() {
    private val _verses = MutableStateFlow<List<Verse>>(emptyList())
    val verses: StateFlow<List<Verse>> = _verses

    fun loadVerses(bookNumber: Int, chapter: Int) {
        viewModelScope.launch {
            val versesList = withContext(Dispatchers.IO) {
                repository.getVerses(bookNumber, chapter)
            }
            _verses.value = versesList
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}