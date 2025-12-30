package com.example.fohbible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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

    FohBibleTheme(darkTheme = darkTheme) {
        var showNavigationModal by remember { mutableStateOf(false) }
        var showColorWheel by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                FohBibleAppBar(
                    onBibleIconClick = { showNavigationModal = true },
                    onThemeToggle = { darkTheme = !darkTheme },
                    onColorLensClick = { showColorWheel = true }
                )
            }
        ) { innerPadding ->
            Greeting(
                name = "Android",
                modifier = Modifier.padding(innerPadding)
            )

            if (showNavigationModal) {
                NavigationModal(onDismissRequest = { showNavigationModal = false })
            }

            if (showColorWheel) {
                ColorWheelDialog(
                    onDismissRequest = { showColorWheel = false },
                    onColorSelected = { color ->
                        selectedColor = color
                        // TODO: Save the selected color to SharedPreferences or ViewModel
                    },
                    initialColor = selectedColor ?: Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FohBibleAppBar(
    modifier: Modifier = Modifier,
    onBibleIconClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Home") },
        modifier = modifier,
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
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Search") },
                    onClick = {
                        // TODO: Navigate to Search
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Bookmarks") },
                    onClick = {
                        // TODO: Navigate to Bookmarks
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        // TODO: Navigate to Settings
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                )
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Fount of Hope Bible",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FohBibleTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun FohBibleAppBarPreview() {
    FohBibleTheme {
        FohBibleAppBar(
            onBibleIconClick = {},
            onThemeToggle = {},
            onColorLensClick = {}
        )
    }
}