package com.fountofhopedotorg.fohbible

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.AppThemeState
import com.fountofhopedotorg.fohbible.data.ColorTheme
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.modals.FontModal
import com.fountofhopedotorg.fohbible.modals.NavigationModal
import com.fountofhopedotorg.fohbible.modals.VersionSelectionModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.screens.BgModal
import com.fountofhopedotorg.fohbible.screens.BookmarksScreen
import com.fountofhopedotorg.fohbible.screens.HomeScreen
import com.fountofhopedotorg.fohbible.screens.NotesScreen
import com.fountofhopedotorg.fohbible.screens.ReaderScreen
import com.fountofhopedotorg.fohbible.screens.SearchScreen
import com.fountofhopedotorg.fohbible.screens.SettingsScreen
import com.fountofhopedotorg.fohbible.ui.theme.DefaultPrimaryColor
import com.fountofhopedotorg.fohbible.ui.theme.FohBibleTheme
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.ui.theme.PredefinedColorThemes
import com.fountofhopedotorg.fohbible.ui.theme.ThemeManager
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val MARKER_COLOR_KEY = intPreferencesKey("marker_color")
private val PRIMARY_BOOK_NUMBER_KEY = intPreferencesKey("primary_book_number")
private val PRIMARY_BOOK_NAME_KEY = stringPreferencesKey("primary_book_name")
private val PRIMARY_CHAPTER_KEY = intPreferencesKey("primary_chapter")
private val PRIMARY_VERSE_KEY = intPreferencesKey("primary_verse")
private val SECONDARY_BOOK_NUMBER_KEY = intPreferencesKey("secondary_book_number")
private val SECONDARY_BOOK_NAME_KEY = stringPreferencesKey("secondary_book_name")
private val SECONDARY_CHAPTER_KEY = intPreferencesKey("secondary_chapter")
private val SECONDARY_VERSE_KEY = intPreferencesKey("secondary_verse")
private val FONT_SIZE_KEY = intPreferencesKey("font_size")
private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
private val SELECTED_COLOR_KEY = intPreferencesKey("selected_color")
private val IS_CUSTOM_COLOR_KEY = booleanPreferencesKey("is_custom_color")
private val CUSTOM_COLOR_KEY = intPreferencesKey("custom_color")
private val FONT_FAMILY_KEY = stringPreferencesKey("font_family")
private val PRIMARY_DB_KEY = stringPreferencesKey("primary_db")
private val PRIMARY_ABBR_KEY = stringPreferencesKey("primary_abbr")
private val MULTI_VERSION_KEY = booleanPreferencesKey("multi_version")
private val SECONDARY_DB_KEY = stringPreferencesKey("secondary_db")
private val SECONDARY_ABBR_KEY = stringPreferencesKey("secondary_abbr")
private val MULTI_LAYOUT_KEY = stringPreferencesKey("multi_layout")
private val SCROLL_SYNC_KEY = booleanPreferencesKey("scroll_sync")
private val BG_INDEX_KEY = intPreferencesKey("bg_index")
private val CUSTOM_TEXTURE_KEY = stringPreferencesKey("custom_texture")
private val OVERLAY_OPACITY_KEY = floatPreferencesKey("overlay_opacity")
private val LIGHT_OVERLAY_COLOR_KEY = intPreferencesKey("light_overlay_color")
private val DARK_OVERLAY_COLOR_KEY = intPreferencesKey("dark_overlay_color")
private val IS_STUDY_MODE_KEY = booleanPreferencesKey("is_study_mode")
private val IS_DICTIONARY_MODE_KEY = booleanPreferencesKey("is_dictionary_mode")
private val VERSE_MARKER_COLOR_KEY = intPreferencesKey("verse_marker_color")
private val LIGHT_MODAL_BG_COLOR_KEY = intPreferencesKey("light_modal_bg_color")
private val DARK_MODAL_BG_COLOR_KEY = intPreferencesKey("dark_modal_bg_color")
private val SELECTED_DICTIONARY_KEY = stringPreferencesKey("selected_dictionary")
private val SELECTED_VERSE_COMMENTARY_KEY = stringPreferencesKey("selected_verse_commentary")
private val SELECTED_CROSS_REFERENCE_DB_KEY = stringPreferencesKey("selected_cross_ref_db")
private val PREDEFINED_HIGHLIGHT_COLORS_KEY = stringPreferencesKey("predefined_highlight_colors")
val ComponentActivity.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            FohBibleApp(this, viewModel)
        }
    }
}

@Composable
fun <T> SavePreference(
    getValue: () -> T,
    key: Preferences.Key<T>,
    dataStore: DataStore<Preferences>
) {
    LaunchedEffect(Unit) {
        snapshotFlow { getValue() }.collectLatest { value ->
            dataStore.edit { prefs -> prefs[key] = value }
        }
    }
}

@Composable
fun SaveNullableStringPreference(
    getValue: () -> String?,
    key: Preferences.Key<String>,
    dataStore: DataStore<Preferences>
) {
    LaunchedEffect(Unit) {
        snapshotFlow { getValue() }.collectLatest { uri ->
            dataStore.edit { prefs ->
                if (uri != null) prefs[key] = uri
                else prefs.remove(key)
            }
        }
    }
}


@Composable
fun FohBibleApp(activity: MainActivity, viewModel: AppViewModel) {
    val currentScreen = viewModel.navigationStack.last()
    var isUsingCustomColor by remember { mutableStateOf(viewModel.isCustomColor) }
    var customColor by remember { mutableStateOf(viewModel.customColor) }
    val dataStore = remember { activity.appDataStore }

    // Load all preferences once
    LaunchedEffect(Unit) {
        val prefs = dataStore.data.first()
        with(viewModel) {
            fontSize = prefs[FONT_SIZE_KEY] ?: 18
            darkTheme = prefs[DARK_THEME_KEY] ?: false
            selectedColor = Color(prefs[SELECTED_COLOR_KEY] ?: DefaultPrimaryColor.toArgb())
            isCustomColor = prefs[IS_CUSTOM_COLOR_KEY] ?: false
            customColor = Color(prefs[CUSTOM_COLOR_KEY] ?: DefaultPrimaryColor.toArgb())
            selectedFontFamily = prefs[FONT_FAMILY_KEY] ?: "system"
            currentDbName = prefs[PRIMARY_DB_KEY] ?: "kj2.sqlite3"
            currentVersionAbbr = prefs[PRIMARY_ABBR_KEY] ?: BibleVersionUtils.versionMap["kj2.sqlite3"]!!
            multiVersion = prefs[MULTI_VERSION_KEY] ?: false
            secondaryDbName = prefs[SECONDARY_DB_KEY] ?: "kjv+.sqlite3"
            secondaryVersionAbbr = prefs[SECONDARY_ABBR_KEY] ?: BibleVersionUtils.versionMap["kjv+.sqlite3"]!!
            multiViewLayout = prefs[MULTI_LAYOUT_KEY] ?: "horizontal"
            scrollSync = prefs[SCROLL_SYNC_KEY] ?: true
            bgImageIndex = prefs[BG_INDEX_KEY] ?: 0
            customTextureUri = prefs[CUSTOM_TEXTURE_KEY]
            overlayOpacity = prefs[OVERLAY_OPACITY_KEY] ?: 0.8f
            lightOverlayColor = Color(prefs[LIGHT_OVERLAY_COLOR_KEY] ?: Color(0xFFF5F5DC).toArgb())
            darkOverlayColor = Color(prefs[DARK_OVERLAY_COLOR_KEY] ?: Color(0xFF100F21).toArgb())
            wordMarkerColor = Color(prefs[MARKER_COLOR_KEY] ?: Color(0xDDAC95E1).toArgb())
            isStudyMode = prefs[IS_STUDY_MODE_KEY] ?: true
            isDictionaryMode = prefs[IS_DICTIONARY_MODE_KEY] ?: true
            verseMarkerColor = Color(prefs[VERSE_MARKER_COLOR_KEY] ?: Color(0xFF95F198).toArgb())
            lightModalBackgroundColor = Color(prefs[LIGHT_MODAL_BG_COLOR_KEY] ?: Color(0xFFEAE7E3).toArgb())
            darkModalBackgroundColor = Color(prefs[DARK_MODAL_BG_COLOR_KEY] ?: Color(0xFF121523).toArgb())
            selectedDictionary = prefs[SELECTED_DICTIONARY_KEY] ?: "atsbd"
            selectedVerseCommentary = prefs[SELECTED_VERSE_COMMENTARY_KEY] ?: "cbsc"
            selectedCrossReferenceDatabase = prefs[SELECTED_CROSS_REFERENCE_DB_KEY] ?: "obx"
            primaryPassage = PassageSelection(
                bookNumber = prefs[PRIMARY_BOOK_NUMBER_KEY] ?: 10,
                bookName = prefs[PRIMARY_BOOK_NAME_KEY] ?: "Genesis",
                chapter = prefs[PRIMARY_CHAPTER_KEY] ?: 1,
                verse = prefs[PRIMARY_VERSE_KEY] ?: 1
            )
            secondaryPassage = PassageSelection(
                bookNumber = prefs[SECONDARY_BOOK_NUMBER_KEY] ?: 500,
                bookName = prefs[SECONDARY_BOOK_NAME_KEY] ?: "John",
                chapter = prefs[SECONDARY_CHAPTER_KEY] ?: 1,
                verse = prefs[SECONDARY_VERSE_KEY] ?: 1
            )
            val savedHighlightColors = prefs[PREDEFINED_HIGHLIGHT_COLORS_KEY]
            predefinedHighlightColors.clear()
            if (!savedHighlightColors.isNullOrBlank()) {
                savedHighlightColors.split(",").forEach { argbStr ->
                    try { predefinedHighlightColors.add(Color(argbStr.toInt())) } catch (_: Exception) {}
                }
            }
            if (predefinedHighlightColors.isEmpty()) resetHighlightColorsToDefault()
        }
    }

    SavePreference({ viewModel.fontSize }, FONT_SIZE_KEY, dataStore)
    SavePreference({ viewModel.darkTheme }, DARK_THEME_KEY, dataStore)
    SavePreference({ viewModel.selectedColor?.toArgb() ?: DefaultPrimaryColor.toArgb() }, SELECTED_COLOR_KEY, dataStore)
    SavePreference({ viewModel.isCustomColor }, IS_CUSTOM_COLOR_KEY, dataStore)
    SavePreference({ viewModel.customColor?.toArgb() ?: DefaultPrimaryColor.toArgb() }, CUSTOM_COLOR_KEY, dataStore)
    SavePreference({ viewModel.selectedFontFamily }, FONT_FAMILY_KEY, dataStore)
    SavePreference({ viewModel.currentDbName }, PRIMARY_DB_KEY, dataStore)
    SavePreference({ viewModel.currentVersionAbbr }, PRIMARY_ABBR_KEY, dataStore)
    SavePreference({ viewModel.multiVersion }, MULTI_VERSION_KEY, dataStore)
    SavePreference({ viewModel.secondaryDbName }, SECONDARY_DB_KEY, dataStore)
    SavePreference({ viewModel.secondaryVersionAbbr }, SECONDARY_ABBR_KEY, dataStore)
    SavePreference({ viewModel.multiViewLayout }, MULTI_LAYOUT_KEY, dataStore)
    SavePreference({ viewModel.scrollSync }, SCROLL_SYNC_KEY, dataStore)
    SavePreference({ viewModel.bgImageIndex }, BG_INDEX_KEY, dataStore)
    SavePreference({ viewModel.overlayOpacity }, OVERLAY_OPACITY_KEY, dataStore)
    SavePreference({ viewModel.lightOverlayColor.toArgb() }, LIGHT_OVERLAY_COLOR_KEY, dataStore)
    SavePreference({ viewModel.darkOverlayColor.toArgb() }, DARK_OVERLAY_COLOR_KEY, dataStore)
    SavePreference({ viewModel.wordMarkerColor.toArgb() }, MARKER_COLOR_KEY, dataStore)
    SavePreference({ viewModel.isStudyMode }, IS_STUDY_MODE_KEY, dataStore)
    SavePreference({ viewModel.isDictionaryMode }, IS_DICTIONARY_MODE_KEY, dataStore)
    SavePreference({ viewModel.verseMarkerColor.toArgb() }, VERSE_MARKER_COLOR_KEY, dataStore)
    SavePreference({ viewModel.lightModalBackgroundColor.toArgb() }, LIGHT_MODAL_BG_COLOR_KEY, dataStore)
    SavePreference({ viewModel.darkModalBackgroundColor.toArgb() }, DARK_MODAL_BG_COLOR_KEY, dataStore)
    SavePreference({ viewModel.selectedDictionary }, SELECTED_DICTIONARY_KEY, dataStore)
    SavePreference({ viewModel.selectedVerseCommentary }, SELECTED_VERSE_COMMENTARY_KEY, dataStore)
    SavePreference({ viewModel.selectedCrossReferenceDatabase }, SELECTED_CROSS_REFERENCE_DB_KEY, dataStore)
    SavePreference(
        { viewModel.predefinedHighlightColors.joinToString(",") { it.toArgb().toString() } },
        PREDEFINED_HIGHLIGHT_COLORS_KEY,
        dataStore
    )
    SaveNullableStringPreference({ viewModel.customTextureUri }, CUSTOM_TEXTURE_KEY, dataStore)

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.primaryPassage }.collectLatest { passage ->
            dataStore.edit { prefs ->
                prefs[PRIMARY_BOOK_NUMBER_KEY] = passage.bookNumber
                prefs[PRIMARY_BOOK_NAME_KEY] = passage.bookName
                prefs[PRIMARY_CHAPTER_KEY] = passage.chapter
                prefs[PRIMARY_VERSE_KEY] = passage.verse ?: 1
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.secondaryPassage }.collectLatest { passage ->
            dataStore.edit { prefs ->
                prefs[SECONDARY_BOOK_NUMBER_KEY] = passage.bookNumber
                prefs[SECONDARY_BOOK_NAME_KEY] = passage.bookName
                prefs[SECONDARY_CHAPTER_KEY] = passage.chapter
                prefs[SECONDARY_VERSE_KEY] = passage.verse ?: 1
            }
        }
    }


    LaunchedEffect(viewModel.selectedColor, viewModel.darkTheme, viewModel.isCustomColor, viewModel.customColor) {
        viewModel.selectedColor?.let {
            ThemeManager.primaryColor = it
            ThemeManager.darkTheme = viewModel.darkTheme
            ThemeManager.isCustomColor = viewModel.isCustomColor
        }
        isUsingCustomColor = viewModel.isCustomColor
        customColor = viewModel.customColor
    }

    val themeState = AppThemeState(
        darkTheme = viewModel.darkTheme,
        primaryColor = viewModel.selectedColor ?: DefaultPrimaryColor,
        isCustomColor = viewModel.isCustomColor
    )

    var dbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    LaunchedEffect(viewModel.currentDbName) {
        dbHelper?.close()
        dbHelper = DatabaseHelper(activity, viewModel.currentDbName)
    }
    DisposableEffect(Unit) {
        onDispose { dbHelper?.close() }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.customTextureUri = it.toString()
            viewModel.bgImageIndex = 34
        }
    }

    CompositionLocalProvider(LocalAppTheme provides themeState) {
        FohBibleTheme(darkTheme = viewModel.darkTheme) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (currentScreen !is Screen.Reader || !viewModel.isReaderFullScreen) {
                        if (currentScreen is Screen.Reader) {
                            ReaderAppBar(
                                currentScreen = currentScreen,
                                currentVersionAbbr = viewModel.currentVersionAbbr,
                                onBibleIconClick = { viewModel.showNavigationModal = true },
                                onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                                onColorLensClick = { viewModel.showColorThemeDialog = true },
                                onScreenChange = { screen ->
                                    val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                    viewModel.navigateTo(targetScreen)
                                },
                                onBack = if (viewModel.navigationStack.size > 1) { { viewModel.goBack() } } else null
                            )
                        } else {
                            HomeAppBar(
                                currentScreen = currentScreen,
                                onBibleIconClick = { viewModel.showNavigationModal = true },
                                onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                                onColorLensClick = { viewModel.showColorThemeDialog = true },
                                onScreenChange = { screen ->
                                    val targetScreen = if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                    viewModel.navigateTo(targetScreen)
                                },
                                onBack = if (viewModel.navigationStack.size > 1) { { viewModel.goBack() } } else null,
                                appViewModel = viewModel
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (currentScreen is Screen.Home) {
                        FloatingActionButton(
                            onClick = { viewModel.showNavigationModal = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Book, contentDescription = "Open Bible")
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    BackHandler(enabled = viewModel.navigationStack.size > 1) { viewModel.goBack() }
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            onNavigateToReader = { passage ->
                                viewModel.primaryPassage = passage
                                if (viewModel.scrollSync) viewModel.secondaryPassage = passage
                                viewModel.navigateTo(Screen.Reader(passage))
                            },
                            onNavigateToScreen = { screen ->
                                when (screen) {
                                    is Screen.Reader -> viewModel.navigateTo(Screen.Reader(viewModel.primaryPassage))
                                    else -> viewModel.navigateTo(screen)
                                }
                            },
                            databaseHelper = dbHelper
                        )
                        is Screen.Reader -> {
                            val passage = currentScreen.passage ?: viewModel.primaryPassage
                            val onPassageChange: (PassageSelection) -> Unit = { newPassage ->
                                viewModel.navigationStack[viewModel.navigationStack.lastIndex] = Screen.Reader(newPassage)
                                viewModel.primaryPassage = newPassage
                                if (viewModel.scrollSync) viewModel.secondaryPassage = newPassage
                            }
                            ReaderScreen(passage = passage, databaseHelper = dbHelper, onPassageChange = onPassageChange)
                        }
                        Screen.Bookmarks -> BookmarksScreen(onNavigateToReader = { passage ->
                            viewModel.primaryPassage = passage
                            if (viewModel.scrollSync) viewModel.secondaryPassage = passage
                            viewModel.navigateTo(Screen.Reader(passage))
                        })
                        Screen.Notes -> NotesScreen(onNavigateToReader = { passage ->
                            viewModel.primaryPassage = passage
                            if (viewModel.scrollSync) viewModel.secondaryPassage = passage
                            viewModel.navigateTo(Screen.Reader(passage))
                        })
                        Screen.Settings -> SettingsScreen()
                        Screen.Search -> SearchScreen(
                            databaseHelper = dbHelper,
                            onPassageSelected = { passage ->
                                viewModel.primaryPassage = passage
                                if (viewModel.scrollSync) viewModel.secondaryPassage = passage
                                if (viewModel.navigationStack.size > 1 && viewModel.navigationStack[viewModel.navigationStack.size - 2] is Screen.Reader) {
                                    viewModel.navigationStack[viewModel.navigationStack.size - 2] = Screen.Reader(passage)
                                    viewModel.goBack()
                                } else {
                                    viewModel.goBack()
                                    viewModel.navigateTo(Screen.Reader(passage))
                                }
                            },
                            currentVersionKey = viewModel.currentDbName,
                            onVersionChange = { newVersionKey ->
                                viewModel.currentDbName = newVersionKey
                                viewModel.currentVersionAbbr = BibleVersionUtils.versionMap[newVersionKey] ?: "Bible"
                            }
                        )
                    }
                    if (viewModel.showNavigationModal) {
                        NavigationModal(
                            onDismissRequest = { viewModel.showNavigationModal = false },
                            onPassageSelected = { passage ->
                                viewModel.primaryPassage = passage
                                if (viewModel.scrollSync) viewModel.secondaryPassage = passage
                                viewModel.showNavigationModal = false
                                viewModel.updateCurrentScreen(Screen.Reader(passage))
                            },
                            showNavigationModal = true,
                            databaseHelper = dbHelper,
                            initialBookNumber = viewModel.primaryPassage.bookNumber,
                            initialChapter = viewModel.primaryPassage.chapter,
                            initialVerse = viewModel.primaryPassage.verse,
                        )
                    }
                    if (viewModel.showSecondaryNavigationModal) {
                        NavigationModal(
                            onDismissRequest = { viewModel.showSecondaryNavigationModal = false },
                            onPassageSelected = { passage ->
                                viewModel.secondaryPassage = passage
                                viewModel.showSecondaryNavigationModal = false
                            },
                            showNavigationModal = true,
                            databaseHelper = dbHelper,
                            initialBookNumber = viewModel.secondaryPassage.bookNumber,
                            initialChapter = viewModel.secondaryPassage.chapter,
                            initialVerse = viewModel.secondaryPassage.verse,
                        )
                    }
                    if (viewModel.showPrimaryVersionDropdown) {
                        VersionSelectionModal(
                            currentVersionKey = viewModel.currentDbName,
                            onVersionSelected = { file ->
                                viewModel.currentDbName = file
                                viewModel.currentVersionAbbr = BibleVersionUtils.versionMap[file] ?: "Bible"
                                viewModel.showPrimaryVersionDropdown = false
                            },
                            onDismiss = { viewModel.showPrimaryVersionDropdown = false },
                            colors = mapOf(
                                "primary" to MaterialTheme.colorScheme.primary,
                                "card" to if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor,
                                "text" to MaterialTheme.colorScheme.onSurface,
                                "muted" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                "border" to MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                    if (viewModel.showSecondaryVersionDropdown) {
                        VersionSelectionModal(
                            currentVersionKey = viewModel.secondaryDbName,
                            onVersionSelected = { file ->
                                viewModel.secondaryDbName = file
                                viewModel.secondaryVersionAbbr = BibleVersionUtils.versionMap[file] ?: "Bible"
                                viewModel.showSecondaryVersionDropdown = false
                            },
                            onDismiss = { viewModel.showSecondaryVersionDropdown = false },
                            colors = mapOf(
                                "primary" to MaterialTheme.colorScheme.primary,
                                "card" to if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor,
                                "text" to MaterialTheme.colorScheme.onSurface,
                                "muted" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                "border" to MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                    if (viewModel.showColorThemeDialog) {
                        Dialog(onDismissRequest = { viewModel.showColorThemeDialog = false }) {
                            UpdatedColorThemeDialog(
                                onDismiss = { viewModel.showColorThemeDialog = false },
                                onColorSelected = { color ->
                                    viewModel.selectedColor = color
                                    viewModel.isCustomColor = false
                                    viewModel.customColor = null
                                },
                                onCustomColorClick = {
                                    viewModel.showColorThemeDialog = false
                                    viewModel.showColorWheelDialog = true
                                },
                                appViewModel = viewModel
                            )
                        }
                    }
                    if (viewModel.showColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showColorWheelDialog = false },
                            onColorSelected = { color ->
                                viewModel.selectedColor = color
                                viewModel.isCustomColor = true
                                viewModel.customColor = color
                                viewModel.showColorWheelDialog = false
                            },
                            initialColor = if (viewModel.isCustomColor && viewModel.customColor != null) viewModel.customColor!! else viewModel.selectedColor ?: ThemeManager.primaryColor
                        )
                    }
                    if (viewModel.showBgModal) {
                        BgModal(
                            currentIndex = viewModel.bgImageIndex,
                            customUri = viewModel.customTextureUri,
                            onSelect = { index -> viewModel.bgImageIndex = index; viewModel.showBgModal = false },
                            onDismiss = { viewModel.showBgModal = false },
                            onPickCustom = { imagePickerLauncher.launch("image/*") },
                            onRemoveCustom = { viewModel.customTextureUri = null }
                        )
                    }
                    if (viewModel.showReaderOverlayColorWheel) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showReaderOverlayColorWheel = false },
                            onColorSelected = { color ->
                                if (viewModel.darkTheme) viewModel.darkOverlayColor = color else viewModel.lightOverlayColor = color
                                viewModel.showReaderOverlayColorWheel = false
                            },
                            initialColor = if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor
                        )
                    }
                    if (viewModel.showWordMarkerColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showWordMarkerColorWheelDialog = false },
                            onColorSelected = { color -> viewModel.wordMarkerColor = color; viewModel.showWordMarkerColorWheelDialog = false },
                            initialColor = viewModel.wordMarkerColor
                        )
                    }
                    if (viewModel.showVerseMarkerColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showVerseMarkerColorWheelDialog = false },
                            onColorSelected = { color -> viewModel.verseMarkerColor = color; viewModel.showVerseMarkerColorWheelDialog = false },
                            initialColor = viewModel.verseMarkerColor
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    tint: Color = Color.White
) {
    var targetRotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(300),
        label = "iconRotation"
    )
    IconButton(
        onClick = {
            targetRotation += rotation
            onClick()
        },
        modifier = modifier
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.rotate(animatedRotation))
    }
}

@Composable
fun ColorPickerRow(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = { Text(label, modifier = Modifier.fillMaxWidth()) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
            )
        },
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun FontSizeControls(viewModel: AppViewModel) {
    val minFontSize = 1
    val maxFontSize = 100
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var tempFontSize by remember { mutableStateOf(viewModel.fontSize.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Font Size", style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
            Text("1-100", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.fontSize = maxOf(minFontSize, viewModel.fontSize - 1) }, modifier = Modifier.size(32.dp)) {
                Text("A-", fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${viewModel.fontSize}",
                modifier = Modifier.padding(horizontal = 16.dp).clickable { showFontSizeDialog = true },
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.fontSize = minOf(maxFontSize, viewModel.fontSize + 1) }, modifier = Modifier.size(32.dp)) {
                Text("A+", fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showFontSizeDialog) {
        FontModal(
            tempSize = tempFontSize,
            onChange = { tempFontSize = it },
            onConfirm = {
                val newSize = tempFontSize.toIntOrNull()?.coerceIn(minFontSize, maxFontSize) ?: viewModel.fontSize
                viewModel.fontSize = newSize
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false },
            appViewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayOpacitySlider(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Overlay Opacity", style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
            Text("${(viewModel.overlayOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = viewModel.overlayOpacity,
            onValueChange = { viewModel.overlayOpacity = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                inactiveTickColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .shadow(2.dp, shape = CircleShape)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        )
    }
}

@Composable
fun DropdownMenuItemWithIcon(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(200),
        label = "dropdownBackground"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "dropdownTextColor"
    )
    DropdownMenuItem(
        text = { Text(title, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = textColor) },
        onClick = onClick,
        modifier = modifier.background(backgroundColor),
        leadingIcon = {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (title == "Notes") iconModifier.rotate(90f) else iconModifier
            )
        }
    )
}

val allScreens = listOf(
    "Home" to Icons.Filled.Home,
    "Reader" to Icons.Filled.Book,
    "Bookmarks" to Icons.Filled.Bookmark,
    "Notes" to Icons.AutoMirrored.Filled.Note,
    "Search" to Icons.Filled.Search,
    "Settings" to Icons.Filled.Settings
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    currentScreen: Screen,
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScreenChange: (Screen) -> Unit,
    onBack: (() -> Unit)? = null,
    appViewModel: AppViewModel
) {
    var showNavigationDropdown by remember { mutableStateOf(false) }
    val viewModel: AppViewModel = viewModel()
    val rotation by animateFloatAsState(targetValue = if (showNavigationDropdown) 180f else 0f, animationSpec = tween(300), label = "menuIconRotation")
    val screenTitle = when (currentScreen) {
        is Screen.Home -> "Home"
        is Screen.Reader -> "Reader"
        is Screen.Bookmarks -> "Bookmarks"
        is Screen.Notes -> "Notes"
        is Screen.Settings -> "Settings"
        is Screen.Search -> "Search"
    }
    TopAppBar(
        title = { Text(text = screenTitle, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(start = 0.dp), textAlign = TextAlign.Start) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = modifier.background(Brush.verticalGradient(0.0f to LocalAppTheme.current.primaryColor, 0.7f to LocalAppTheme.current.primaryColor, 1.0f to Color.Transparent)),
        navigationIcon = {
            if (onBack != null) {
                AnimatedIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", rotation = 360f)
            }
        },
        actions = {
            AnimatedIconButton(onClick = onBibleIconClick, icon = Icons.Filled.Book, contentDescription = "Bible Navigation", rotation = 360f)
            AnimatedIconButton(onClick = onThemeToggle, icon = if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2, contentDescription = "Toggle Theme", rotation = 180f)
            AnimatedIconButton(onClick = onColorLensClick, icon = Icons.Filled.ColorLens, contentDescription = "Color Scheme", rotation = 180f)
            IconButton(onClick = { showNavigationDropdown = !showNavigationDropdown }, modifier = Modifier.rotate(rotation)) {
                Crossfade(targetState = showNavigationDropdown, animationSpec = tween(300), label = "iconCrossfade") { isOpen ->
                    Icon(imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Menu, contentDescription = if (isOpen) "Close Navigation" else "Open Navigation", tint = Color.White)
                }
            }
            DropdownMenu(
                expanded = showNavigationDropdown,
                onDismissRequest = { showNavigationDropdown = false },
                modifier = Modifier.background(if (appViewModel.darkTheme) appViewModel.darkModalBackgroundColor else appViewModel.lightModalBackgroundColor)
            ) {
                allScreens.forEach { (title, icon) ->
                    val isActive = when (title) {
                        "Home" -> currentScreen is Screen.Home
                        "Reader" -> currentScreen is Screen.Reader
                        "Bookmarks" -> currentScreen == Screen.Bookmarks
                        "Notes" -> currentScreen == Screen.Notes
                        "Search" -> currentScreen == Screen.Search
                        "Settings" -> currentScreen == Screen.Settings
                        else -> false
                    }
                    DropdownMenuItemWithIcon(
                        title = title,
                        icon = icon,
                        isActive = isActive,
                        onClick = {
                            val targetScreen = when (title) {
                                "Home" -> Screen.Home
                                "Reader" -> Screen.Reader()
                                "Bookmarks" -> Screen.Bookmarks
                                "Notes" -> Screen.Notes
                                "Search" -> Screen.Search
                                "Settings" -> Screen.Settings
                                else -> Screen.Home
                            }
                            onScreenChange(targetScreen)
                            showNavigationDropdown = false
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppBar(
    currentScreen: Screen.Reader,
    currentVersionAbbr: String,
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScreenChange: (Screen) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: AppViewModel = viewModel()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()

    var showNavigationDropdown by remember { mutableStateOf(false) }
    var showMultiDropdown by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(targetValue = if (showNavigationDropdown) 180f else 0f, animationSpec = tween(300), label = "menuIconRotation")
    val multiRotation by animateFloatAsState(targetValue = if (showMultiDropdown) 180f else 0f, animationSpec = tween(300), label = "multiIconRotation")

    TopAppBar(
        title = {
            if (!viewModel.multiVersion) {
                Row(modifier = Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onBibleIconClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(25.dp).weight(0.7f).padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentScreen.passage?.bookName ?: "Reader", fontWeight = FontWeight.Bold, fontSize = 16.sp, overflow = TextOverflow.Ellipsis, maxLines = 1, color = Color.White, modifier = Modifier.weight(0.5f))
                            Text(text = currentScreen.passage?.chapter?.let { " $it" } ?: "", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                    Button(
                        onClick = { viewModel.showPrimaryVersionDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(25.dp).weight(0.5f).padding(end = if (onBack == null) 8.dp else 2.dp)
                    ) {
                        Text(text = currentVersionAbbr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, maxLines = 1)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = modifier.background(Brush.verticalGradient(0.0f to LocalAppTheme.current.primaryColor, 0.7f to LocalAppTheme.current.primaryColor, 1.0f to Color.Transparent)),
        navigationIcon = {
            if (onBack != null) {
                AnimatedIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", rotation = 360f)
            }
        },
        actions = {
            AnimatedIconButton(onClick = onThemeToggle, icon = if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2, contentDescription = "Toggle Theme", rotation = 180f)
            AnimatedIconButton(onClick = onColorLensClick, icon = Icons.Filled.ColorLens, contentDescription = "Color Scheme", rotation = 180f)
            if (viewModel.multiVersion) {
                AnimatedIconButton(onClick = { viewModel.scrollSync = !viewModel.scrollSync }, icon = if (viewModel.scrollSync) Icons.Filled.Link else Icons.Filled.LinkOff, contentDescription = "Toggle Scroll Sync", rotation = 180f)
            }
            IconButton(onClick = { showMultiDropdown = !showMultiDropdown }, modifier = Modifier.size(40.dp).rotate(multiRotation)) {
                Crossfade(targetState = showMultiDropdown, animationSpec = tween(300), label = "multiIconCrossfade") { isOpen ->
                    Icon(imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.AutoAwesomeMosaic, contentDescription = if (isOpen) "Close MultiView" else "MultiView", tint = Color.White)
                }
            }
            DropdownMenu(
                expanded = showMultiDropdown,
                onDismissRequest = { showMultiDropdown = false },
                modifier = Modifier.background(if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor),
                offset = DpOffset(x = 100.dp, y = 0.dp),
            ) {
                Text("Windows Layout", modifier = Modifier.fillMaxWidth().height(25.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                listOf("Single", "Horizontal", "Vertical").forEach { layout ->
                    val isActive = when (layout.lowercase()) {
                        "single" -> !viewModel.multiVersion
                        "horizontal" -> viewModel.multiVersion && viewModel.multiViewLayout == "horizontal"
                        "vertical" -> viewModel.multiVersion && viewModel.multiViewLayout == "vertical"
                        else -> false
                    }
                    val icon = when (layout.lowercase()) {
                        "single" -> Icons.Default.LooksOne
                        "horizontal", "vertical" -> Icons.Default.ViewStream
                        else -> Icons.AutoMirrored.Filled.Label
                    }
                    val iconModifier = if (layout.lowercase() == "horizontal") Modifier.rotate(90f) else Modifier
                    DropdownMenuItemWithIcon(
                        title = layout,
                        icon = icon,
                        isActive = isActive,
                        onClick = {
                            when (layout.lowercase()) {
                                "single" -> viewModel.multiVersion = false
                                "horizontal" -> { viewModel.multiVersion = true; viewModel.multiViewLayout = "horizontal" }
                                "vertical" -> { viewModel.multiVersion = true; viewModel.multiViewLayout = "vertical" }
                            }
                            showMultiDropdown = false
                        },
                        iconModifier = iconModifier
                    )
                }
            }
            IconButton(onClick = { showNavigationDropdown = !showNavigationDropdown }, modifier = Modifier.size(40.dp).rotate(rotation)) {
                Crossfade(targetState = showNavigationDropdown, animationSpec = tween(300), label = "iconCrossfade") { isOpen ->
                    Icon(imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Menu, contentDescription = if (isOpen) "Close Navigation" else "Open Navigation", tint = Color.White)
                }
            }
            DropdownMenu(
                expanded = showNavigationDropdown,
                onDismissRequest = { showNavigationDropdown = false },
                modifier = Modifier.background(if (viewModel.darkTheme) viewModel.darkModalBackgroundColor else viewModel.lightModalBackgroundColor)
            ) {
                ReaderDropdownContent(isLandscape, viewModel, onScreenChange, coroutineScope)
            }
        }
    )
}

@Composable
fun ReaderDropdownContent(
    isLandscape: Boolean,
    viewModel: AppViewModel,
    onScreenChange: (Screen) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val commonItems = @Composable {
        allScreens.forEach { (title, icon) ->
            val isActive = when (title) {
                "Reader" -> true
                else -> false
            }
            DropdownMenuItemWithIcon(
                title = title,
                icon = icon,
                isActive = isActive,
                onClick = {
                    val targetScreen = when (title) {
                        "Home" -> Screen.Home
                        "Reader" -> Screen.Reader()
                        "Bookmarks" -> Screen.Bookmarks
                        "Notes" -> Screen.Notes
                        "Search" -> Screen.Search
                        "Settings" -> Screen.Settings
                        else -> Screen.Home
                    }
                    onScreenChange(targetScreen)
                }
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Background Texture", modifier = Modifier.fillMaxWidth()) },
            leadingIcon = { Icon(Icons.Outlined.Texture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            onClick = { viewModel.showBgModal = true }
        )
        HorizontalDivider()
        OverlayOpacitySlider(viewModel)
        HorizontalDivider()
        ColorPickerRow(
            label = if (viewModel.darkTheme) "Dark Overlay Color" else "Light Overlay Color",
            color = if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor,
            onClick = { viewModel.showReaderOverlayColorWheel = true }
        )
        HorizontalDivider()
        ColorPickerRow(label = "Verse Marker Color", color = viewModel.verseMarkerColor, onClick = { viewModel.showVerseMarkerColorWheelDialog = true })
        HorizontalDivider()
        ColorPickerRow(label = "Word Marker Color", color = viewModel.wordMarkerColor, onClick = { viewModel.showWordMarkerColorWheelDialog = true })
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(text = if (viewModel.isDictionaryMode) "Dictionary Mode On" else "Highlight Mode On", modifier = Modifier.fillMaxWidth()) },
            leadingIcon = { Icon(if (viewModel.isDictionaryMode) Icons.AutoMirrored.Filled.Label else Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                viewModel.isDictionaryMode = !viewModel.isDictionaryMode
                coroutineScope.launch { delay(400) }
            }
        )
        HorizontalDivider()
        FontSizeControls(viewModel)
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                commonItems()
                Spacer(modifier = Modifier.weight(1f))
            }
            VerticalDivider()
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                ExtraReaderControls(viewModel, coroutineScope)
            }
        }
    } else {
        Column { commonItems() }
    }
}

@Composable
fun ExtraReaderControls(viewModel: AppViewModel, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    OverlayOpacitySlider(viewModel)
    HorizontalDivider()
    ColorPickerRow(label = if (viewModel.darkTheme) "Dark Overlay Color" else "Light Overlay Color", color = if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor, onClick = { viewModel.showReaderOverlayColorWheel = true })
    HorizontalDivider()
    ColorPickerRow(label = "Verse Marker Color", color = viewModel.verseMarkerColor, onClick = { viewModel.showVerseMarkerColorWheelDialog = true })
    HorizontalDivider()
    ColorPickerRow(label = "Word Marker Color", color = viewModel.wordMarkerColor, onClick = { viewModel.showWordMarkerColorWheelDialog = true })
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text(text = if (viewModel.isDictionaryMode) "Dictionary Mode On" else "Highlight Mode On", modifier = Modifier.fillMaxWidth()) },
        leadingIcon = { Icon(if (viewModel.isDictionaryMode) Icons.AutoMirrored.Filled.Label else Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        onClick = {
            viewModel.isDictionaryMode = !viewModel.isDictionaryMode
            coroutineScope.launch { delay(400) }
        }
    )
    HorizontalDivider()
    FontSizeControls(viewModel)
}
// endregion

// region Color Theme Dialog
@Composable
fun UpdatedColorThemeDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    appViewModel: AppViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appViewModel.darkTheme) appViewModel.darkModalBackgroundColor else appViewModel.lightModalBackgroundColor
        )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Choose Theme Color", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Close, "Close") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(PredefinedColorThemes) { theme ->
                    ColorOptionItem(theme = theme, onClick = { onColorSelected(theme.primaryColor); onDismiss() })
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Custom Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().height(80.dp).clickable(onClick = onCustomColorClick),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(
                                    Brush.sweepGradient(colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))
                                ).border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Custom Color Picker", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Choose any color with color wheel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun ColorOptionItem(theme: ColorTheme, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.primaryColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(
                        Brush.horizontalGradient(colors = listOf(theme.primaryColor, theme.secondaryColor))
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(theme.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Primary & Secondary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
sealed class Screen {
    object Home : Screen()
    data class Reader(val passage: PassageSelection? = null) : Screen()
    object Bookmarks : Screen()
    object Notes : Screen()
    object Settings : Screen()
    object Search : Screen()
}