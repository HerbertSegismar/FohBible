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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
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

    var fontSize by mutableIntStateOf(18)
    var darkTheme by mutableStateOf(false)
    var selectedColor by mutableStateOf<Color?>(null)
    var isCustomColor by mutableStateOf(false)
    val navigationStack = mutableStateListOf<Screen>(Screen.Home)
    var currentDbName by mutableStateOf("kj2.sqlite3")
    var currentVersionAbbr by mutableStateOf(BibleVersionUtils.versionMap["kj2.sqlite3"]!!)

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
    LaunchedEffect(viewModel.selectedColor, viewModel.darkTheme) {
        viewModel.selectedColor?.let {
            ThemeManager.primaryColor = it
            ThemeManager.darkTheme = viewModel.darkTheme
            ThemeManager.isCustomColor = true
            viewModel.isCustomColor = true
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
    DisposableEffect(Unit) {
        onDispose {
            dbHelper?.close()
        }
    }
    CompositionLocalProvider(LocalAppTheme provides themeState) {
        FohBibleTheme(darkTheme = viewModel.darkTheme) {
            var showNavigationModal by remember { mutableStateOf(false) }
            var showColorThemeDialog by remember { mutableStateOf(false) }
            var showColorWheelDialog by remember { mutableStateOf(false) }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (currentScreen is Screen.Reader) {
                        ReaderAppBar(
                            currentScreen = currentScreen,
                            currentVersionAbbr = viewModel.currentVersionAbbr,
                            versionMap = BibleVersionUtils.versionMap,
                            descriptionMap = descriptionMap,
                            onBibleIconClick = { showNavigationModal = true },
                            onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                            onColorLensClick = { showColorThemeDialog = true },
                            onVersionChange = { file, abbr ->
                                viewModel.currentDbName = file
                                viewModel.currentVersionAbbr = abbr
                            },
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
                            onBack = if (viewModel.navigationStack.size > 1) { { viewModel.goBack() } } else null
                        )
                    } else {
                        HomeAppBar(
                            currentScreen = currentScreen,
                            onBibleIconClick = { showNavigationModal = true },
                            onThemeToggle = { viewModel.darkTheme = !viewModel.darkTheme },
                            onColorLensClick = { showColorThemeDialog = true },
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
                            onBack = if (viewModel.navigationStack.size > 1) { { viewModel.goBack() } } else null
                        )
                    }
                },
                floatingActionButton = {
                    if (currentScreen is Screen.Home) {
                        FloatingActionButton(
                            onClick = { showNavigationModal = true },
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
                                onBibleClick = { showNavigationModal = true },
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
                        Screen.Search -> SearchScreen()
                        Screen.Settings -> SettingsScreen()
                    }
                    if (showNavigationModal) {
                        NavigationModal(
                            showNavigationModal = true,
                            onDismissRequest = { showNavigationModal = false },
                            onPassageSelected = { passage ->
                                viewModel.navigateTo(Screen.Reader(passage))
                                showNavigationModal = false
                            },
                            databaseHelper = dbHelper
                        )
                    }
                    if (showColorThemeDialog) {
                        Dialog(
                            onDismissRequest = { showColorThemeDialog = false }
                        ) {
                            UpdatedColorThemeDialog(
                                onDismiss = { showColorThemeDialog = false },
                                onColorSelected = { color ->
                                    viewModel.selectedColor = color
                                    viewModel.isCustomColor = true
                                },
                                onCustomColorClick = {
                                    showColorThemeDialog = false
                                    showColorWheelDialog = true
                                }
                            )
                        }
                    }
                    if (showColorWheelDialog) {
                        ColorWheelDialog(
                            onDismissRequest = { showColorWheelDialog = false },
                            onColorSelected = { color ->
                                viewModel.selectedColor = color
                                viewModel.isCustomColor = true
                                showColorWheelDialog = false
                            },
                            initialColor = viewModel.selectedColor ?: ThemeManager.primaryColor
                        )
                    }
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
                    fontSize = 18.dp.value.sp
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
    val rotation by animateFloatAsState(
        targetValue = if (showNavigationDropdown) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "menuIconRotation"
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
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        },
        actions = {
            IconButton(onClick = onBibleIconClick) {
                Icon(Icons.Filled.Book, contentDescription = "Bible Navigation", tint = Color.White)
            }
            IconButton(onClick = onThemeToggle) {
                Icon(Icons.Filled.Brightness6, contentDescription = "Toggle Theme", tint = Color.White)
            }
            IconButton(onClick = onColorLensClick) {
                Icon(Icons.Filled.ColorLens, contentDescription = "Color Scheme", tint = Color.White)
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
    versionMap: Map<String, String>,
    descriptionMap: Map<String, String>,
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onVersionChange: (String, String) -> Unit,
    onScreenChange: (Screen) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var showNavigationDropdown by remember { mutableStateOf(false) }
    var showVersionDropdown by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (showNavigationDropdown) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "menuIconRotation"
    )
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onBibleIconClick() },
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
                Box {
                    Button(
                        onClick = { showVersionDropdown = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = currentVersionAbbr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showVersionDropdown,
                        onDismissRequest = { showVersionDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        @Composable
                        fun createVersionItem(
                            file: String,
                            abbr: String,
                            description: String,
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
                                    Column {
                                        Text(
                                            text = abbr,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor
                                        )
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                onClick = {
                                    onVersionChange(file, abbr)
                                    showVersionDropdown = false
                                },
                                modifier = Modifier.background(backgroundColor)
                            )
                        }
                        versionMap.forEach { (file, abbr) ->
                            val desc = descriptionMap[file] ?: "Bible translation"
                            createVersionItem(
                                file = file,
                                abbr = abbr,
                                description = desc,
                                isActive = abbr == currentVersionAbbr
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
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        },
        actions = {
            IconButton(onClick = onThemeToggle) {
                Icon(Icons.Filled.Brightness6, contentDescription = "Toggle Theme", tint = Color.White)
            }
            IconButton(onClick = onColorLensClick) {
                Icon(Icons.Filled.ColorLens, contentDescription = "Color Scheme", tint = Color.White)
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