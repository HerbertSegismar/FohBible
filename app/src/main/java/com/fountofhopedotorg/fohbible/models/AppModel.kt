package com.fountofhopedotorg.fohbible.models

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.utils.InteractiveModalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

// CHANGED: Now extends AndroidViewModel to have access to Application context
@Stable
class AppViewModel(application: Application) : AndroidViewModel(application) {

    val predefinedHighlightColors = mutableStateListOf<Color>().apply {
        addAll(
            listOf(
                Color(0xFFFBEE4E),
                Color(0xFF6EEB7E),
                Color(0xFF4EC7EB),
                Color(0xFF4E7BEB),
                Color(0xFFAF4EEB),
                Color(0xFFEB4E9E)
            )
        )
    }

    val imageFilesSm = listOf(
        "w1.jpg", "w2.jpg", "w3.jpg", "w4.jpg", "w5.jpg", "w6.jpg", "w7.jpg",
        "n1.jpg", "n2.jpg", "n3.jpg", "n4.jpg", "n5.jpg", "n6.jpg", "n7.jpg", "n8.jpg",
        "n9.jpg", "n10.jpg", "n11.jpg", "n12.jpg", "n13.jpg", "n14.jpg", "n15.jpg", "n16.jpg",
        "n17.jpg", "n18.jpg", "n19.jpg", "n20.jpg", "n21.jpg", "n22.jpg", "n23.jpg", "n24.jpg",
        "n25.jpg", "n26.jpg", "n27.jpg", "n28.jpg", "n29.jpg", "n30.jpg", "n31.jpg",
        "o1.jpg", "o2.jpg", "o3.jpg", "o4.jpg", "o5.jpg", "o6.jpg",
        "o7.jpg", "o8.jpg", "o9.jpg", "o10.jpg"
    )

    val imageFilesMd = listOf(
        "wm1.jpg", "wm2.jpg", "wm3.jpg", "wm4.jpg", "wm5.jpg", "wm6.jpg", "wm7.jpg",
        "nm1.jpg", "nm2.jpg", "nm3.jpg", "nm4.jpg", "nm5.jpg", "nm6.jpg", "nm7.jpg", "nm8.jpg",
        "nm9.jpg", "nm10.jpg", "nm11.jpg", "nm12.jpg", "nm13.jpg", "nm14.jpg",
        "om1.jpg", "om2.jpg", "om3.jpg", "om4.jpg", "om5.jpg"
    )

    var versionInfoForDialog by mutableStateOf("")
    var showVersionInfoDialog by mutableStateOf(false)
    var selectedPrimaryDictLanguage by mutableStateOf("English")
    var selectedSecondaryDictLanguage by mutableStateOf("English")
    var selectedPrimaryDictionary by mutableStateOf("atsbd")
    var selectedSecondaryDictionary by mutableStateOf("cbtel")
    var headerButtonsColor by mutableStateOf(Color(0xFFFFFFFF))
    var showStrongs by mutableStateOf(false)
    var squareAspectViews by mutableStateOf(true)
    var scrollSyncAction by mutableStateOf(false)
    var lightThemeReaderFontColor by mutableStateOf(Color(0xFF101015))
    var darkThemeReaderFontColor by mutableStateOf(Color(0xFFFFFFFF))
    var renderOrbs by mutableStateOf(false)
    var orbsCount by mutableIntStateOf(3)
    var wordsOfJesus by mutableStateOf(Color(0xFFDA4227))
    var editingHighlightColorIndex by mutableIntStateOf(-1)
    var showHighlightColorEditor by mutableStateOf(false)
    var isStudyMode by mutableStateOf(false)
    var wordMarkerColor by mutableStateOf(Color(0xDDAC95E1))
    var showWordMarkerColorWheelDialog by mutableStateOf(false)
    var showJesusWordsColorWheelDialog by mutableStateOf(false)
    var showLightReaderFontColorWheelDialog by mutableStateOf(false)
    var showDarkReaderFontColorWheelDialog by mutableStateOf(false)
    var showDarkOverlayColorWheel by  mutableStateOf(false)
    var showLightOverlayColorWheel by mutableStateOf(false)
    var isDictionaryMode by mutableStateOf(true)
    var verseMarkerColor by mutableStateOf(Color(0xFF95F198))
    var showVerseMarkerColorWheelDialog by mutableStateOf(false)
    var showSecondaryNavigationModal by mutableStateOf(false)
    var lightModalBackgroundColor by mutableStateOf(Color(0xFFEAE7E3))
    var darkModalBackgroundColor by mutableStateOf(Color(0xFF121523))
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
    var secondaryDbName by mutableStateOf("kjv+.sqlite3")
    var secondaryVersionAbbr by mutableStateOf(BibleVersionUtils.versionMap["kjv+.sqlite3"]!!)
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
    var overlayOpacity by mutableFloatStateOf(0.8f)
    var lightOverlayColor by mutableStateOf(Color(0xFFFFFFFF))
    var darkOverlayColor by mutableStateOf(Color(0xFF100F21))
    var selectedVerseCommentary by mutableStateOf("cbsc")
    var selectedCrossReferenceDatabase by mutableStateOf("obx")
    val isOldTestament: Boolean
        get() = BibleData.getBookByCustomNumber(primaryPassage.bookNumber)?.testament == Testament.OLD
    val isSecondaryOldTestament: Boolean
        get() = BibleData.getBookByCustomNumber(secondaryPassage.bookNumber)?.testament == Testament.OLD
    var isRefreshingDatabases by mutableStateOf(false)
    var lastRefreshMessage by mutableStateOf("")
    var lastRefreshSuccess by mutableStateOf(false)
    var showReaderOverlayColorWheel by mutableStateOf(false)

    // ADDED: Expose a databaseHelper for the current Bible version
    // Because currentDbName can change, we make it a property that reacts to changes.
    // For simplicity, a lazy property is used here – it will use the initial database name.
    // If you need it to switch automatically, you can replace this with a custom getter that returns
    // a new DatabaseHelper whenever currentDbName changes, but be careful about lifecycle.
    val databaseHelper by lazy {
        DatabaseHelper(getApplication(), currentDbName)
    }

    fun updateDictionaryForBibleLanguage(bibleFile: String) {
        val lang = BibleVersionUtils.getLanguageForVersion(bibleFile)
        val availableDicts = InteractiveModalUtils.dictionariesByLanguage[lang]
        selectedPrimaryDictLanguage = lang

        selectedPrimaryDictionary = if (!availableDicts.isNullOrEmpty()) {
            availableDicts.first()
        } else {
            ""
        }
    }

    fun updateSecondaryDictionaryForBibleLanguage(bibleFile: String) {
        val lang = BibleVersionUtils.getLanguageForVersion(bibleFile)
        val availableDicts = InteractiveModalUtils.dictionariesByLanguage[lang]
        selectedSecondaryDictLanguage = lang

        selectedSecondaryDictionary = if (!availableDicts.isNullOrEmpty()) {
            availableDicts.first()
        } else {
            ""
        }
    }

    fun updateHighlightColor(index: Int, newColor: Color) {
        if (index in predefinedHighlightColors.indices) {
            predefinedHighlightColors[index] = newColor
        }
    }
    fun resetHighlightColorsToDefault() {
        predefinedHighlightColors.clear()
        predefinedHighlightColors.addAll(
            listOf(
                Color(0xFFFBEE4E),
                Color(0xFF6EEB7E),
                Color(0xFF4EC7EB),
                Color(0xFF4E7BEB),
                Color(0xFFAF4EEB),
                Color(0xFFEB4E9E)
            )
        )
    }
    fun refreshDatabases(context: Context) {
        isRefreshingDatabases = true
        lastRefreshMessage = "Starting database refresh..."
        lastRefreshSuccess = false
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val databaseFiles = mutableListOf<String>()
                var successCount = 0
                var totalCount: Int
                val assetDirs = listOf("databases", "dictionaries", "commentaries", "cross-references", "subheadings", "topical")
                assetDirs.forEach { dir ->
                    try {
                        val files = context.assets.list(dir)
                        files?.forEach { file ->
                            if (file.endsWith(".sqlite3")) {
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