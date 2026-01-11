@file:Suppress("VariableNeverRead", "AssignedValueIsNeverRead")

package com.example.fohbible

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.screens.BookmarksScreen
import com.example.fohbible.screens.HomeScreen
import com.example.fohbible.screens.ReaderScreen
import com.example.fohbible.screens.SearchScreen
import com.example.fohbible.screens.SettingsScreen
import com.example.fohbible.ui.theme.AppThemeState
import com.example.fohbible.ui.theme.ColorTheme
import com.example.fohbible.ui.theme.DefaultPrimaryColor
import com.example.fohbible.ui.theme.FohBibleTheme
import com.example.fohbible.ui.theme.LocalAppTheme
import com.example.fohbible.ui.theme.PredefinedColorThemes
import com.example.fohbible.ui.theme.ThemeManager
import com.example.fohbible.utils.BibleVersionUtils
import com.example.fohbible.utils.BibleVersionUtils.descriptionMap

class AppViewModel : ViewModel() {
    var showSecondaryNavigationModal by mutableStateOf(false)
    var lastNavigationWasPrimary by mutableStateOf(true)
    var currentNavigationPassage by mutableStateOf<PassageSelection?>(null)
    var fontSize by mutableIntStateOf(18)
    var darkTheme by mutableStateOf(false)
    var selectedColor by mutableStateOf<Color?>(null)
    var isCustomColor by mutableStateOf(false)
    var selectedFontFamily by mutableStateOf("system")
    val navigationStack = mutableStateListOf<Screen>(Screen.Home)
    var currentDbName by mutableStateOf("kj2.sqlite3")
    var currentVersionAbbr by mutableStateOf(BibleVersionUtils.versionMap["kj2.sqlite3"]!!)
    // New: Track custom color separately
    var customColor by mutableStateOf<Color?>(null)
    // Multi-version fields
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

    fun navigateTo(screen: Screen) {
        navigationStack.add(screen)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun goBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
        }
    }

    fun updateCurrentScreen(newScreen: Screen) {
        if (navigationStack.isNotEmpty()) {
            navigationStack[navigationStack.lastIndex] = newScreen
        }
    }
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            FohBibleApp(this, viewModel)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Suppress("AssignedValueIsNeverRead")
@Composable
fun FohBibleApp(activity: MainActivity, viewModel: AppViewModel) {
    val currentScreen = viewModel.navigationStack.last()
    var isUsingCustomColor by remember { mutableStateOf(viewModel.isCustomColor) }
    var customColor by remember { mutableStateOf(viewModel.customColor) }

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
        onDispose {
            dbHelper?.close()
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
                                        is Screen.Reader -> Screen.Reader(
                                            PassageSelection(
                                                bookNumber = 10,
                                                bookName = "Genesis",
                                                chapter = 1,
                                                verse = 1,
                                            )
                                        )
                                        else -> screen
                                    }
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
                                    val targetScreen = when (screen) {
                                        is Screen.Reader -> Screen.Reader(
                                            PassageSelection(
                                                bookNumber = 10,
                                                bookName = "Genesis",
                                                chapter = 1,
                                                verse = 1,
                                            )
                                        )
                                        else -> screen
                                    }
                                    viewModel.navigateTo(targetScreen)
                                },
                                onBack = if (viewModel.navigationStack.size > 1) {
                                    { viewModel.goBack() }
                                } else null
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
                                onBibleClick = { viewModel.showNavigationModal = true },
                                databaseHelper = dbHelper
                            )
                        }
                        is Screen.Reader -> {
                            val passage = currentScreen.passage ?: PassageSelection(
                                bookNumber = 10,
                                bookName = "Genesis",
                                chapter = 1,
                                verse = 1
                            )
                            ReaderScreen(
                                passage = passage,
                                databaseHelper = dbHelper,
                                onPassageChange = { newPassage ->
                                    viewModel.updateCurrentScreen(Screen.Reader(newPassage))
                                }
                            )
                        }
                        Screen.Bookmarks -> BookmarksScreen()
                        Screen.Search -> SearchScreen(databaseHelper = dbHelper)
                        Screen.Settings -> SettingsScreen()
                    }

                    if (viewModel.showNavigationModal) {
                        NavigationModal(
                            showNavigationModal = true,
                            onDismissRequest = { viewModel.showNavigationModal = false },
                            onPassageSelected = { passage ->
                                viewModel.navigateTo(Screen.Reader(passage))
                                viewModel.showNavigationModal = false
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
            HorizontalDivider()
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
                            .clickable { onVersionSelected(file, abbr) },
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
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
            HorizontalDivider()
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

@OptIn(ExperimentalMaterial3Api::class)
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
        is Screen.Search -> "Search"
        is Screen.Settings -> "Settings"
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.rotate(backAnimatedRotation))
                }
            }
        },
        actions = {
            IconButton(onClick = {
                bibleTargetRotation += 360f
                onBibleIconClick()
            }) {
                Icon(Icons.Filled.Book, contentDescription = "Bible Navigation", tint = Color.White, modifier = Modifier.rotate(bibleAnimatedRotation))
            }
            IconButton(onClick = {
                themeTargetRotation += 180f
                onThemeToggle()
            }) {
                Icon(if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2, contentDescription = "Toggle Theme", tint = Color.White, modifier = Modifier.rotate(themeAnimatedRotation))
            }
            IconButton(onClick = {
                colorTargetRotation += 180f
                onColorLensClick()
            }) {
                Icon(Icons.Filled.ColorLens, contentDescription = "Color Scheme", tint = Color.White, modifier = Modifier.rotate(colorAnimatedRotation))
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
                    screen: Screen,
                    isActive: Boolean
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
                        onClick = {
                            onScreenChange(screen)
                            showNavigationDropdown = false
                        },
                        modifier = Modifier.background(backgroundColor),
                        leadingIcon = {
                            Icon(
                                icon,
                                contentDescription = title,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                val isHomeActive = currentScreen is Screen.Home
                val isReaderActive = currentScreen is Screen.Reader
                val isBookmarksActive = currentScreen == Screen.Bookmarks
                val isSearchActive = currentScreen == Screen.Search
                val isSettingsActive = currentScreen == Screen.Settings

                createDropdownItem("Home", Icons.Filled.Home, Screen.Home, isHomeActive)
                createDropdownItem("Reader", Icons.Filled.Book, Screen.Reader(), isReaderActive)
                createDropdownItem("Bookmarks", Icons.Filled.Bookmark, Screen.Bookmarks, isBookmarksActive)
                createDropdownItem("Search", Icons.Filled.Search, Screen.Search, isSearchActive)
                createDropdownItem("Settings", Icons.Filled.Settings, Screen.Settings, isSettingsActive)
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
    onBack: (() -> Unit)? = null
) {
    val viewModel: AppViewModel = viewModel()
    var showNavigationDropdown by remember { mutableStateOf(false) }
    var showMultiDropdown by remember { mutableStateOf(false) }
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

    TopAppBar(
        title = {
            if (!viewModel.multiVersion){
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBibleIconClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .width(120.dp)
                        .padding(end = 8.dp)
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
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = currentScreen.passage?.chapter?.let { " $it" } ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Button(
                    onClick = { viewModel.showPrimaryVersionDropdown = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .padding(end = if (viewModel.multiVersion) 8.dp else 0.dp)
                ) {
                    Text(
                        text = currentVersionAbbr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.rotate(backAnimatedRotation))
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    themeTargetRotation += 180f
                    onThemeToggle()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2, contentDescription = "Toggle Theme", tint = Color.White, modifier = Modifier.rotate(themeAnimatedRotation))
            }
            IconButton(
                onClick = {
                    colorTargetRotation += 180f
                    onColorLensClick()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.ColorLens, contentDescription = "Color Scheme", tint = Color.White, modifier = Modifier.rotate(colorAnimatedRotation))
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
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                val current = if (!viewModel.multiVersion) "single" else viewModel.multiViewLayout

                @Composable
                fun createItem(title: String, onClick: () -> Unit) {
                    val isActive = title.lowercase() == current
                    val backgroundColor by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = tween(durationMillis = 200)
                    )
                    val textColor by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 200)
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        },
                        onClick = {
                            onClick()
                            showMultiDropdown = false
                        },
                        modifier = Modifier.background(backgroundColor)
                    )
                }

                createItem("Single") {
                    viewModel.multiVersion = false
                }
                createItem("Horizontal") {
                    viewModel.multiVersion = true
                    viewModel.multiViewLayout = "horizontal"
                }
                createItem("Vertical") {
                    viewModel.multiVersion = true
                    viewModel.multiViewLayout = "vertical"
                }
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
                        if (viewModel.scrollSync) Icons.Filled.LinkOff else Icons.Filled.Link,
                        contentDescription = "Toggle Scroll Sync",
                        tint = Color.White,
                        modifier = Modifier.rotate(syncAnimatedRotation)
                    )
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
                    screen: Screen,
                    isActive: Boolean
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
                        onClick = {
                            onScreenChange(screen)
                            showNavigationDropdown = false
                        },
                        modifier = Modifier.background(backgroundColor),
                        leadingIcon = {
                            Icon(
                                icon,
                                contentDescription = title,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                val isHomeActive = false
                val isReaderActive = true
                val isBookmarksActive = false
                val isSearchActive = false
                val isSettingsActive = false

                createDropdownItem("Home", Icons.Filled.Home, Screen.Home, isHomeActive)
                createDropdownItem("Reader", Icons.Filled.Book, Screen.Reader(), isReaderActive)
                createDropdownItem("Bookmarks", Icons.Filled.Bookmark, Screen.Bookmarks, isBookmarksActive)
                createDropdownItem("Search", Icons.Filled.Search, Screen.Search, isSearchActive)
                createDropdownItem("Settings", Icons.Filled.Settings, Screen.Settings, isSettingsActive)
            }
        }
    )
}

sealed class Screen {
    object Home : Screen()
    data class Reader(val passage: PassageSelection? = null) : Screen()
    object Bookmarks : Screen()
    object Search : Screen()
    object Settings : Screen()
}