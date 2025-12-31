package com.example.fohbible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fohbible.ui.theme.FohBibleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FohBibleApp()
        }
    }
}

@Composable
fun FohBibleApp() {
    var darkTheme by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var isCustomColor by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

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
        primaryColor = selectedColor ?: Color(0xFF6200EE),
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
                    HomeAppBar(
                        currentScreen = currentScreen,
                        onBibleIconClick = { showNavigationModal = true },
                        onThemeToggle = { darkTheme = !darkTheme },
                        onColorLensClick = { showColorThemeDialog = true },
                        onScreenChange = { screen -> currentScreen = screen }
                    )
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
                    when (currentScreen) {
                        is Screen.Home -> HomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            onBibleClick = { showNavigationModal = true }
                        )
                        is Screen.Reading -> ReadingScreen()
                        is Screen.Bookmarks -> BookmarksScreen()
                        is Screen.Settings -> SettingsScreen()
                        is Screen.Search -> SearchScreen()
                    }

                    // Navigation Modal Dialog
                    if (showNavigationModal) {
                        Dialog(
                            onDismissRequest = { showNavigationModal = false }
                        ) {
                            BibleNavigationDialog(
                                onDismiss = { showNavigationModal = false }
                            )
                        }
                    }

                    // Color Theme Dialog (shows preset colors)
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

                    // Color Wheel Dialog (for custom color selection)
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
            // Header
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

            // Color Options
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(colorOptions.chunked(2)) { rowThemes ->
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
                        // Fill empty spaces if row has less than 2 items
                        if (rowThemes.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Add Custom Color option
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Custom Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Custom Color Option Card
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

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    currentScreen: Screen,
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScreenChange: (Screen) -> Unit
) {
    var showNavigationDropdown by remember { mutableStateOf(false) }

    // Animate the rotation of the menu icon to X
    val rotation by animateFloatAsState(
        targetValue = if (showNavigationDropdown) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "menuIconRotation"
    )

    // FIX: Ensure all branches return String, not Unit
    val screenTitle = when (currentScreen) {
        is Screen.Home -> "Home"
        is Screen.Reading -> "Reading"
        is Screen.Bookmarks -> "Bookmarks"
        is Screen.Settings -> "Settings"
        is Screen.Search -> "Search"
    }

    TopAppBar(
        title = {
            Text(
                text = screenTitle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 0.dp),
                textAlign = TextAlign.Start
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.inversePrimary
        ),
        modifier = modifier,
        navigationIcon = {
            // Navigation dropdown button with animated icon
            IconButton(
                onClick = { showNavigationDropdown = !showNavigationDropdown },
                modifier = Modifier.rotate(rotation)
            ) {
                // Animate between menu and close icons
                if (showNavigationDropdown) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close Navigation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Open Navigation",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Enhanced DropdownMenu with highlighted active screen
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
                    screen: Screen
                ) {
                    val isActive = currentScreen == screen
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "dropdownBackground"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
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
                                tint = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // Dropdown items with active screen highlighting
                createDropdownItem("Home", Icons.Filled.Home, Screen.Home)
                createDropdownItem("Reading", Icons.Filled.Book, Screen.Reading)
                createDropdownItem("Bookmarks", Icons.Filled.Bookmark, Screen.Bookmarks)
                createDropdownItem("Settings", Icons.Filled.Settings, Screen.Settings)
                createDropdownItem("Search", Icons.Filled.Search, Screen.Search)
            }
        },
        actions = {
            IconButton(onClick = onBibleIconClick) {
                Icon(Icons.Filled.Book, contentDescription = "Bible Navigation")
            }
            IconButton(onClick = onThemeToggle) {
                Icon(Icons.Filled.Brightness6, contentDescription = "Toggle Theme")
            }
            IconButton(onClick = onColorLensClick) {
                Icon(Icons.Filled.ColorLens, contentDescription = "Color Scheme")
            }
        }
    )
}

@Composable
fun BibleNavigationDialog(
    onDismiss: () -> Unit
) {
    val booksOfBible = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
        "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
        "Nehemiah", "Esther", "Job", "Psalms", "Proverbs",
        "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah", "Lamentations",
        "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
        "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk",
        "Zephaniah", "Haggai", "Zechariah", "Malachi", "Matthew",
        "Mark", "Luke", "John", "Acts", "Romans",
        "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians", "Philippians",
        "Colossians", "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy",
        "Titus", "Philemon", "Hebrews", "James", "1 Peter",
        "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bible Navigation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
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

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search Bible...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // Books List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(booksOfBible.chunked(3)) { rowBooks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowBooks.forEach { book ->
                            Text(
                                text = book,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .clickable {
                                        // Navigate to book
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Go to random verse */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Random Verse")
                }
                Button(
                    onClick = { /* Go to daily verse */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Daily Verse")
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

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onBibleClick: () -> Unit
) {
    val dailyVerse = "For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life. - John 3:16"
    val recentReadings = listOf(
        RecentReading("Psalm 23", "The Lord is my shepherd..."),
        RecentReading("Matthew 6:9-13", "The Lord's Prayer"),
        RecentReading("1 Corinthians 13", "The Love Chapter")
    )
    val quickActions = listOf(
        QuickAction("Read Bible", Icons.Filled.Book, color = MaterialTheme.colorScheme.primary),
        QuickAction("Audio Bible", Icons.AutoMirrored.Filled.VolumeUp, color = MaterialTheme.colorScheme.primary),
        QuickAction("Reading Plan", Icons.Filled.History, color = MaterialTheme.colorScheme.primary),
        QuickAction("Bookmarks", Icons.Filled.Bookmark, color = MaterialTheme.colorScheme.primary)
    )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            DailyVerseCard(verse = dailyVerse)
        }

        item {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            QuickActionsGrid(actions = quickActions, onBibleClick = onBibleClick)
        }

        item {
            RecentReadingsSection(readings = recentReadings)
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DailyVerseCard(verse: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Verse of the Day",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = verse,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                textAlign = TextAlign.Justify
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Save to bookmarks */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Share",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { /* Share verse */ }
                )
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    actions: List<QuickAction>,
    onBibleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        actions.forEach { action ->
            QuickActionItem(action = action, onClick = {
                if (action.title == "Read Bible") onBibleClick()
            })
        }
    }
}

@Composable
fun QuickActionItem(action: QuickAction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = action.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentReadingsSection(readings: List<RecentReading>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Readings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See All",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* Navigate to all readings */ }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        readings.forEachIndexed { index, reading ->
            RecentReadingItem(reading = reading)
            if (index < readings.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun RecentReadingItem(reading: RecentReading) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to reading */ }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = reading.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = reading.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { /* Bookmark */ }) {
            Icon(
                Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Other screens (simplified versions)
@Composable
fun ReadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Reading Screen")
    }
}

@Composable
fun BookmarksScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Bookmarks Screen")
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Settings Screen")
    }
}

@Composable
fun SearchScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Search Screen")
    }
}

// Data classes
data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

data class RecentReading(
    val title: String,
    val preview: String
)

sealed class Screen {
    object Home : Screen()
    object Reading : Screen()
    object Bookmarks : Screen()
    object Settings : Screen()
    object Search : Screen()
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FohBibleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen(onBibleClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyVerseCardPreview() {
    FohBibleTheme {
        DailyVerseCard(verse = "For God so loved the world...")
    }
}

@Preview(showBackground = true)
@Composable
fun HomeAppBarPreview() {
    FohBibleTheme {
        HomeAppBar(
            currentScreen = Screen.Home,
            onBibleIconClick = {},
            onThemeToggle = {},
            onColorLensClick = {},
            onScreenChange = {}
        )
    }
}