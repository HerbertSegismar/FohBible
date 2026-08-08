package com.fountofhopedotorg.fohbible

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.app_composables.BibleVersionInfoDialog
import com.fountofhopedotorg.fohbible.app_composables.HomeAppBar
import com.fountofhopedotorg.fohbible.app_composables.SaveNullableStringPreference
import com.fountofhopedotorg.fohbible.app_composables.SavePreference
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.AppThemeState
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.modals.BgModal
import com.fountofhopedotorg.fohbible.modals.NavigationModal
import com.fountofhopedotorg.fohbible.modals.VersionSelectionModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.data.BibleVersionInfo
import com.fountofhopedotorg.fohbible.data.BibleVersionInfoRepository
import com.fountofhopedotorg.fohbible.dictionary.DictionaryScreen
import com.fountofhopedotorg.fohbible.learn.LearnGreekScreen
import com.fountofhopedotorg.fohbible.learn.LearnHebrewScreen
import com.fountofhopedotorg.fohbible.quiz.BibleQuizScreen
import com.fountofhopedotorg.fohbible.bookmarks.BookmarksScreen
import com.fountofhopedotorg.fohbible.color_wheel.ColorThemeDialog
import com.fountofhopedotorg.fohbible.home.HomeScreen
import com.fountofhopedotorg.fohbible.notes.NotesScreen
import com.fountofhopedotorg.fohbible.reader.ReaderAppBar
import com.fountofhopedotorg.fohbible.reader.ReaderScreen
import com.fountofhopedotorg.fohbible.search.SearchScreen
import com.fountofhopedotorg.fohbible.settings.SettingsScreen
import com.fountofhopedotorg.fohbible.theme.DefaultPrimaryColor
import com.fountofhopedotorg.fohbible.theme.FohBibleTheme
import com.fountofhopedotorg.fohbible.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.theme.ThemeManager
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.gfx_animator.AnimatorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val LIGHT_THEME_READER_FONT_COLOR_KEY = intPreferencesKey("light_theme_reader_font_color")
private val DARK_THEME_READER_FONT_COLOR_KEY = intPreferencesKey("dark_theme_reader_font_color")
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
private val SHOW_STRONGS_KEY = booleanPreferencesKey("show_strongs")
private val IS_DICTIONARY_MODE_KEY = booleanPreferencesKey("is_dictionary_mode")
private val VERSE_MARKER_COLOR_KEY = intPreferencesKey("verse_marker_color")
private val LIGHT_MODAL_BG_COLOR_KEY = intPreferencesKey("light_modal_bg_color")
private val DARK_MODAL_BG_COLOR_KEY = intPreferencesKey("dark_modal_bg_color")
private val WORDS_OF_JESUS_KEY = intPreferencesKey("words_of_jesus")
private val HEADER_BUTTONS_KEY = intPreferencesKey("header_buttons_color")
private val SELECTED_DICTIONARY_KEY = stringPreferencesKey("selected_dictionary")
private val PRIMARY_DICT_LANGUAGE_KEY = stringPreferencesKey("primary_dict_language")
private val SECONDARY_DICT_LANGUAGE_KEY = stringPreferencesKey("secondary_dict_language")
private val SECONDARY_DICTIONARY_KEY = stringPreferencesKey("secondary_dictionary")
private val SELECTED_VERSE_COMMENTARY_KEY = stringPreferencesKey("selected_verse_commentary")
private val SELECTED_CROSS_REFERENCE_DB_KEY = stringPreferencesKey("selected_cross_ref_db")
private val PREDEFINED_HIGHLIGHT_COLORS_KEY = stringPreferencesKey("predefined_highlight_colors")
private val RENDER_ORBS_KEY = booleanPreferencesKey("show_orbs")
private val ORBS_COUNT_KEY = intPreferencesKey("orbs_count")
private val DISABLED_VERSIONS_KEY = stringPreferencesKey("disabled_versions")

val ComponentActivity.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class MainActivity : ComponentActivity() {
    private var pendingTemplateUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val viewModel: AppViewModel = viewModel()

            var animatorTemplateUri by remember { mutableStateOf<Uri?>(null) }

            LaunchedEffect(pendingTemplateUri) {
                pendingTemplateUri?.let {
                    animatorTemplateUri = it
                    pendingTemplateUri = null
                }
            }

            FohBibleApp(
                activity = this,
                viewModel = viewModel,
                animatorTemplateUri = animatorTemplateUri,
                onAnimatorTemplateConsumed = { animatorTemplateUri = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            pendingTemplateUri = intent.data
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun FohBibleApp(
    activity: MainActivity,
    viewModel: AppViewModel,
    animatorTemplateUri: Uri? = null,
    onAnimatorTemplateConsumed: () -> Unit = {}
) {
    var bibleInfoData by remember { mutableStateOf<BibleVersionInfo?>(null) }
    var isLoadingVersionInfo by remember { mutableStateOf(false) }
    var secondaryDbHelper by remember { mutableStateOf<DatabaseHelper?>(null) }
    val currentScreen = viewModel.navigationStack.last()
    val dataStore = remember { activity.appDataStore }

    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val context = LocalContext.current
    val activity = context as Activity
    val window = activity.window
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val insets = WindowInsets.systemBars.asPaddingValues()
    val vInset = if (!isLandscape) insets.calculateBottomPadding() else 0.dp

    val systemRightPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateRightPadding(LayoutDirection.Ltr)
    val systemLeftPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateLeftPadding(LayoutDirection.Ltr)
    val hInset = if (isLandscape) (systemRightPadding + systemLeftPadding) else 0.dp

    val toggleFullscreen = {
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (isLandscape) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(isLandscape) {
        toggleFullscreen()
    }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                toggleFullscreen()
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        val prefs = dataStore.data.first()
        with(viewModel) {
            fontSize = prefs[FONT_SIZE_KEY] ?: 18
            lightThemeReaderFontColor = Color(prefs[LIGHT_THEME_READER_FONT_COLOR_KEY] ?: Color(0xFF101015).toArgb())
            darkThemeReaderFontColor = Color(prefs[DARK_THEME_READER_FONT_COLOR_KEY] ?: Color(0xFFFFFFFF).toArgb())
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
            lightOverlayColor = Color(prefs[LIGHT_OVERLAY_COLOR_KEY] ?: Color(0xFFFFFFFF).toArgb())
            darkOverlayColor = Color(prefs[DARK_OVERLAY_COLOR_KEY] ?: Color(0xFF100F21).toArgb())
            wordMarkerColor = Color(prefs[MARKER_COLOR_KEY] ?: Color(0xDDAC95E1).toArgb())
            isStudyMode = prefs[IS_STUDY_MODE_KEY] ?: false
            showStrongs = prefs[SHOW_STRONGS_KEY] ?: false
            isDictionaryMode = prefs[IS_DICTIONARY_MODE_KEY] ?: true
            verseMarkerColor = Color(prefs[VERSE_MARKER_COLOR_KEY] ?: Color(0xFF95F198).toArgb())
            lightModalBackgroundColor = Color(prefs[LIGHT_MODAL_BG_COLOR_KEY] ?: Color(0xFFEAE7E3).toArgb())
            darkModalBackgroundColor = Color(prefs[DARK_MODAL_BG_COLOR_KEY] ?: Color(0xFF121523).toArgb())
            wordsOfJesus = Color(prefs[WORDS_OF_JESUS_KEY] ?: Color(0xFFDA4227).toArgb())
            headerButtonsColor = Color(prefs[HEADER_BUTTONS_KEY] ?: Color(0xFFFFFFFF).toArgb())
            selectedPrimaryDictionary = prefs[SELECTED_DICTIONARY_KEY] ?: "atsbd"
            selectedPrimaryDictLanguage = prefs[PRIMARY_DICT_LANGUAGE_KEY] ?: "English"
            selectedSecondaryDictLanguage = prefs[SECONDARY_DICT_LANGUAGE_KEY] ?: "English"
            selectedSecondaryDictionary = prefs[SECONDARY_DICTIONARY_KEY] ?: "cbtel"
            selectedVerseCommentary = prefs[SELECTED_VERSE_COMMENTARY_KEY] ?: "cbsc"
            selectedCrossReferenceDatabase = prefs[SELECTED_CROSS_REFERENCE_DB_KEY] ?: "obx"
            renderOrbs = prefs[RENDER_ORBS_KEY] ?: false
            orbsCount = prefs[ORBS_COUNT_KEY] ?: 3
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
            val disabledVersionsStr = prefs[DISABLED_VERSIONS_KEY] ?: ""
            viewModel.disabledVersions = if (disabledVersionsStr.isBlank()) emptySet()
            else disabledVersionsStr.split(",").toSet()
        }
    }
    SavePreference({ viewModel.disabledVersions.joinToString(",") }, DISABLED_VERSIONS_KEY, dataStore)
    SavePreference({ viewModel.renderOrbs }, RENDER_ORBS_KEY, dataStore)
    SavePreference({ viewModel.orbsCount }, ORBS_COUNT_KEY, dataStore)
    SavePreference({ viewModel.lightThemeReaderFontColor.toArgb() }, LIGHT_THEME_READER_FONT_COLOR_KEY, dataStore)
    SavePreference({ viewModel.darkThemeReaderFontColor.toArgb() }, DARK_THEME_READER_FONT_COLOR_KEY, dataStore)
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
    SavePreference({ viewModel.showStrongs }, SHOW_STRONGS_KEY, dataStore)
    SavePreference({ viewModel.isDictionaryMode }, IS_DICTIONARY_MODE_KEY, dataStore)
    SavePreference({ viewModel.verseMarkerColor.toArgb() }, VERSE_MARKER_COLOR_KEY, dataStore)
    SavePreference({ viewModel.lightModalBackgroundColor.toArgb() }, LIGHT_MODAL_BG_COLOR_KEY, dataStore)
    SavePreference({ viewModel.darkModalBackgroundColor.toArgb() }, DARK_MODAL_BG_COLOR_KEY, dataStore)
    SavePreference({ viewModel.wordsOfJesus.toArgb() }, WORDS_OF_JESUS_KEY, dataStore)
    SavePreference({ viewModel.headerButtonsColor.toArgb() }, HEADER_BUTTONS_KEY, dataStore)
    SavePreference({ viewModel.selectedPrimaryDictionary }, SELECTED_DICTIONARY_KEY, dataStore)
    SavePreference({ viewModel.selectedPrimaryDictLanguage }, PRIMARY_DICT_LANGUAGE_KEY, dataStore)
    SavePreference({ viewModel.selectedSecondaryDictLanguage }, SECONDARY_DICT_LANGUAGE_KEY, dataStore)
    SavePreference({ viewModel.selectedSecondaryDictionary }, SECONDARY_DICTIONARY_KEY, dataStore)
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

    LaunchedEffect(viewModel.selectedColor, viewModel.darkTheme, viewModel.isCustomColor) {
        viewModel.selectedColor?.let {
            ThemeManager.primaryColor = it
            ThemeManager.darkTheme = viewModel.darkTheme
            ThemeManager.isCustomColor = viewModel.isCustomColor
        }
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

    LaunchedEffect(viewModel.secondaryDbName) {
        secondaryDbHelper?.close()
        secondaryDbHelper = DatabaseHelper(activity, viewModel.secondaryDbName)
    }

    LaunchedEffect(animatorTemplateUri) {
        if (animatorTemplateUri != null) {
            if (viewModel.navigationStack.last() !is Screen.Animator) {
                viewModel.navigateTo(Screen.Animator)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            dbHelper?.close()
            secondaryDbHelper?.close()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.customTextureUri?.let { oldPath ->
                try { File(oldPath).delete() } catch (_: Exception) { }
            }
            val persistentPath = copyUriToInternalStorage(context, it)
            if (persistentPath != null) {
                viewModel.customTextureUri = persistentPath
                viewModel.bgImageIndex = 36
            }
        }
    }

    CompositionLocalProvider(LocalAppTheme provides themeState) {
        FohBibleTheme(darkTheme = viewModel.darkTheme) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(hInset, vInset, hInset, vInset),
                topBar = {
                    if ((currentScreen !is Screen.Reader || !viewModel.isReaderFullScreen) &&
                        (currentScreen !is Screen.Animator || !viewModel.isAnimatorFullScreen)) {
                        Surface(
                            modifier = Modifier.padding(horizontal = hInset),
                            color = Color.Transparent
                        ) {
                            if (currentScreen is Screen.Reader) {
                                ReaderAppBar(
                                    modifier = Modifier,
                                    currentScreen = currentScreen,
                                    currentVersionAbbr = viewModel.currentVersionAbbr,
                                    onBibleIconClick = { viewModel.showNavigationModal = true },
                                    onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                                    onColorLensClick = { viewModel.showColorThemeDialog = true },
                                    onScreenChange = { screen ->
                                        val targetScreen =
                                            if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                        viewModel.navigateTo(targetScreen)
                                    },
                                    onBack = if (viewModel.navigationStack.size > 1) {
                                        { viewModel.goBack() }
                                    } else null
                                )
                            } else {
                                HomeAppBar(
                                    currentScreen = currentScreen,
                                    onBibleIconClick = { viewModel.showNavigationModal = true },
                                    onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                                    onColorLensClick = { viewModel.showColorThemeDialog = true },
                                    onScreenChange = { screen ->
                                        val targetScreen =
                                            if (screen is Screen.Reader) Screen.Reader(viewModel.primaryPassage) else screen
                                        viewModel.navigateTo(targetScreen)
                                    },
                                    onBack = if (viewModel.navigationStack.size > 1) {
                                        { viewModel.goBack() }
                                    } else null,
                                    appViewModel = viewModel
                                )
                            }
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
                            databaseHelper = dbHelper,
                            onTakeBibleQuizClick = { viewModel.navigateTo(Screen.Quiz) },
                            onLearnHebrewClick = {viewModel.navigateTo(Screen.LearnHebrew)},
                            onLearnGreekClick = {viewModel.navigateTo(Screen.LearnGreek)},
                            onOpenDictionaryClick = { viewModel.navigateTo(Screen.Dictionary) },
                            onOpenAnimatorClick = { viewModel.navigateTo(Screen.Animator) }
                        )
                        is Screen.Reader -> {
                            val passage = currentScreen.passage ?: viewModel.primaryPassage
                            val onPassageChange: (PassageSelection) -> Unit = { newPassage ->
                                viewModel.navigationStack[viewModel.navigationStack.lastIndex] = Screen.Reader(newPassage)
                                viewModel.primaryPassage = newPassage
                                if (viewModel.scrollSync) viewModel.secondaryPassage = newPassage
                            }
                            key(
                                viewModel.darkThemeReaderFontColor, viewModel.lightThemeReaderFontColor, viewModel.wordsOfJesus
                            ) {
                                ReaderScreen(
                                    passage = passage,
                                    databaseHelper = dbHelper,
                                    onPassageChange = onPassageChange
                                )
                            }
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
                        Screen.Quiz -> BibleQuizScreen()
                        Screen.LearnHebrew -> LearnHebrewScreen()
                        Screen.LearnGreek -> LearnGreekScreen()
                        Screen.Dictionary -> DictionaryScreen(
                            onNavigateToReader = { passage ->
                                viewModel.primaryPassage = passage
                                if (viewModel.scrollSync) viewModel.secondaryPassage = passage
                                viewModel.navigateTo(Screen.Reader(passage))
                            }
                        )
                        Screen.Animator -> AnimatorScreen(
                            templateUriToLoad = animatorTemplateUri,
                            onTemplateConsumed = onAnimatorTemplateConsumed
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
                            isSecondary = false,
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
                            isSecondary = true,
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
                    if (viewModel.showLightOverlayColorWheel) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showLightOverlayColorWheel = false },
                            onColorSelected = { color ->
                                viewModel.lightOverlayColor = color
                                viewModel.showLightOverlayColorWheel = false
                            },
                            initialColor = viewModel.lightOverlayColor
                        )
                    }
                    if (viewModel.showDarkOverlayColorWheel) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showDarkOverlayColorWheel = false },
                            onColorSelected = { color ->
                                viewModel.darkOverlayColor = color
                                viewModel.showDarkOverlayColorWheel = false
                            },
                            initialColor = viewModel.darkOverlayColor
                        )
                    }
                    if (viewModel.showColorThemeDialog) {
                        Dialog(onDismissRequest = { viewModel.showColorThemeDialog = false }) {
                            ColorThemeDialog(
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
                            initialColor = if (viewModel.isCustomColor && viewModel.customColor != null) viewModel.customColor!! else viewModel.selectedColor
                                ?: ThemeManager.primaryColor
                        )
                    }
                    if (viewModel.showBgModal) {
                        BgModal(
                            currentIndex = viewModel.bgImageIndex,
                            customUri = viewModel.customTextureUri,
                            onSelect = { index ->
                                viewModel.bgImageIndex = index
                                viewModel.renderOrbs = (index == 35)
                                viewModel.showBgModal = false
                            },
                            onDismiss = { viewModel.showBgModal = false },
                            onPickCustom = { imagePickerLauncher.launch("image/*") },
                            onRemoveCustom = {
                                viewModel.customTextureUri?.let { path ->
                                    try { File(path).delete() } catch (_: Exception) { }
                                }
                                viewModel.customTextureUri = null
                                viewModel.bgImageIndex = 0
                            }
                        )
                    }
                    if (viewModel.showReaderOverlayColorWheel) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showReaderOverlayColorWheel = false },
                            onColorSelected = { color ->
                                if (viewModel.darkTheme) viewModel.darkOverlayColor =
                                    color else viewModel.lightOverlayColor = color
                                viewModel.showReaderOverlayColorWheel = false
                            },
                            initialColor = if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor
                        )
                    }
                    if (viewModel.showWordMarkerColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showWordMarkerColorWheelDialog = false },
                            onColorSelected = { color ->
                                viewModel.wordMarkerColor =
                                    color; viewModel.showWordMarkerColorWheelDialog = false
                            },
                            initialColor = viewModel.wordMarkerColor
                        )
                    }
                    if (viewModel.showJesusWordsColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showJesusWordsColorWheelDialog = false },
                            onColorSelected = { color ->
                                viewModel.wordsOfJesus =
                                    color; viewModel.showJesusWordsColorWheelDialog = false
                            },
                            initialColor = viewModel.wordsOfJesus
                        )
                    }
                    if (viewModel.showLightReaderFontColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = {
                                viewModel.showLightReaderFontColorWheelDialog = false
                            },
                            onColorSelected = { color ->
                                viewModel.lightThemeReaderFontColor =
                                    color; viewModel.showLightReaderFontColorWheelDialog = false
                            },
                            initialColor = viewModel.lightThemeReaderFontColor
                        )
                    }
                    if (viewModel.showDarkReaderFontColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = {
                                viewModel.showDarkReaderFontColorWheelDialog = false
                            },
                            onColorSelected = { color ->
                                viewModel.darkThemeReaderFontColor =
                                    color; viewModel.showDarkReaderFontColorWheelDialog = false
                            },
                            initialColor = viewModel.darkThemeReaderFontColor
                        )
                    }
                    if (viewModel.showVerseMarkerColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = {
                                viewModel.showVerseMarkerColorWheelDialog = false
                            },
                            onColorSelected = { color ->
                                viewModel.verseMarkerColor =
                                    color; viewModel.showVerseMarkerColorWheelDialog = false
                            },
                            initialColor = viewModel.verseMarkerColor
                        )
                    }
                    if (viewModel.showVersionInfoDialog && viewModel.versionInfoForDialog.isNotEmpty()) {
                        LaunchedEffect(viewModel.versionInfoForDialog) {
                            isLoadingVersionInfo = true
                            bibleInfoData = null
                            withContext(Dispatchers.IO) {
                                val info = BibleVersionInfoRepository.getVersionInfo(context, viewModel.versionInfoForDialog)
                                withContext(Dispatchers.Main) {
                                    bibleInfoData = info
                                    isLoadingVersionInfo = false
                                }
                            }
                        }

                        BibleVersionInfoDialog(
                            showDialog = viewModel.showVersionInfoDialog,
                            onDismiss = {
                                viewModel.showVersionInfoDialog = false
                                bibleInfoData = null
                            },
                            isLoading = isLoadingVersionInfo,
                            versionInfo = bibleInfoData?.let {
                                BibleVersionInfo(description = it.description, detailedInfo = it.detailedInfo)
                            },
                            titleTextStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = viewModel.fontSize.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

val allScreens = listOf(
    "Home" to Icons.Filled.Home,
    "Reader" to Icons.Filled.Book,
    "Bookmarks" to Icons.Filled.Bookmark,
    "Notes" to Icons.AutoMirrored.Filled.Note,
    "Search" to Icons.Filled.Search,
    "Settings" to Icons.Filled.Settings
)

sealed class Screen {
    object Home : Screen()
    data class Reader(val passage: PassageSelection? = null) : Screen()
    object Bookmarks : Screen()
    object Notes : Screen()
    object Settings : Screen()
    object Search : Screen()
    object Quiz : Screen()
    object LearnHebrew : Screen()
    object LearnGreek : Screen()
    object Dictionary : Screen()
    object Animator : Screen()
}

private fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
        val fileName = "custom_bg_${System.currentTimeMillis()}.jpg"
        val destFile = File(context.filesDir, fileName)
        FileOutputStream(destFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}