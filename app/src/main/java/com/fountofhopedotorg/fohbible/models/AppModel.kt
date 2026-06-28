package com.fountofhopedotorg.fohbible.models

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.gfx_creator.getElementDisplayName
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.utils.InteractiveModalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@Stable
class AppViewModel(application: Application) : AndroidViewModel(application) {
    var canvasNotes = mutableStateListOf<CanvasNote>()
    var animatorKeyframeTargetNoteId: String? by mutableStateOf(null)
    var animatorShowKeyframeDialog: Boolean by mutableStateOf(false)

    fun updateAnimatorNoteKeyframes(noteId: String, keyframes: List<CanvasKeyframe>) {
        val index = animatorCanvasNotes.indexOfFirst { it.id == noteId }
        if (index != -1) {
            animatorCanvasNotes[index] = animatorCanvasNotes[index].copy(keyframes = keyframes)
        }
    }

    fun addToCanvas(note: CanvasNote) {
        canvasNotes.add(note.copy(id = UUID.randomUUID().toString()))
    }

    fun removeFromCanvas(index: Int) {
        if (index in canvasNotes.indices) {
            canvasNotes.removeAt(index)
        }
    }

    fun updateNoteColor(id: String, color: Color) {
        val index = canvasNotes.indexOfFirst { it.id == id }
        if (index != -1) {
            canvasNotes[index] = canvasNotes[index].copy(backgroundColor = color)
        }
    }

    fun updateNoteTextColor(noteId: String, color: Color) {
        val index = canvasNotes.indexOfFirst { it.id == noteId }
        if (index != -1) {
            canvasNotes[index] = canvasNotes[index].copy(textColor = color)
        }
    }

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

    val textures = listOf(
        "1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg", "7.jpg", "8.jpg", "9.jpg", "10.jpg", "11.jpg", "12.jpg", "13.jpg", "14.jpg", "15.jpg", "16.jpg", "17.jpg",
        "18.jpg", "19.jpg", "20.jpg", "21.jpg", "22.jpg", "23.jpg", "24.jpg", "25.jpg", "26.jpg", "27.jpg", "28.jpg", "29.jpg", "30.jpg", "31.jpg", "32.jpg", "33.jpg"
    )

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

    val animatorGradientPairs = mutableStateMapOf<String, GradientConfig>()
    var animatorCanvasNotes = mutableStateListOf<CanvasNote>()
    var animatorSelectedNoteIds by mutableStateOf<Set<String>>(emptySet())
    var animatorSelectedNoteId by mutableStateOf<String?>(null)
    var animatorShowGroupDialog by mutableStateOf(false)
    var animatorGroupName by mutableStateOf("")
    val animatorGroupNames = mutableStateMapOf<String, String>()
    var animatorGroupToRenameId by mutableStateOf<String?>(null)
    var animatorGroupRenameText by mutableStateOf("")
    var animatorNoteToRenameId by mutableStateOf<String?>(null)
    var animatorRenameText by mutableStateOf("")
    var animatorShowColorPicker by mutableStateOf(false)
    var animatorNoteToColorEditId by mutableStateOf<String?>(null)
    var animatorShowCustomPolygonDialog by mutableStateOf(false)
    var animatorPolygonNoteToEditId by mutableStateOf<String?>(null)
    var animatorInitialPolygonString by mutableStateOf("")
    var animatorInitialIsLineMode by mutableStateOf(false)
    var animatorShowEditPropertiesDialog by mutableStateOf(false)
    var animatorEditPropertiesNoteId by mutableStateOf<String?>(null)
    var animatorEditX by mutableStateOf("")
    var animatorEditY by mutableStateOf("")
    var animatorEditScaleX by mutableStateOf("")
    var animatorEditScaleY by mutableStateOf("")
    var animatorEditRotation by mutableStateOf("")
    var animatorEditColorForDialog by mutableStateOf(Color.White)
    var animatorEditShadowColorForDialog by mutableStateOf<Color?>(null)
    var animatorEditShadowOffsetX by mutableFloatStateOf(0f)
    var animatorEditShadowOffsetY by mutableFloatStateOf(0f)
    var animatorEditBorderThickness by mutableFloatStateOf(0f)
    var animatorEditBorderColorForDialog by mutableStateOf<Color?>(null)
    var animatorSelectedInputMode by mutableStateOf("Add SVG")
    var animatorDialogType by mutableStateOf<AnimatorDialogType?>(null)
    var isAnimatorFullScreen by mutableStateOf(false)
    var proportionalEditing by mutableStateOf(true)
    var showSaveMenu by mutableStateOf(false)
    var isCreatorFullScreen by mutableStateOf(false)
    var disabledVersions by mutableStateOf<Set<String>>(emptySet())
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

    fun toggleVisibility(noteId: String) {
        val index = canvasNotes.indexOfFirst { it.id == noteId }
        if (index != -1) {
            canvasNotes[index] = canvasNotes[index].copy(isVisible = !canvasNotes[index].isVisible)
        }
    }

    fun toggleLock(noteId: String) {
        val index = canvasNotes.indexOfFirst { it.id == noteId }
        if (index != -1) {
            canvasNotes[index] = canvasNotes[index].copy(isLocked = !canvasNotes[index].isLocked)
        }
    }

    fun renameCanvasNote(noteId: String, newName: String) {
        val index = canvasNotes.indexOfFirst { it.id == noteId }
        if (index != -1) {
            val updatedNote = canvasNotes[index].copy(customName = newName.trim())
            canvasNotes[index] = updatedNote
        }
    }

    fun createGroup(noteIds: List<String>) {
        if (noteIds.size < 2) return

        val groupId = "group_${UUID.randomUUID().toString().take(8)}"
        val groupedNotes = canvasNotes.filter { it.id in noteIds }.map { note ->
            val originalIndex = canvasNotes.indexOf(note)
            note.copy(
                groupId = groupId,
                customName = note.customName ?: getElementDisplayName(note, originalIndex, canvasNotes)
            )
        }
        canvasNotes.removeAll { it.id in noteIds }
        canvasNotes.addAll(0, groupedNotes)
    }

    fun ungroupNotes(noteIds: Set<String>) {
        for (i in canvasNotes.indices) {
            if (noteIds.contains(canvasNotes[i].id)) {
                canvasNotes[i] = canvasNotes[i].copy(groupId = null)
            }
        }
    }

    fun updateNoteProperties(id: String, x: Float, y: Float, width: Float, height: Float, rotation: Float) {
        val index = canvasNotes.indexOfFirst { it.id == id }
        if (index != -1) {
            canvasNotes[index] = canvasNotes[index].copy(
                offset = Offset(x, y),
                width = width,
                height = height,
                rotation = rotation
            )
        }
    }

    fun updateNoteScale(id: String, scaleX: Float, scaleY: Float) {
        val index = canvasNotes.indexOfFirst { it.id == id }
        if (index != -1) {
            canvasNotes[index] = canvasNotes[index].copy(scaleX = scaleX, scaleY = scaleY)
        }
    }

    fun updateNoteContent(id: String, newContent: String) {
        val index = canvasNotes.indexOfFirst { it.id == id }
        if (index != -1) {
            canvasNotes[index] = canvasNotes[index].copy(content = newContent)
        }
    }

    fun reorderCanvasNotes(from: Int, to: Int) {
        if (from == to || from !in canvasNotes.indices || to !in canvasNotes.indices) return
        val item = canvasNotes.removeAt(from)
        canvasNotes.add(to, item)
    }

    val groupNames = mutableStateMapOf<String, String>()

    fun renameGroup(groupId: String, newName: String) {
        groupNames[groupId] = newName
    }

    fun applyAllNoteProperties(
        id: String,
        x: Float, y: Float, width: Float, height: Float, rotation: Float,
        scaleX: Float, scaleY: Float,
        color: Color, isTextElement: Boolean,
        shadowColor: Color?, shadowOffsetX: Float, shadowOffsetY: Float,
        borderThickness: Float, borderColor: Color?
    ) {
        val index = canvasNotes.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = canvasNotes[index]
            canvasNotes[index] = current.copy(
                offset = Offset(x, y),
                width = width,
                height = height,
                rotation = rotation,
                scaleX = scaleX,
                scaleY = scaleY,
                textColor = if (isTextElement) color else current.textColor,
                backgroundColor = if (!isTextElement) color else current.backgroundColor,
                shadowColor = shadowColor,
                shadowOffsetX = shadowOffsetX,
                shadowOffsetY = shadowOffsetY,
                borderThickness = borderThickness,
                borderColor = borderColor
            )
        }
    }

    fun addToAnimatorCanvas(note: CanvasNote) {
        animatorCanvasNotes.add(note.copy(id = UUID.randomUUID().toString()))
    }

    fun removeFromAnimatorCanvas(index: Int) {
        if (index in animatorCanvasNotes.indices) animatorCanvasNotes.removeAt(index)
    }

    fun updateAnimatorNoteColor(id: String, color: Color) {
        animatorCanvasNotes.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(backgroundColor = color)
        }
    }

    fun updateAnimatorNoteTextColor(noteId: String, color: Color) {
        animatorCanvasNotes.indexOfFirst { it.id == noteId }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(textColor = color)
        }
    }

    fun updateAnimatorNoteProperties(id: String, x: Float, y: Float, width: Float, height: Float, rotation: Float) {
        animatorCanvasNotes.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(
                offset = Offset(x, y), width = width, height = height, rotation = rotation
            )
        }
    }

    fun updateAnimatorNoteScale(id: String, scaleX: Float, scaleY: Float) {
        animatorCanvasNotes.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(scaleX = scaleX, scaleY = scaleY)
        }
    }

    fun updateAnimatorNoteContent(id: String, newContent: String) {
        animatorCanvasNotes.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(content = newContent)
        }
    }

    fun toggleAnimatorVisibility(noteId: String) {
        animatorCanvasNotes.indexOfFirst { it.id == noteId }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(isVisible = !animatorCanvasNotes[it].isVisible)
        }
    }

    fun toggleAnimatorLock(noteId: String) {
        animatorCanvasNotes.indexOfFirst { it.id == noteId }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(isLocked = !animatorCanvasNotes[it].isLocked)
        }
    }

    fun renameAnimatorCanvasNote(noteId: String, newName: String) {
        animatorCanvasNotes.indexOfFirst { it.id == noteId }.takeIf { it != -1 }?.let {
            animatorCanvasNotes[it] = animatorCanvasNotes[it].copy(customName = newName.trim())
        }
    }

    fun renameAnimatorGroup(groupId: String, newName: String) {
        animatorGroupNames[groupId] = newName
    }

    fun createAnimatorGroup(noteIds: List<String>) {
        if (noteIds.size < 2) return
        val groupId = "vid_group_${UUID.randomUUID().toString().take(8)}"
        val grouped = animatorCanvasNotes.filter { it.id in noteIds }.mapIndexed { _, note ->
            note.copy(groupId = groupId)
        }
        animatorCanvasNotes.removeAll { it.id in noteIds }
        animatorCanvasNotes.addAll(0, grouped)
    }

    fun ungroupAnimatorNotes(noteIds: Set<String>) {
        for (i in animatorCanvasNotes.indices) {
            if (animatorCanvasNotes[i].id in noteIds) {
                animatorCanvasNotes[i] = animatorCanvasNotes[i].copy(groupId = null)
            }
        }
    }

    fun applyAllAnimatorNoteProperties(
        id: String, x: Float, y: Float, width: Float, height: Float, rotation: Float,
        scaleX: Float, scaleY: Float, color: Color, isTextElement: Boolean,
        shadowColor: Color?, shadowOffsetX: Float, shadowOffsetY: Float,
        borderThickness: Float, borderColor: Color?
    ) {
        animatorCanvasNotes.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let {
            val current = animatorCanvasNotes[it]
            animatorCanvasNotes[it] = current.copy(
                offset = Offset(x, y), width = width, height = height, rotation = rotation,
                scaleX = scaleX, scaleY = scaleY,
                textColor = if (isTextElement) color else current.textColor,
                backgroundColor = if (!isTextElement) color else current.backgroundColor,
                shadowColor = shadowColor, shadowOffsetX = shadowOffsetX, shadowOffsetY = shadowOffsetY,
                borderThickness = borderThickness, borderColor = borderColor
            )
        }
    }

    fun reorderAnimatorCanvasNotes(from: Int, to: Int) {
        if (from == to || from !in animatorCanvasNotes.indices || to !in animatorCanvasNotes.indices) return
        val item = animatorCanvasNotes.removeAt(from)
        animatorCanvasNotes.add(to, item)
    }
}