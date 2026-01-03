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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fohbible.data.DatabaseHelper
import com.example.fohbible.data.PassageSelection
import com.example.fohbible.screens.BookmarksScreen
import com.example.fohbible.screens.HomeScreen
import com.example.fohbible.screens.ReaderScreen
import com.example.fohbible.screens.SearchScreen
import com.example.fohbible.screens.SettingsScreen
import com.example.fohbible.ui.theme.FohBibleTheme
import androidx.compose.ui.text.style.TextOverflow

class MainActivity : ComponentActivity() {
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        databaseHelper = DatabaseHelper(this)
        setContent {
            FohBibleApp(databaseHelper)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseHelper.close()
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
fun FohBibleApp(databaseHelper: DatabaseHelper? = null) {
    var darkTheme by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var isCustomColor by remember { mutableStateOf(false) }
    val navigationStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = navigationStack.last()

    fun navigateTo(screen: Screen) {
        navigationStack.add(screen)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun goBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
        }
    }

    LaunchedEffect(selectedColor, darkTheme) {
        selectedColor?.let {
            ThemeManager.primaryColor = it
            ThemeManager.darkTheme = darkTheme
            ThemeManager.isCustomColor = true
            isCustomColor = true
        }
    }

    val themeState = AppThemeState(
        darkTheme = darkTheme,
        primaryColor = selectedColor ?: Color(0xFF220F3D),
        isCustomColor = isCustomColor
    )

    CompositionLocalProvider(LocalAppTheme provides themeState) {
        FohBibleTheme(darkTheme = darkTheme) {
            var showNavigationModal by remember { mutableStateOf(false) }
            var showColorThemeDialog by remember { mutableStateOf(false) }
            var showColorWheelDialog by remember { mutableStateOf(false) }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (currentScreen is Screen.Reader) {
                        ReaderAppBar(
                            currentScreen = currentScreen,
                            onBibleIconClick = { showNavigationModal = true },
                            onThemeToggle = { darkTheme = !darkTheme },
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
                                navigateTo(targetScreen)
                            },
                            onBack = if (navigationStack.size > 1) { { goBack() } } else null
                        )
                    } else {
                        HomeAppBar(
                            currentScreen = currentScreen,
                            onBibleIconClick = { showNavigationModal = true },
                            onThemeToggle = { darkTheme = !darkTheme },
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
                                navigateTo(targetScreen)
                            },
                            onBack = if (navigationStack.size > 1) { { goBack() } } else null
                        )
                    }
                },
                floatingActionButton = {
                    if (currentScreen is Screen.Home) {
                        FloatingActionButton(
                            onClick = { showNavigationModal = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Book, contentDescription = "Open Bible")
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    BackHandler(enabled = navigationStack.size > 1) {
                        goBack()
                    }

                    when (currentScreen) {
                        Screen.Home -> {
                            HomeScreen(
                                modifier = Modifier.fillMaxSize(),
                                onBibleClick = { showNavigationModal = true },
                                databaseHelper = databaseHelper
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
                                databaseHelper = databaseHelper
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
                                navigateTo(Screen.Reader(passage))
                                showNavigationModal = false
                            },
                            databaseHelper = databaseHelper
                        )
                    }

                    if (showColorThemeDialog) {
                        Dialog(
                            onDismissRequest = { showColorThemeDialog = false }
                        ) {
                            UpdatedColorThemeDialog(
                                onDismiss = { showColorThemeDialog = false },
                                onColorSelected = { color ->
                                    selectedColor = color
                                    isCustomColor = true
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
                                selectedColor = color
                                isCustomColor = true
                                showColorWheelDialog = false
                            },
                            initialColor = selectedColor ?: ThemeManager.primaryColor
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
    val colorOptions = listOf(
        ColorTheme("Blue Theme", Color(0xFF2196F3), Color(0xFF1976D2)),
        ColorTheme("Green Theme", Color(0xFF4CAF50), Color(0xFF388E3C)),
        ColorTheme("Purple Theme", Color(0xFF9C27B0), Color(0xFF7B1FA2)),
        ColorTheme("Orange Theme", Color(0xFFFF9800), Color(0xFFF57C00)),
        ColorTheme("Red Theme", Color(0xFFF44336), Color(0xFFD32F2F)),
        ColorTheme("Teal Theme", Color(0xFF009688), Color(0xFF00796B)),
    )

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
                items(colorOptions.chunked(1)) { rowThemes ->
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

data class ColorTheme(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color
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
    onBack: (() -> Unit)? = null
) {
    var showNavigationDropdown by remember { mutableStateOf(false) }

    // Animate the rotation of the menu icon to X
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
            containerColor = MaterialTheme.colorScheme.primary
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
                // Helper function to create dropdown items with highlight
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
                        .width(135.dp)
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
                    onClick = { /* TODO: implement version selection */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "KJ2",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
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