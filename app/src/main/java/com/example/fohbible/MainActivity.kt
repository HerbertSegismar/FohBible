@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.fohbible

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.modals.FontModal
import com.example.fohbible.modals.NavigationModal
import com.example.fohbible.models.AppViewModel
import com.example.fohbible.screens.BgModal
import com.example.fohbible.screens.BookmarksScreen
import com.example.fohbible.screens.HomeScreen
import com.example.fohbible.screens.NotesScreen
import com.example.fohbible.screens.ReaderScreen
import com.example.fohbible.screens.SearchScreen
import com.example.fohbible.screens.SettingsScreen
import com.example.fohbible.screens.getFontFamily
import com.example.fohbible.ui.theme.AppThemeState
import com.example.fohbible.ui.theme.ColorTheme
import com.example.fohbible.ui.theme.DefaultPrimaryColor
import com.example.fohbible.ui.theme.FohBibleTheme
import com.example.fohbible.ui.theme.LocalAppTheme
import com.example.fohbible.ui.theme.PredefinedColorThemes
import com.example.fohbible.ui.theme.ThemeManager
import com.example.fohbible.utils.BibleVersionUtils
import com.example.fohbible.utils.BibleVersionUtils.descriptionMap
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

// ====================== DATASTORE KEYS ======================
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
fun FohBibleApp(activity: MainActivity, viewModel: AppViewModel) {
    val currentScreen = viewModel.navigationStack.last()

    var isUsingCustomColor by remember { mutableStateOf(viewModel.isCustomColor) }
    var customColor by remember { mutableStateOf(viewModel.customColor) }

    val dataStore = remember { activity.appDataStore }

    LaunchedEffect(Unit) {
        val prefs = dataStore.data.first()

        viewModel.fontSize = prefs[FONT_SIZE_KEY] ?: 18
        viewModel.darkTheme = prefs[DARK_THEME_KEY] ?: false
        viewModel.selectedColor = Color(prefs[SELECTED_COLOR_KEY] ?: DefaultPrimaryColor.toArgb())
        viewModel.isCustomColor = prefs[IS_CUSTOM_COLOR_KEY] ?: false
        viewModel.customColor = Color(prefs[CUSTOM_COLOR_KEY] ?: DefaultPrimaryColor.toArgb())
        viewModel.selectedFontFamily = prefs[FONT_FAMILY_KEY] ?: "system"
        viewModel.currentDbName = prefs[PRIMARY_DB_KEY] ?: "kj2.sqlite3"
        viewModel.currentVersionAbbr = prefs[PRIMARY_ABBR_KEY] ?: BibleVersionUtils.versionMap["kj2.sqlite3"]!!
        viewModel.multiVersion = prefs[MULTI_VERSION_KEY] ?: false
        viewModel.secondaryDbName = prefs[SECONDARY_DB_KEY] ?: "esv.sqlite3"
        viewModel.secondaryVersionAbbr = prefs[SECONDARY_ABBR_KEY] ?: BibleVersionUtils.versionMap["esv.sqlite3"]!!
        viewModel.multiViewLayout = prefs[MULTI_LAYOUT_KEY] ?: "horizontal"
        viewModel.scrollSync = prefs[SCROLL_SYNC_KEY] ?: true
        viewModel.bgImageIndex = prefs[BG_INDEX_KEY] ?: 0
        viewModel.customTextureUri = prefs[CUSTOM_TEXTURE_KEY]
        viewModel.overlayOpacity = prefs[OVERLAY_OPACITY_KEY] ?: 0.15f
        viewModel.lightOverlayColor = Color(prefs[LIGHT_OVERLAY_COLOR_KEY] ?: Color(0xFFF5F5DC).toArgb())
        viewModel.darkOverlayColor = Color(prefs[DARK_OVERLAY_COLOR_KEY] ?: Color(0xFF100F21).toArgb())

        viewModel.primaryPassage = PassageSelection(
            bookNumber = prefs[PRIMARY_BOOK_NUMBER_KEY] ?: 10,
            bookName = prefs[PRIMARY_BOOK_NAME_KEY] ?: "Genesis",
            chapter = prefs[PRIMARY_CHAPTER_KEY] ?: 1,
            verse = prefs[PRIMARY_VERSE_KEY] ?: 1
        )
        viewModel.secondaryPassage = PassageSelection(
            bookNumber = prefs[SECONDARY_BOOK_NUMBER_KEY] ?: 10,
            bookName = prefs[SECONDARY_BOOK_NAME_KEY] ?: "Genesis",
            chapter = prefs[SECONDARY_CHAPTER_KEY] ?: 1,
            verse = prefs[SECONDARY_VERSE_KEY] ?: 1
        )
    }

    // ==================== SAVE ALL PREFERENCES ====================
    LaunchedEffect(Unit) { snapshotFlow { viewModel.fontSize }.collectLatest { dataStore.edit { prefs -> prefs[FONT_SIZE_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.darkTheme }.collectLatest { dataStore.edit { prefs -> prefs[DARK_THEME_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.selectedColor }.collectLatest { dataStore.edit { prefs -> prefs[SELECTED_COLOR_KEY] = it?.toArgb() ?: DefaultPrimaryColor.toArgb() } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.isCustomColor }.collectLatest { dataStore.edit { prefs -> prefs[IS_CUSTOM_COLOR_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.customColor }.collectLatest { dataStore.edit { prefs -> prefs[CUSTOM_COLOR_KEY] = it?.toArgb() ?: DefaultPrimaryColor.toArgb() } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.selectedFontFamily }.collectLatest { dataStore.edit { prefs -> prefs[FONT_FAMILY_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.currentDbName }.collectLatest { dataStore.edit { prefs -> prefs[PRIMARY_DB_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.currentVersionAbbr }.collectLatest { dataStore.edit { prefs -> prefs[PRIMARY_ABBR_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.multiVersion }.collectLatest { dataStore.edit { prefs -> prefs[MULTI_VERSION_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.secondaryDbName }.collectLatest { dataStore.edit { prefs -> prefs[SECONDARY_DB_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.secondaryVersionAbbr }.collectLatest { dataStore.edit { prefs -> prefs[SECONDARY_ABBR_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.multiViewLayout }.collectLatest { dataStore.edit { prefs -> prefs[MULTI_LAYOUT_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.scrollSync }.collectLatest { dataStore.edit { prefs -> prefs[SCROLL_SYNC_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.bgImageIndex }.collectLatest { dataStore.edit { prefs -> prefs[BG_INDEX_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.customTextureUri }.collectLatest { uri ->
        dataStore.edit { prefs -> if (uri != null) { prefs[CUSTOM_TEXTURE_KEY] = uri } else { prefs.remove(CUSTOM_TEXTURE_KEY) } }
    } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.overlayOpacity }.collectLatest { dataStore.edit { prefs -> prefs[OVERLAY_OPACITY_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.lightOverlayColor.toArgb() }.collectLatest { dataStore.edit { prefs -> prefs[LIGHT_OVERLAY_COLOR_KEY] = it } } }
    LaunchedEffect(Unit) { snapshotFlow { viewModel.darkOverlayColor.toArgb() }.collectLatest { dataStore.edit { prefs -> prefs[DARK_OVERLAY_COLOR_KEY] = it } } }

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
                                    val targetScreen = when (screen) {
                                        is Screen.Reader -> Screen.Reader(viewModel.primaryPassage)
                                        else -> screen
                                    }
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
                                    val targetScreen = when (screen) {
                                        is Screen.Reader -> Screen.Reader(viewModel.primaryPassage)
                                        else -> screen
                                    }
                                    viewModel.navigateTo(targetScreen)
                                },
                                onBack = if (viewModel.navigationStack.size > 1) { { viewModel.goBack() } } else null
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
                    BackHandler(enabled = viewModel.navigationStack.size > 1) {
                        viewModel.goBack()
                    }

                    when (currentScreen) {
                        Screen.Home -> {
                            HomeScreen(
                                modifier = Modifier.fillMaxSize(),
                                onNavigateToReader = { passage ->
                                    viewModel.primaryPassage = passage
                                    if (viewModel.scrollSync) {
                                        viewModel.secondaryPassage = passage
                                    }
                                    viewModel.navigateTo(Screen.Reader(passage))
                                },
                                onNavigateToScreen = { screen ->
                                    when (screen) {
                                        is Screen.Reader -> {
                                            viewModel.navigateTo(Screen.Reader(viewModel.primaryPassage))
                                        }
                                        else -> viewModel.navigateTo(screen)
                                    }
                                },
                                databaseHelper = dbHelper
                            )
                        }
                        is Screen.Reader -> {
                            ReaderScreen(
                                passage = currentScreen.passage ?: viewModel.primaryPassage,
                                databaseHelper = dbHelper,
                                onPassageChange = { newPassage ->
                                    viewModel.navigationStack[viewModel.navigationStack.lastIndex] = Screen.Reader(newPassage)
                                    viewModel.primaryPassage = newPassage
                                    if (viewModel.scrollSync) {
                                        viewModel.secondaryPassage = newPassage
                                    }
                                }
                            )
                        }
                        Screen.Bookmarks -> BookmarksScreen(
                            onNavigateToReader = { passage ->
                                viewModel.primaryPassage = passage
                                if (viewModel.scrollSync) {
                                    viewModel.secondaryPassage = passage
                                }
                                viewModel.navigateTo(Screen.Reader(passage))
                            },
                        )
                        Screen.Notes -> NotesScreen(
                            onNavigateToReader = { passage ->
                                viewModel.primaryPassage = passage
                                if (viewModel.scrollSync) {
                                    viewModel.secondaryPassage = passage
                                }
                                viewModel.navigateTo(Screen.Reader(passage))
                            }
                        )
                        Screen.Settings -> SettingsScreen()
                        Screen.Search -> {
                            SearchScreen(
                                databaseHelper = dbHelper,
                                onPassageSelected = { passage ->
                                    viewModel.primaryPassage = passage
                                    if (viewModel.scrollSync) {
                                        viewModel.secondaryPassage = passage
                                    }
                                    if (viewModel.navigationStack.size > 1 && viewModel.navigationStack[viewModel.navigationStack.size - 2] is Screen.Reader) {
                                        viewModel.navigationStack[viewModel.navigationStack.size - 2] = Screen.Reader(passage)
                                        viewModel.goBack()
                                    } else {
                                        viewModel.goBack()
                                        viewModel.navigateTo(Screen.Reader(passage))
                                    }
                                }
                            )
                        }
                    }

                    if (viewModel.showNavigationModal) {
                        NavigationModal(
                            showNavigationModal = true,
                            onDismissRequest = { viewModel.showNavigationModal = false },
                            onPassageSelected = { passage ->
                                viewModel.primaryPassage = passage
                                if (viewModel.scrollSync) {
                                    viewModel.secondaryPassage = passage
                                }
                                viewModel.showNavigationModal = false
                                viewModel.updateCurrentScreen(Screen.Reader(passage))
                            },
                            databaseHelper = dbHelper
                        )
                    }
                    if (viewModel.showSecondaryNavigationModal) {
                        NavigationModal(
                            showNavigationModal = true,
                            onDismissRequest = { viewModel.showSecondaryNavigationModal = false },
                            onPassageSelected = { passage ->
                                viewModel.secondaryPassage = passage
                                viewModel.showSecondaryNavigationModal = false
                            },
                            databaseHelper = dbHelper
                        )
                    }
                    if (viewModel.showPrimaryVersionDropdown) {
                        Dialog(onDismissRequest = { viewModel.showPrimaryVersionDropdown = false }) {
                            VersionSelectionDialog(
                                onDismiss = { viewModel.showPrimaryVersionDropdown = false },
                                onVersionSelected = { file, abbr ->
                                    viewModel.currentDbName = file
                                    viewModel.currentVersionAbbr = abbr
                                },
                                currentAbbr = viewModel.currentVersionAbbr,
                                versionMap = BibleVersionUtils.versionMap,
                                descriptionMap = descriptionMap
                            )
                        }
                    }
                    if (viewModel.showSecondaryVersionDropdown) {
                        Dialog(onDismissRequest = { viewModel.showSecondaryVersionDropdown = false }) {
                            VersionSelectionDialog(
                                onDismiss = { viewModel.showSecondaryVersionDropdown = false },
                                onVersionSelected = { file, abbr ->
                                    viewModel.secondaryDbName = file
                                    viewModel.secondaryVersionAbbr = abbr
                                },
                                currentAbbr = viewModel.secondaryVersionAbbr,
                                versionMap = BibleVersionUtils.versionMap,
                                descriptionMap = descriptionMap
                            )
                        }
                    }
                    if (viewModel.showColorThemeDialog) {
                        Dialog(
                            onDismissRequest = { viewModel.showColorThemeDialog = false }
                        ) {
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
                                }
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
                            onSelect = { index ->
                                viewModel.bgImageIndex = index
                                viewModel.showBgModal = false
                            },
                            onDismiss = { viewModel.showBgModal = false },
                            onPickCustom = { imagePickerLauncher.launch("image/*") },
                            onRemoveCustom = { viewModel.customTextureUri = null }
                        )
                    }
                    if (viewModel.showReaderOverlayColorWheel) {
                        ColorWheelDialog(
                            onDismissRequest = { viewModel.showReaderOverlayColorWheel = false },
                            onColorSelected = { color ->
                                if (viewModel.darkTheme) {
                                    viewModel.darkOverlayColor = color
                                } else {
                                    viewModel.lightOverlayColor = color
                                }
                                viewModel.showReaderOverlayColorWheel = false
                            },
                            initialColor = if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VersionSelectionDialog(
    onDismiss: () -> Unit,
    onVersionSelected: (String, String) -> Unit,
    currentAbbr: String,
    versionMap: Map<String, String>,
    descriptionMap: Map<String, String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose Bible Version",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(versionMap.entries.toList()) { entry ->
                    val file = entry.key
                    val abbr = entry.value
                    val desc = descriptionMap[file] ?: "Bible translation"
                    val isActive = abbr == currentAbbr
                    val backgroundColor by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = tween(durationMillis = 200)
                    )
                    val textColor by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 200)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onVersionSelected(file, abbr)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = backgroundColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = abbr,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun UpdatedColorThemeDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose Theme Color",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(PredefinedColorThemes.chunked(1)) { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowThemes.forEach { theme ->
                            ColorOptionItem(
                                theme = theme,
                                onClick = {
                                    onColorSelected(theme.primaryColor)
                                    onDismiss()
                                }
                            )
                        }
                        if (rowThemes.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Custom Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable(onClick = onCustomColorClick),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                Color.Red,
                                                Color.Yellow,
                                                Color.Green,
                                                Color.Cyan,
                                                Color.Blue,
                                                Color.Magenta,
                                                Color.Red
                                            )
                                        )
                                    )
                                    .border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Custom Color Picker",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Choose any color with color wheel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun ColorOptionItem(
    theme: ColorTheme,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.primaryColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(theme.primaryColor, theme.secondaryColor)
                            )
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Primary & Secondary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeAppBar(
    currentScreen: Screen,
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScreenChange: (Screen) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var showNavigationDropdown by remember { mutableStateOf(false) }
    val viewModel: AppViewModel = viewModel()
    val rotation by animateFloatAsState(
        targetValue = if (showNavigationDropdown) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "menuIconRotation"
    )
    var bibleTargetRotation by remember { mutableFloatStateOf(0f) }
    val bibleAnimatedRotation by animateFloatAsState(
        targetValue = bibleTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "bibleRotation"
    )
    var themeTargetRotation by remember { mutableFloatStateOf(0f) }
    val themeAnimatedRotation by animateFloatAsState(
        targetValue = themeTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "themeRotation"
    )
    var colorTargetRotation by remember { mutableFloatStateOf(0f) }
    val colorAnimatedRotation by animateFloatAsState(
        targetValue = colorTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "colorRotation"
    )
    var backTargetRotation by remember { mutableFloatStateOf(0f) }
    val backAnimatedRotation by animateFloatAsState(
        targetValue = backTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "backRotation"
    )
    val screenTitle = when (currentScreen) {
        is Screen.Home -> "Home"
        is Screen.Reader -> "Reader"
        is Screen.Bookmarks -> "Bookmarks"
        is Screen.Notes -> "Notes"
        is Screen.Settings -> "Settings"
        is Screen.Search -> "Search"
    }
    TopAppBar(
        title = {
            Text(
                text = screenTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 0.dp),
                textAlign = TextAlign.Start
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = LocalAppTheme.current.primaryColor
        ),
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.rotate(backAnimatedRotation)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = {
                bibleTargetRotation += 360f
                onBibleIconClick()
            }) {
                Icon(
                    Icons.Filled.Book,
                    contentDescription = "Bible Navigation",
                    tint = Color.White,
                    modifier = Modifier.rotate(bibleAnimatedRotation)
                )
            }
            IconButton(onClick = {
                themeTargetRotation += 180f
                onThemeToggle()
            }) {
                Icon(
                    if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2,
                    contentDescription = "Toggle Theme",
                    tint = Color.White,
                    modifier = Modifier.rotate(themeAnimatedRotation)
                )
            }
            IconButton(onClick = {
                colorTargetRotation += 180f
                onColorLensClick()
            }) {
                Icon(
                    Icons.Filled.ColorLens,
                    contentDescription = "Color Scheme",
                    tint = Color.White,
                    modifier = Modifier.rotate(colorAnimatedRotation)
                )
            }
            IconButton(
                onClick = { showNavigationDropdown = !showNavigationDropdown },
                modifier = Modifier.rotate(rotation)
            ) {
                Crossfade(
                    targetState = showNavigationDropdown,
                    animationSpec = tween(durationMillis = 300),
                    label = "iconCrossfade"
                ) { isOpen ->
                    Icon(
                        imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Menu,
                        contentDescription = if (isOpen) "Close Navigation" else "Open Navigation",
                        tint = Color.White
                    )
                }
            }
            DropdownMenu(
                expanded = showNavigationDropdown,
                onDismissRequest = { showNavigationDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                @Composable
                fun createDropdownItem(
                    title: String,
                    icon: ImageVector,
                    isActive: Boolean,
                    onClick: () -> Unit
                ) {
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "dropdownBackground"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 200),
                        label = "dropdownTextColor"
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        },
                        onClick = onClick,
                        modifier = Modifier.background(backgroundColor),
                        leadingIcon = {
                            val modifier = if (title == "Notes") Modifier.rotate(90f) else Modifier
                            Icon(
                                icon,
                                contentDescription = title,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = modifier
                            )
                        }
                    )
                }
                val isHomeActive = currentScreen is Screen.Home
                val isReaderActive = currentScreen is Screen.Reader
                val isBookmarksActive = currentScreen == Screen.Bookmarks
                val isNotesActive = currentScreen == Screen.Notes
                val isSearchActive = currentScreen == Screen.Search
                val isSettingsActive = currentScreen == Screen.Settings
                createDropdownItem("Home", Icons.Filled.Home, isHomeActive) {
                    onScreenChange(Screen.Home)
                    showNavigationDropdown = false
                }
                createDropdownItem("Reader", Icons.Filled.Book, isReaderActive) {
                    onScreenChange(Screen.Reader())
                    showNavigationDropdown = false
                }
                createDropdownItem("Bookmarks", Icons.Filled.Bookmark, isBookmarksActive) {
                    onScreenChange(Screen.Bookmarks)
                    showNavigationDropdown = false
                }
                createDropdownItem("Notes", Icons.AutoMirrored.Filled.Note, isNotesActive) {
                    onScreenChange(Screen.Notes)
                    showNavigationDropdown = false
                }
                createDropdownItem("Search", Icons.Filled.Search, isSearchActive) {
                    onScreenChange(Screen.Search)
                    showNavigationDropdown = false
                }
                createDropdownItem("Settings", Icons.Filled.Settings, isSettingsActive) {
                    onScreenChange(Screen.Settings)
                    showNavigationDropdown = false
                }
            }
        }
    )
}

@Composable
fun ReaderAppBar(
    currentScreen: Screen.Reader,
    currentVersionAbbr: String,
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScreenChange: (Screen) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val viewModel: AppViewModel = viewModel()
    var showNavigationDropdown by remember { mutableStateOf(false) }
    var showMultiDropdown by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var tempFontSize by remember { mutableStateOf(viewModel.fontSize.toString()) }
    val rotation by animateFloatAsState(
        targetValue = if (showNavigationDropdown) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "menuIconRotation"
    )
    val multiRotation by animateFloatAsState(
        targetValue = if (showMultiDropdown) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "multiIconRotation"
    )
    var themeTargetRotation by remember { mutableFloatStateOf(0f) }
    val themeAnimatedRotation by animateFloatAsState(
        targetValue = themeTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "themeRotation"
    )
    var colorTargetRotation by remember { mutableFloatStateOf(0f) }
    val colorAnimatedRotation by animateFloatAsState(
        targetValue = colorTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "colorRotation"
    )
    var backTargetRotation by remember { mutableFloatStateOf(0f) }
    val backAnimatedRotation by animateFloatAsState(
        targetValue = backTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "backRotation"
    )
    var syncTargetRotation by remember { mutableFloatStateOf(0f) }
    val syncAnimatedRotation by animateFloatAsState(
        targetValue = syncTargetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "syncRotation"
    )
    val minFontSize = 8
    val maxFontSize = 50
    TopAppBar(
        title = {
            if (!viewModel.multiVersion) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBibleIconClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .weight(0.7f)
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentScreen.passage?.bookName ?: "Reader",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = currentScreen.passage?.chapter?.let { " $it" } ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.showPrimaryVersionDropdown = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .weight(0.5f)
                            .padding(end = if (onBack == null) 8.dp else 2.dp)
                    ) {
                        Text(
                            text = currentVersionAbbr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                    if (viewModel.multiVersion) {
                        Button(
                            onClick = { viewModel.showSecondaryVersionDropdown = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .padding(end = 0.dp)
                        ) {
                            Text(
                                text = viewModel.secondaryVersionAbbr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = LocalAppTheme.current.primaryColor
        ),
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.rotate(backAnimatedRotation)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    themeTargetRotation += 180f
                    onThemeToggle()
                },
                modifier = Modifier.size(40.dp).padding(start = 4.dp)
            ) {
                Icon(
                    if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2,
                    contentDescription = "Toggle Theme",
                    tint = Color.White,
                    modifier = Modifier.rotate(themeAnimatedRotation)
                )
            }
            IconButton(
                onClick = {
                    colorTargetRotation += 180f
                    onColorLensClick()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.ColorLens,
                    contentDescription = "Color Scheme",
                    tint = Color.White,
                    modifier = Modifier.rotate(colorAnimatedRotation)
                )
            }
            if (viewModel.multiVersion) {
                IconButton(
                    onClick = {
                        syncTargetRotation += 180f
                        viewModel.scrollSync = !viewModel.scrollSync
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (viewModel.scrollSync) Icons.Filled.Link else Icons.Filled.LinkOff,
                        contentDescription = "Toggle Scroll Sync",
                        tint = Color.White,
                        modifier = Modifier.rotate(syncAnimatedRotation)
                    )
                }
            }
            IconButton(
                onClick = { showMultiDropdown = !showMultiDropdown },
                modifier = Modifier
                    .size(40.dp)
                    .rotate(multiRotation)
            ) {
                Crossfade(
                    targetState = showMultiDropdown,
                    animationSpec = tween(durationMillis = 300),
                    label = "multiIconCrossfade"
                ) { isOpen ->
                    Icon(
                        imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.AutoAwesomeMosaic,
                        contentDescription = if (isOpen) "Close MultiView" else "MultiView",
                        tint = Color.White
                    )
                }
            }
            DropdownMenu(
                expanded = showMultiDropdown,
                onDismissRequest = { showMultiDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                offset = DpOffset(x = 100.dp, y = 0.dp)
            ) {
                Text(
                    text = "Windows Layout",
                    modifier = Modifier.fillMaxWidth().height(25.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider()
                val current = if (!viewModel.multiVersion) "single" else viewModel.multiViewLayout
                @Composable
                fun createItem(title: String, onClick: () -> Unit) {
                    val isActive = title.lowercase() == current
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    val leadingIcon: (@Composable () -> Unit) = {
                        when (title.lowercase()) {
                            "single" -> Icon(
                                Icons.Default.LooksOne,
                                contentDescription = null,
                                tint = textColor
                            )
                            "horizontal" -> Icon(
                                Icons.Default.ViewStream,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.rotate(90f)
                            )
                            "vertical" -> Icon(
                                Icons.Default.ViewStream,
                                contentDescription = null,
                                tint = textColor
                            )
                            else -> Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = textColor
                            )
                        }
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            onClick()
                            showMultiDropdown = false
                        },
                        leadingIcon = leadingIcon,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
                createItem("Single") { viewModel.multiVersion = false }
                createItem("Horizontal") {
                    viewModel.multiVersion = true
                    viewModel.multiViewLayout = "horizontal"
                }
                createItem("Vertical") {
                    viewModel.multiVersion = true
                    viewModel.multiViewLayout = "vertical"
                }
            }
            IconButton(
                onClick = { showNavigationDropdown = !showNavigationDropdown },
                modifier = Modifier
                    .size(40.dp)
                    .rotate(rotation)
            ) {
                Crossfade(
                    targetState = showNavigationDropdown,
                    animationSpec = tween(durationMillis = 300),
                    label = "iconCrossfade"
                ) { isOpen ->
                    Icon(
                        imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Menu,
                        contentDescription = if (isOpen) "Close Navigation" else "Open Navigation",
                        tint = Color.White
                    )
                }
            }
            DropdownMenu(
                expanded = showNavigationDropdown,
                onDismissRequest = { showNavigationDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                @Composable
                fun createDropdownItem(
                    title: String,
                    icon: ImageVector,
                    isActive: Boolean,
                    onClick: () -> Unit
                ) {
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "dropdownBackground"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 200),
                        label = "dropdownTextColor"
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        },
                        onClick = onClick,
                        modifier = Modifier.background(backgroundColor),
                        leadingIcon = {
                            val modifier = if (title == "Notes") Modifier.rotate(90f) else Modifier
                            Icon(
                                icon,
                                contentDescription = title,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = modifier
                            )
                        }
                    )
                }
                val isHomeActive = false
                val isReaderActive = true
                val isBookmarksActive = false
                val isNotesActive = false
                val isSearchActive = false
                val isSettingsActive = false
                createDropdownItem("Home", Icons.Filled.Home, isHomeActive) {
                    onScreenChange(Screen.Home)
                    showNavigationDropdown = false
                }
                createDropdownItem("Reader", Icons.Filled.Book, isReaderActive) {
                    onScreenChange(Screen.Reader())
                    showNavigationDropdown = false
                }
                createDropdownItem("Bookmarks", Icons.Filled.Bookmark, isBookmarksActive) {
                    onScreenChange(Screen.Bookmarks)
                    showNavigationDropdown = false
                }
                createDropdownItem("Notes", Icons.AutoMirrored.Filled.Note, isNotesActive) {
                    onScreenChange(Screen.Notes)
                    showNavigationDropdown = false
                }
                createDropdownItem("Search", Icons.Filled.Search, isSearchActive) {
                    onScreenChange(Screen.Search)
                    showNavigationDropdown = false
                }
                createDropdownItem("Settings", Icons.Filled.Settings, isSettingsActive) {
                    onScreenChange(Screen.Settings)
                    showNavigationDropdown = false
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Background Texture",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Texture,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        viewModel.showBgModal = true
                        showNavigationDropdown = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overlay Opacity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${(viewModel.overlayOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = viewModel.overlayOpacity,
                        onValueChange = { viewModel.overlayOpacity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            inactiveTickColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .shadow(2.dp, shape = CircleShape)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    )
                            )
                        },
                        onValueChangeFinished = { }
                    )
                    Text(
                        text = "Adjust Overlay Opacity with Slider",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        fontFamily = getFontFamily("oswald"),
                        fontSize = 12.sp
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (viewModel.darkTheme) "Dark Overlay Color" else "Light Overlay Color",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    },
                    onClick = {
                        viewModel.showReaderOverlayColorWheel = true
                        showNavigationDropdown = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Font Size",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "8-50",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.fontSize = maxOf(minFontSize, viewModel.fontSize - 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${viewModel.fontSize}",
                            modifier = Modifier.padding(horizontal = 16.dp).clickable { showFontSizeDialog = true },
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.fontSize = minOf(maxFontSize, viewModel.fontSize + 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
    if (showFontSizeDialog) {
        FontModal(
            tempSize = tempFontSize,
            onChange = { tempFontSize = it },
            onConfirm = {
                val newSize = tempFontSize.toIntOrNull()?.coerceIn(minFontSize, maxFontSize) ?: viewModel.fontSize
                viewModel.fontSize = newSize
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false }
        )
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