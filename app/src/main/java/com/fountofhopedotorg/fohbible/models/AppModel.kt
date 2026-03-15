package com.fountofhopedotorg.fohbible.models

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AppViewModel : ViewModel() {

    var showSecondaryNavigationModal by mutableStateOf(false)
    var lightModalBackgroundColor by mutableStateOf(Color(0xFFE0E0E0))
    var darkModalBackgroundColor by mutableStateOf(Color(0xFF2D2D2D))
    var fontSize by mutableIntStateOf(18)
    var darkTheme by mutableStateOf(false)
    var selectedColor by mutableStateOf<Color?>(null)
    var isCustomColor by mutableStateOf(false)
    var selectedFontFamily by mutableStateOf("system")
    val navigationStack = mutableStateListOf<Screen>(Screen.Home)
    var currentDbName by mutableStateOf("kj2.sqlite3")
    var currentVersionAbbr by mutableStateOf(BibleVersionUtils.versionMap["kj2.sqlite3"]!!)
    var customColor by mutableStateOf<Color?>(null)
    var multiVersion by mutableStateOf(false)
    var secondaryDbName by mutableStateOf("esv.sqlite3")
    var secondaryVersionAbbr by mutableStateOf(BibleVersionUtils.versionMap["esv.sqlite3"]!!)
    var multiViewLayout by mutableStateOf("horizontal")
    var scrollSync by mutableStateOf(true)
    var isReaderFullScreen by mutableStateOf(false)
    var showNavigationModal by mutableStateOf(false)
    var showPrimaryVersionDropdown by mutableStateOf(false)
    var showSecondaryVersionDropdown by mutableStateOf(false)
    var showColorThemeDialog by mutableStateOf(false)
    var showColorWheelDialog by mutableStateOf(false)
    var showBgModal by mutableStateOf(false)
    var primaryPassage by mutableStateOf(PassageSelection(10, "Genesis", 1, 1))
    var secondaryPassage by mutableStateOf(PassageSelection(500, "John", 1, 1))
    var bgImageIndex by mutableIntStateOf(0)
    var customTextureUri by mutableStateOf<String?>(null)
    var overlayOpacity by mutableFloatStateOf(0.15f)
    var lightOverlayColor by mutableStateOf(Color(0xFFF5F5DC))
    var darkOverlayColor by mutableStateOf(Color(0xFF100F21))
    var selectedDictionary by mutableStateOf("atsbd")

    var selectedVerseCommentary by mutableStateOf("cbsc")

    val isOldTestament: Boolean
        get() = BibleData.getBookByCustomNumber(primaryPassage.bookNumber)?.testament == Testament.OLD

    val isSecondaryOldTestament: Boolean
        get() = BibleData.getBookByCustomNumber(secondaryPassage.bookNumber)?.testament == Testament.OLD

    var isRefreshingDatabases by mutableStateOf(false)
    var lastRefreshMessage by mutableStateOf("")
    var lastRefreshSuccess by mutableStateOf(false)

    var showReaderOverlayColorWheel by mutableStateOf(false)

    fun refreshDatabases(context: Context) {
        isRefreshingDatabases = true
        lastRefreshMessage = "Starting database refresh..."
        lastRefreshSuccess = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val databaseFiles = mutableListOf<String>()
                var successCount = 0
                var totalCount: Int
                val assetDirs = listOf("databases", "dictionaries")

                assetDirs.forEach { dir ->
                    try {
                        val files = context.assets.list(dir)
                        files?.forEach { file ->
                            if (file.endsWith(".sqlite3") || file.endsWith(".sqlite")) {
                                databaseFiles.add(file)
                            }
                        }
                    } catch (_: IOException) {
                    }
                }
                val allDatabases = databaseFiles.distinct().toMutableList()
                if (!allDatabases.contains(currentDbName) && currentDbName.isNotEmpty()) {
                    allDatabases.add(currentDbName)
                }
                if (!allDatabases.contains(secondaryDbName) && secondaryDbName.isNotEmpty()) {
                    allDatabases.add(secondaryDbName)
                }

                totalCount = allDatabases.size
                allDatabases.forEachIndexed { index, dbName ->
                    try {
                        withContext(Dispatchers.Main) {
                            lastRefreshMessage = "Refreshing database ${index + 1}/$totalCount: $dbName"
                        }

                        val dbHelper = DatabaseHelper(context, dbName)
                        if (dbHelper.refreshDatabase()) {
                            successCount++
                        }
                        dbHelper.close()
                    } catch (_: Exception) {
                    }
                }

                withContext(Dispatchers.Main) {
                    isRefreshingDatabases = false
                    if (successCount == totalCount) {
                        lastRefreshMessage = "✅ Successfully refreshed all $totalCount databases!"
                        lastRefreshSuccess = true
                    } else {
                        lastRefreshMessage = "⚠ Refreshed $successCount out of $totalCount databases. Some may have failed."
                        lastRefreshSuccess = false
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isRefreshingDatabases = false
                    lastRefreshMessage = "❌ Error refreshing databases: ${e.message}"
                    lastRefreshSuccess = false
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        navigationStack.add(screen)
    }

    fun goBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.lastIndex)
        }
    }

    fun updateCurrentScreen(newScreen: Screen) {
        if (navigationStack.isNotEmpty()) {
            navigationStack[navigationStack.lastIndex] = newScreen
        }
    }
}