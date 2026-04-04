@file:OptIn(ExperimentalMaterial3Api::class)
package com.fountofhopedotorg.fohbible.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fountofhopedotorg.fohbible.ColorWheelDialog
import com.fountofhopedotorg.fohbible.modals.FontModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.DefaultPrimaryColor
import com.fountofhopedotorg.fohbible.ui.theme.PredefinedColorThemes
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.utils.availableFontFamilies
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import java.util.Locale

const val MAX_FONT_SIZE = 100
const val MIN_FONT_SIZE = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current

    var showVersionInfoDialog by remember { mutableStateOf(false) }
    var selectedVersionInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showBgModal by remember { mutableStateOf(false) }
    var showFontModal by remember { mutableStateOf(false) }
    var tempFontSize by remember { mutableStateOf(viewModel.fontSize.toString()) }
    var showColorWheel by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(viewModel.customColor) }
    var isUsingCustomColor by remember { mutableStateOf(viewModel.isCustomColor) }
    var showLightOverlayColorWheel by remember { mutableStateOf(false) }
    var showDarkOverlayColorWheel by remember { mutableStateOf(false) }
    var showLightModalColorWheel by remember { mutableStateOf(false) }
    var showDarkModalColorWheel by remember { mutableStateOf(false) }
    var showRefreshConfirmDialog by remember { mutableStateOf(false) }
    var showRefreshResultDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showWordMarkerColorWheel by remember { mutableStateOf(false) }
    var showVerseMarkerColorWheel by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showHighlightColorWheel by remember { mutableStateOf(false) }
    var showResetHighlightColorsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.isCustomColor, viewModel.customColor) {
        isUsingCustomColor = viewModel.isCustomColor
        customColor = viewModel.customColor
    }

    LaunchedEffect(viewModel.isRefreshingDatabases) {
        if (viewModel.isRefreshingDatabases && !showRefreshResultDialog) {
            showRefreshResultDialog = true
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.customTextureUri = it.toString()
            viewModel.bgImageIndex = 34
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "App Settings", subtitle = "Customize App appearance") {
                SettingsItem(
                    title = "Dark Mode",
                    subtitle = "Toggle between light and dark themes"
                ) {
                    Switch(
                        checked = viewModel.darkTheme,
                        onCheckedChange = { viewModel.darkTheme = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Column {
                    Text("Color Scheme", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PredefinedColorThemes) { theme ->
                            ColorButton(
                                color = theme.primaryColor,
                                name = theme.name,
                                isSelected = viewModel.selectedColor == theme.primaryColor && !isUsingCustomColor,
                                onClick = {
                                    viewModel.selectedColor = theme.primaryColor
                                    viewModel.isCustomColor = false
                                    isUsingCustomColor = false
                                }
                            )
                        }
                        item {
                            ColorButton(
                                color = customColor ?: DefaultPrimaryColor,
                                name = "Custom",
                                isSelected = isUsingCustomColor,
                                onClick = { showColorWheel = true }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Reader Settings", subtitle = "Customize your Bible reading experience") {
                BibleVersionSelector(
                    title = "Primary Bible Version",
                    currentAbbr = viewModel.currentVersionAbbr,
                    description = BibleVersionUtils.descriptionMap[viewModel.currentDbName] ?: "Bible translation",
                    onVersionSelected = { file, abbr ->
                        viewModel.currentDbName = file
                        viewModel.currentVersionAbbr = abbr
                    }
                )

                SettingsItem(
                    title = "Multi-Version Display",
                    subtitle = "Show two Bible versions side by side"
                ) {
                    Switch(
                        checked = viewModel.multiVersion,
                        onCheckedChange = { viewModel.multiVersion = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                if (viewModel.multiVersion) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BibleVersionSelector(
                        title = "Secondary Bible Version",
                        currentAbbr = viewModel.secondaryVersionAbbr.ifEmpty { "Select version" },
                        description = if (viewModel.secondaryVersionAbbr.isNotEmpty()) {
                            BibleVersionUtils.versionMap.entries
                                .find { it.value == viewModel.secondaryVersionAbbr }
                                ?.let { BibleVersionUtils.descriptionMap[it.key] }
                                ?: "Bible translation"
                        } else {
                            "Select a secondary version"
                        },
                        onVersionSelected = { file, abbr ->
                            viewModel.secondaryDbName = file
                            viewModel.secondaryVersionAbbr = abbr
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Multi-View Layout",
                        subtitle = "Horizontal or vertical arrangement"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Horizontal",
                                modifier = Modifier
                                    .clickable { viewModel.multiViewLayout = "horizontal" }
                                    .background(
                                        if (viewModel.multiViewLayout == "horizontal") MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        ) else Color.Transparent
                                    )
                                    .padding(8.dp)
                            )
                            Text(
                                text = "Vertical",
                                modifier = Modifier
                                    .clickable { viewModel.multiViewLayout = "vertical" }
                                    .background(
                                        if (viewModel.multiViewLayout == "vertical") MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        ) else Color.Transparent
                                    )
                                    .padding(8.dp)
                            )
                        }
                    }
                    SettingsItem(
                        title = "Scroll Sync",
                        subtitle = "Synchronize scrolling between versions"
                    ) {
                        Switch(
                            checked = viewModel.scrollSync,
                            onCheckedChange = { viewModel.scrollSync = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column {
                    Text("Font Family", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableFontFamilies) { family ->
                            FontButton(
                                family = family,
                                isSelected = viewModel.selectedFontFamily == family,
                                onClick = { viewModel.selectedFontFamily = family }
                            )
                        }
                    }
                }

                SettingsItem(
                    title = "Font Size",
                    subtitle = "Adjust text size for better readability",
                    onClick = { showFontModal = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.fontSize = maxOf(MIN_FONT_SIZE, viewModel.fontSize - 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${viewModel.fontSize}",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { viewModel.fontSize = minOf(MAX_FONT_SIZE, viewModel.fontSize + 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                SettingsItem(
                    title = "Custom Background",
                    subtitle = "Add your own photo as background"
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add custom background")
                    }
                }

                SettingsItem(
                    title = "Background Texture",
                    subtitle = "Choose from built-in textures",
                    onClick = { showBgModal = true }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                SettingsItem(
                    title = "Overlay Opacity",
                    subtitle = "Adjust the overlay transparency"
                ) {
                    Slider(
                        value = viewModel.overlayOpacity,
                        onValueChange = { viewModel.overlayOpacity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        },
                    )
                }

                SettingsItem(
                    title = "Light Overlay Color",
                    subtitle = "Overlay color for light theme"
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(viewModel.lightOverlayColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showLightOverlayColorWheel = true }
                    )
                }

                SettingsItem(
                    title = "Dark Overlay Color",
                    subtitle = "Overlay color for dark theme"
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(viewModel.darkOverlayColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showDarkOverlayColorWheel = true }
                    )
                }
                SettingsItem(
                    title = "Verse Marker Color",
                    subtitle = "Color for highlighting entire verses"
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(viewModel.verseMarkerColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showVerseMarkerColorWheel = true }
                    )
                }

                SettingsItem(
                    title = "Light Modal Background Color",
                    subtitle = "Modal background color for light theme"
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(viewModel.lightModalBackgroundColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showLightModalColorWheel = true }
                    )
                }

                SettingsItem(
                    title = "Dark Modal Background Color",
                    subtitle = "Modal background color for dark theme"
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(viewModel.darkModalBackgroundColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showDarkModalColorWheel = true }
                    )
                }
                if (viewModel.isStudyMode) {
                    SettingsItem(
                        title = "Word Marker Color",
                        subtitle = "Color for highlighting individual words"
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(viewModel.wordMarkerColor)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showWordMarkerColorWheel = true }
                        )
                    }

                    SettingsItem(
                        title = "Dictionary Mode",
                        subtitle = "Toggle between dictionary or highlight mode on word tap"
                    ) {
                        Switch(
                            checked = viewModel.isDictionaryMode,
                            onCheckedChange = { viewModel.isDictionaryMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                SettingsItem(
                    title = "Lazy Reader Mode",
                    subtitle = "Toggle between partial or full chapter loading. Lazy reader only loads the visible verses at least and is ideal for slower devices"
                ) {
                    Switch(
                        checked = viewModel.isLazyReader,
                        onCheckedChange = { viewModel.isLazyReader = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
                SettingsItem(
                    title = "Study Mode",
                    subtitle = "Turn this off if you want simple verse rendering mode activated"
                ) {
                    Switch(
                        checked = viewModel.isStudyMode,
                        onCheckedChange = { viewModel.isStudyMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = "Word Marker Colors",
                subtitle = "Tap to edit colors"
            ) {
                Column {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0..5) {
                                HighlightColorSquare(
                                    color = viewModel.predefinedHighlightColors.getOrElse(i) { Color.White },
                                    onClick = {
                                        viewModel.editingHighlightColorIndex = i
                                        viewModel.showHighlightColorEditor = true
                                        showHighlightColorWheel = true
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showResetHighlightColorsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset to Default Colors")
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "Database Management",
                subtitle = "Refresh and manage Bible databases"
            ) {
                SettingsItem(
                    title = "Refresh All Databases",
                    subtitle = "Force recopy all database files from assets",
                    onClick = { showRefreshConfirmDialog = true }
                ) {
                    if (viewModel.isRefreshingDatabases) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh databases")
                    }
                }
                Text(
                    text = "This will recopy all Bible versions and dictionaries from the app assets. Useful if databases become corrupted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            SettingsSection(title = "More Options", subtitle = "Additional preferences") {
                SettingsItem(
                    title = "Data & Storage",
                    subtitle = "Manage app data and cache",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                HorizontalDivider()

                SettingsItem(
                    title = "About",
                    subtitle = "App version and information",
                    onClick = { showAboutDialog = true }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            SettingsSection(title = "Quick Actions", subtitle = "Common tasks") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Reset All")
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:fountofhopedevotionals@gmail.com".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "FoH Bible App Feedback")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Feedback")
                    }
                }
            }
        }
    }

    if (showResetHighlightColorsDialog) {
        AlertDialog(
            onDismissRequest = { showResetHighlightColorsDialog = false },
            title = {
                Text(
                    "Reset Highlight Colors",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text("This will reset all marker colors to their default values. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetHighlightColorsToDefault()
                        showResetHighlightColorsDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetHighlightColorsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHighlightColorWheel) {
        ColorWheelDialog(
            onDismissRequest = {
                showHighlightColorWheel = false
                viewModel.showHighlightColorEditor = false
                viewModel.editingHighlightColorIndex = -1
            },
            onColorSelected = { color ->
                if (viewModel.editingHighlightColorIndex != -1) {
                    // Update the color at the edited index
                    viewModel.updateHighlightColor(viewModel.editingHighlightColorIndex, color)
                    viewModel.showHighlightColorEditor = false
                    viewModel.editingHighlightColorIndex = -1
                }
                showHighlightColorWheel = false
            },
            initialColor = if (viewModel.editingHighlightColorIndex != -1) {
                viewModel.predefinedHighlightColors.getOrNull(viewModel.editingHighlightColorIndex) ?: Color.White
            } else {
                Color.White
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    "Reset All Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text("This will restore EVERY setting to its default value:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Font size, theme, colors", style = MaterialTheme.typography.bodyMedium)
                    Text("• Bible versions, layout, sync", style = MaterialTheme.typography.bodyMedium)
                    Text("• Background, overlay, markers", style = MaterialTheme.typography.bodyMedium)
                    Text("• Study mode, lazy reader, etc.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "This action cannot be undone.\n\nAre you sure you want to continue?",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.fontSize = 18
                        viewModel.darkTheme = false
                        viewModel.selectedColor = DefaultPrimaryColor
                        viewModel.isCustomColor = false
                        viewModel.selectedFontFamily = "system"
                        viewModel.currentDbName = "kj2.sqlite3"
                        viewModel.currentVersionAbbr = BibleVersionUtils.versionMap["kj2.sqlite3"] ?: "KJ2"
                        viewModel.multiVersion = false
                        viewModel.secondaryDbName = "esv.sqlite3"
                        viewModel.secondaryVersionAbbr = BibleVersionUtils.versionMap["esv.sqlite3"] ?: "ESV"
                        viewModel.multiViewLayout = "horizontal"
                        viewModel.scrollSync = true
                        viewModel.customTextureUri = null
                        viewModel.bgImageIndex = 0
                        viewModel.overlayOpacity = 0.8f
                        viewModel.lightOverlayColor = Color(0xFFF5F5DC)
                        viewModel.darkOverlayColor = Color(0xFF100F21)
                        viewModel.lightModalBackgroundColor = Color(0xFFE0E0E0)
                        viewModel.darkModalBackgroundColor = Color(0xFF2D2D2D)
                        viewModel.wordMarkerColor = Color(0xDDAC95E1)
                        viewModel.verseMarkerColor = Color(0xFF95F198)
                        viewModel.isDictionaryMode = true
                        viewModel.isLazyReader = true
                        viewModel.isStudyMode = true
                        viewModel.resetHighlightColorsToDefault()

                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showVersionInfoDialog && selectedVersionInfo != null) {
        VersionInfoDialog(
            versionName = selectedVersionInfo!!.first,
            versionDescription = selectedVersionInfo!!.second,
            onDismiss = { showVersionInfoDialog = false; selectedVersionInfo = null }
        )
    }

    if (showBgModal) {
        BgModal(
            currentIndex = viewModel.bgImageIndex,
            customUri = viewModel.customTextureUri,
            onSelect = { index ->
                viewModel.bgImageIndex = index
                showBgModal = false
            },
            onDismiss = { showBgModal = false },
            onPickCustom = { imagePickerLauncher.launch("image/*") },
            onRemoveCustom = { viewModel.customTextureUri = null }
        )
    }

    if (showFontModal) {
        FontModal(
            tempSize = tempFontSize,
            onChange = { tempFontSize = it },
            onConfirm = {
                val newSize = tempFontSize.toIntOrNull() ?: viewModel.fontSize
                viewModel.fontSize = newSize.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
                showFontModal = false
            },
            onDismiss = { showFontModal = false },
            appViewModel = viewModel
        )
    }

    if (showColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showColorWheel = false },
            onColorSelected = { color ->
                viewModel.customColor = color
                viewModel.selectedColor = color
                viewModel.isCustomColor = true
                isUsingCustomColor = true
                customColor = color
                showColorWheel = false
            },
            initialColor = customColor ?: viewModel.selectedColor ?: DefaultPrimaryColor
        )
    }

    if (showLightOverlayColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showLightOverlayColorWheel = false },
            onColorSelected = { color ->
                viewModel.lightOverlayColor = color
                showLightOverlayColorWheel = false
            },
            initialColor = viewModel.lightOverlayColor
        )
    }

    if (showDarkOverlayColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showDarkOverlayColorWheel = false },
            onColorSelected = { color ->
                viewModel.darkOverlayColor = color
                showDarkOverlayColorWheel = false
            },
            initialColor = viewModel.darkOverlayColor
        )
    }

    if (showWordMarkerColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showWordMarkerColorWheel = false },
            onColorSelected = { color ->
                viewModel.wordMarkerColor = color
                showWordMarkerColorWheel = false
            },
            initialColor = viewModel.wordMarkerColor
        )
    }

    if (showVerseMarkerColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showVerseMarkerColorWheel = false },
            onColorSelected = { color ->
                viewModel.verseMarkerColor = color
                showVerseMarkerColorWheel = false
            },
            initialColor = viewModel.verseMarkerColor
        )
    }

    if (showLightModalColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showLightModalColorWheel = false },
            onColorSelected = { color ->
                viewModel.lightModalBackgroundColor = color
                showLightModalColorWheel = false
            },
            initialColor = viewModel.lightModalBackgroundColor
        )
    }

    if (showDarkModalColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showDarkModalColorWheel = false },
            onColorSelected = { color ->
                viewModel.darkModalBackgroundColor = color
                showDarkModalColorWheel = false
            },
            initialColor = viewModel.darkModalBackgroundColor
        )
    }

    if (showRefreshConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshConfirmDialog = false },
            title = { Text("Refresh All Databases", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("This action will:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Recopy ALL Bible versions from assets")
                    Text("• Recopy dictionary databases")
                    Text("• Recopy commentary databases")
                    Text("• Note: Your bookmarks and settings will NOT be affected")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This may take a few moments. Continue?",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.refreshDatabases(context)
                        showRefreshConfirmDialog = false
                        showRefreshResultDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Refresh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefreshConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRefreshResultDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!viewModel.isRefreshingDatabases) {
                    showRefreshResultDialog = false
                    viewModel.lastRefreshMessage = ""
                }
            },
            title = {
                Text(
                    if (viewModel.isRefreshingDatabases) "Refreshing Databases..." else if (viewModel.lastRefreshSuccess) "Refresh Complete" else "Refresh Incomplete",
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    if (viewModel.isRefreshingDatabases) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Please wait...")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            viewModel.lastRefreshMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (viewModel.lastRefreshSuccess) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(viewModel.lastRefreshMessage, style = MaterialTheme.typography.bodyMedium)
                        if (!viewModel.lastRefreshSuccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Try restarting the app if issues persist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!viewModel.isRefreshingDatabases) {
                    TextButton(
                        onClick = {
                            showRefreshResultDialog = false
                            viewModel.lastRefreshMessage = ""
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun BibleVersionSelector(
    title: String,
    currentAbbr: String,
    description: String,
    onVersionSelected: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(enabled = true, type = ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAbbr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                BibleVersionUtils.versionMap.forEach { (file, abbr) ->
                    val versionDesc = BibleVersionUtils.descriptionMap[file] ?: "Bible translation"
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = abbr,
                                    fontWeight = if (abbr == currentAbbr) FontWeight.Bold else FontWeight.Normal,
                                    color = if (abbr == currentAbbr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = versionDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        },
                        onClick = {
                            onVersionSelected(file, abbr)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            if (abbr == currentAbbr) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                        )
                    )
                    if (file != BibleVersionUtils.versionMap.keys.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VersionInfoDialog(
    versionName: String,
    versionDescription: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = versionName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Description:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = versionDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "About Bible Versions:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Different translations balance word-for-word accuracy with thought-for-thought clarity. Choose based on your study needs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SettingsSection(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        content()
    }
}

@Composable
fun ColorButton(color: Color, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun FontButton(family: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = family.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
        modifier = Modifier
            .clickable { onClick() }
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        fontFamily = getFontFamily(family),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun BgModal(
    currentIndex: Int,
    customUri: String?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onPickCustom: () -> Unit,
    onRemoveCustom: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Background",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentIndex == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onSelect(0) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("None")
                        }
                    }
                    items(33) { i ->
                        val index = i + 1
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentIndex == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onSelect(index) }
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/textures/$index.jpg",
                                contentDescription = "Texture $index",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    item {
                        if (customUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (currentIndex == 34) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { onSelect(34) }
                            ) {
                                AsyncImage(
                                    model = customUri,
                                    contentDescription = "Custom texture",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onPickCustom,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add custom")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPickCustom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Choose Custom Image")
                }

                if (customUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onRemoveCustom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove Custom Image")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightColorSquare(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .border(
                    2.dp,
                    if (color == Color.White) MaterialTheme.colorScheme.outline else color,
                    RoundedCornerShape(10.dp)
                )
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Fount of Hope Study Bible",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Version: $versionName",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Developed by Fount of Hope Devotionals",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "fountofhopedevotionals@gmail.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Thank you for using Fount Of Hope Study Bible. Your support means a lot to us!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}