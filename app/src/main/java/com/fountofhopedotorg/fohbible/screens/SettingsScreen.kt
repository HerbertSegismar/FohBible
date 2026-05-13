package com.fountofhopedotorg.fohbible.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog
import com.fountofhopedotorg.fohbible.composables.AboutDialog
import com.fountofhopedotorg.fohbible.composables.ColorButton
import com.fountofhopedotorg.fohbible.composables.FontButton
import com.fountofhopedotorg.fohbible.composables.HighlightColorSquare
import com.fountofhopedotorg.fohbible.composables.RotatingPhoneGraphics
import com.fountofhopedotorg.fohbible.composables.SettingsItem
import com.fountofhopedotorg.fohbible.composables.SettingsOpacitySlider
import com.fountofhopedotorg.fohbible.composables.SettingsSection
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.modals.FontModal
import com.fountofhopedotorg.fohbible.modals.OrbsCountModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.DefaultPrimaryColor
import com.fountofhopedotorg.fohbible.ui.theme.PredefinedColorThemes
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.utils.availableFontFamilies

const val MAX_FONT_SIZE = 100
const val MIN_FONT_SIZE = 1
const val MAX_ORB_COUNT = 20
const val MIN_ORB_COUNT = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val primary = MaterialTheme.colorScheme.primary
    val viewColor = primary.copy(alpha = 0.2f)
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current
    var showHeaderButtonsColorWheel by remember { mutableStateOf(false) }
    var showOrbsCountModal by remember { mutableStateOf(false) }
    var showFontModal by remember { mutableStateOf(false) }
    var tempFontSize by remember { mutableStateOf(viewModel.fontSize.toString()) }
    var tempOrbsCount by remember { mutableStateOf(viewModel.orbsCount.toString()) }
    var showColorWheel by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(viewModel.customColor) }
    var isUsingCustomColor by remember { mutableStateOf(viewModel.isCustomColor) }
    var showRefreshConfirmDialog by remember { mutableStateOf(false) }
    var showRefreshResultDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
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
                            checkedThumbColor = primary,
                            checkedTrackColor = primary.copy(alpha = 0.5f)
                        )
                    )
                }
                if (viewModel.renderOrbs) {
                    SettingsItem(
                        title = "Orbs Count ($MIN_ORB_COUNT - $MAX_ORB_COUNT)",
                        subtitle = "Adjust the number of orbs rendered",
                        onClick = { showOrbsCountModal = true }
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.orbsCount = maxOf(MIN_ORB_COUNT, viewModel.orbsCount - 1) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    "${viewModel.orbsCount}",
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = primary,
                                    fontSize = 18.sp
                                )
                                IconButton(
                                    onClick = { viewModel.orbsCount = minOf(MAX_ORB_COUNT, viewModel.orbsCount + 1) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
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

        // ----- Reader Settings -----
        item {
            SettingsSection(title = "Reader Settings", subtitle = "Customize your Bible reading experience") {
                // Primary version
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showPrimaryVersionDropdown = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Primary Bible Version",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = viewModel.currentVersionAbbr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = BibleVersionUtils.descriptionMap[viewModel.currentDbName] ?: "Bible translation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.showVersionInfoDialog = true
                                viewModel.versionInfoForDialog = viewModel.currentDbName
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Version info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                SettingsItem(
                    title = "Multi-Version Display",
                    subtitle = "Show two Bible versions side by side"
                ) {
                    Switch(
                        checked = viewModel.multiVersion,
                        onCheckedChange = { viewModel.multiVersion = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primary,
                            checkedTrackColor = primary.copy(alpha = 0.5f)
                        )
                    )
                }
                if (viewModel.multiVersion) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showSecondaryVersionDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Secondary Bible Version",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = viewModel.secondaryVersionAbbr.ifEmpty { "Select version" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (viewModel.secondaryVersionAbbr.isNotEmpty()) {
                                        BibleVersionUtils.descriptionMap[viewModel.secondaryDbName] ?: "Bible translation"
                                    } else {
                                        "Select a secondary version"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.showVersionInfoDialog = true
                                    viewModel.versionInfoForDialog = viewModel.secondaryDbName
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Version info",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        title = "Multi-View Layout",
                        subtitle = "Horizontal or vertical arrangement"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.multiViewLayout == "horizontal") primary.copy(alpha = 0.1f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = "Horizontal",
                                    modifier = Modifier
                                        .clickable { viewModel.multiViewLayout = "horizontal" }
                                        .padding(vertical = 8.dp, horizontal = 12.dp)
                                )
                            }
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.multiViewLayout == "vertical") primary.copy(alpha = 0.1f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = "Vertical",
                                    modifier = Modifier
                                        .clickable { viewModel.multiViewLayout = "vertical" }
                                        .padding(vertical = 8.dp, horizontal = 12.dp)
                                )
                            }
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
                                checkedThumbColor = primary,
                                checkedTrackColor = primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    SettingsItem(
                        title = "Square Aspect Ratio Views",
                        subtitle = if (viewModel.squareAspectViews) {
                            "Turn this off to disable auto square aspect ratio views when multi-version is on and device switches orientation mode"
                        } else {
                            "Turn this on to display square aspect ratio views in multi-window mode automatically when device changes orientation mode"
                        }
                    ) {
                        Switch(
                            checked = viewModel.squareAspectViews,
                            onCheckedChange = { viewModel.squareAspectViews = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primary,
                                checkedTrackColor = primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (viewModel.squareAspectViews) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            RotatingPhoneGraphics(
                                isSquareAspect = viewModel.squareAspectViews,
                                primaryColor = primary,
                                viewColor = viewColor,
                                modifier = Modifier
                            )
                        }
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
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.fontSize = maxOf(MIN_FONT_SIZE, viewModel.fontSize - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("A-", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${viewModel.fontSize}",
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = primary,
                                fontSize = 18.sp
                            )
                            IconButton(
                                onClick = { viewModel.fontSize = minOf(MAX_FONT_SIZE, viewModel.fontSize + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("A+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                SettingsItem(
                    title = "Study Mode",
                    subtitle = if (viewModel.isStudyMode) {
                        "Turn this off to remove the extra buttons for cross-references and commentaries at the end of each verse"
                    } else {
                        "Turn this on to add the extra buttons for cross-references and commentaries at the end of each verse"
                    }
                ) {
                    Switch(
                        checked = viewModel.isStudyMode,
                        onCheckedChange = { viewModel.isStudyMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primary,
                            checkedTrackColor = primary.copy(alpha = 0.5f)
                        )
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
                            checkedThumbColor = primary,
                            checkedTrackColor = primary.copy(alpha = 0.5f)
                        )
                    )
                }
                SettingsItem(
                    title = "Strong's Concordance",
                    subtitle = "Toggle strong's concordance numbers after every word on or off. This only applies to Bible translations which has a plus indicator e.g. KJV+"
                ) {
                    Switch(
                        checked = viewModel.showStrongs,
                        onCheckedChange = { viewModel.showStrongs = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primary,
                            checkedTrackColor = primary.copy(alpha = 0.5f)
                        )
                    )
                }
                if (!viewModel.isDictionaryMode) {
                    SettingsItem(
                        title = "Word Marker Color",
                        subtitle = "Color for highlighting individual words"
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(viewModel.wordMarkerColor)
                                .border(1.dp, primary.copy(0.3f), CircleShape)
                                .clickable { viewModel.showWordMarkerColorWheelDialog = true }
                        )
                    }
                }
                SettingsItem(
                    title = "Words of Jesus Color",
                    subtitle = "Color for the words of the Lord Jesus"
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(viewModel.wordsOfJesus)
                            .border(1.dp, primary.copy(0.3f), CircleShape)
                            .clickable { viewModel.showJesusWordsColorWheelDialog = true }
                    )
                }
                SettingsItem(
                    title = "Header Contents Color",
                    subtitle = "App Bar texts and buttons color"
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(viewModel.headerButtonsColor)
                            .border(1.dp, primary.copy(0.3f), CircleShape)
                            .clickable { showHeaderButtonsColorWheel = true }
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
                            .border(1.dp, primary.copy(0.3f), CircleShape)
                            .clickable { viewModel.showVerseMarkerColorWheelDialog = true }
                    )
                }
                if (viewModel.darkTheme) {
                    SettingsItem(
                        title = "Modal Background Color",
                        subtitle = "Modal background color for dark theme"
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(viewModel.darkModalBackgroundColor)
                                .border(1.dp, primary.copy(0.3f), CircleShape)
                                .clickable { viewModel.showDarkOverlayColorWheel = true }
                        )
                    }
                    SettingsItem(
                        title = "Reader Font Color",
                        subtitle = "Reader text color for dark theme"
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(viewModel.darkThemeReaderFontColor)
                                .border(1.dp, primary.copy(0.3f), CircleShape)
                                .clickable { viewModel.showDarkReaderFontColorWheelDialog = true }
                        )
                    }
                } else {
                    SettingsItem(
                        title = "Modal Background Color",
                        subtitle = "Modal background color for light theme"
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(viewModel.lightModalBackgroundColor)
                                .border(1.dp, primary.copy(0.3f), CircleShape)
                                .clickable { viewModel.showLightOverlayColorWheel = true }
                        )
                    }
                    SettingsItem(
                        title = "Reader Font Color",
                        subtitle = "Reader text color for light theme"
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(viewModel.lightThemeReaderFontColor)
                                .border(1.dp, primary.copy(0.3f), CircleShape)
                                .clickable { viewModel.showLightReaderFontColorWheelDialog = true }
                        )
                    }
                }
                SettingsItem(
                    title = "Reader BG Texture",
                    subtitle = "Choose from built-in textures or upload a custom one",
                    onClick = { viewModel.showBgModal = true }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
                if (viewModel.bgImageIndex > 0) {
                    SettingsItem(title = "") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                SettingsOpacitySlider(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }

        // ----- Word Marker Palette Colors -----
        item {
            SettingsSection(
                title = "Word Marker Palette Colors",
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

        // ----- Database Management -----
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

        // ----- More Options -----
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

        // ----- Quick Actions -----
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
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
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
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Feedback", color = Color.White)
                    }
                }
            }
        }
    }
    if (showResetHighlightColorsDialog) {
        AlertDialog(
            onDismissRequest = { showResetHighlightColorsDialog = false },
            title = { Text("Reset Highlight Colors", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("This will reset all marker colors to their default values. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetHighlightColorsToDefault()
                        showResetHighlightColorsDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetHighlightColorsDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (viewModel.showHighlightColorEditor) {
        ColorWheelDialog(
            onDismissRequest = {
                viewModel.showHighlightColorEditor = false
                viewModel.editingHighlightColorIndex = -1
            },
            onColorSelected = { color ->
                if (viewModel.editingHighlightColorIndex != -1) {
                    viewModel.updateHighlightColor(viewModel.editingHighlightColorIndex, color)
                }
                viewModel.showHighlightColorEditor = false
                viewModel.editingHighlightColorIndex = -1
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
            title = { Text("Reset All Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text("This will restore EVERY setting to its default value:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Font size, theme, colors", style = MaterialTheme.typography.bodyMedium)
                    Text("• Bible versions, layout, sync", style = MaterialTheme.typography.bodyMedium)
                    Text("• Background, overlay, palette colors", style = MaterialTheme.typography.bodyMedium)
                    Text("• Study mode, etc.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("This action cannot be undone.\n\nAre you sure you want to continue?",
                        color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Reset all ViewModel properties to defaults
                        viewModel.headerButtonsColor = Color(0xFFFFFFFF)
                        viewModel.renderOrbs = false
                        viewModel.orbsCount = 3
                        viewModel.fontSize = 18
                        viewModel.darkTheme = false
                        viewModel.selectedColor = DefaultPrimaryColor
                        viewModel.isCustomColor = false
                        viewModel.selectedFontFamily = "system"
                        viewModel.currentDbName = "kj2.sqlite3"
                        viewModel.currentVersionAbbr = BibleVersionUtils.versionMap["kj2.sqlite3"] ?: "KJ2"
                        viewModel.multiVersion = false
                        viewModel.secondaryDbName = "kjv+.sqlite3"
                        viewModel.secondaryVersionAbbr = BibleVersionUtils.versionMap["kjv+.sqlite3"] ?: "KJV+"
                        viewModel.multiViewLayout = "horizontal"
                        viewModel.scrollSync = true
                        viewModel.customTextureUri = null
                        viewModel.bgImageIndex = 0
                        viewModel.overlayOpacity = 0.8f
                        viewModel.lightOverlayColor = Color(0xFFFFFFFF)
                        viewModel.darkOverlayColor = Color(0xFF100F21)
                        viewModel.darkThemeReaderFontColor = Color(0xFFFFFFFF)
                        viewModel.lightThemeReaderFontColor = Color(0xFF101015)
                        viewModel.lightModalBackgroundColor = Color(0xFFEAE7E3)
                        viewModel.darkModalBackgroundColor = Color(0xFF121523)
                        viewModel.wordMarkerColor = Color(0xDDAC95E1)
                        viewModel.verseMarkerColor = Color(0xFF95F198)
                        viewModel.isDictionaryMode = true
                        viewModel.isStudyMode = false
                        viewModel.showStrongs = false
                        viewModel.wordsOfJesus = Color(0xFFDA4227)
                        viewModel.resetHighlightColorsToDefault()
                        viewModel.squareAspectViews = true
                        viewModel.scrollSyncAction = false
                        viewModel.isReaderFullScreen = false
                        viewModel.showNavigationModal = false
                        viewModel.showPrimaryVersionDropdown = false
                        viewModel.showSecondaryVersionDropdown = false
                        viewModel.showColorThemeDialog = false
                        viewModel.showColorWheelDialog = false
                        viewModel.showBgModal = false
                        viewModel.editingHighlightColorIndex = -1
                        viewModel.showHighlightColorEditor = false
                        viewModel.showWordMarkerColorWheelDialog = false
                        viewModel.showJesusWordsColorWheelDialog = false
                        viewModel.showLightReaderFontColorWheelDialog = false
                        viewModel.showDarkReaderFontColorWheelDialog = false
                        viewModel.showDarkOverlayColorWheel = false
                        viewModel.showLightOverlayColorWheel = false
                        viewModel.showVerseMarkerColorWheelDialog = false
                        viewModel.showSecondaryNavigationModal = false
                        viewModel.showReaderOverlayColorWheel = false
                        viewModel.primaryPassage = PassageSelection(10, "Genesis", 1, 1)
                        viewModel.secondaryPassage = PassageSelection(500, "John", 1, 1)
                        viewModel.selectedPrimaryDictLanguage = "English"
                        viewModel.selectedSecondaryDictLanguage = "English"
                        viewModel.selectedPrimaryDictionary = "atsbd"
                        viewModel.selectedSecondaryDictionary = "cbtel"
                        viewModel.selectedVerseCommentary = "cbsc"
                        viewModel.selectedCrossReferenceDatabase = "obx"
                        viewModel.isRefreshingDatabases = false
                        viewModel.lastRefreshMessage = ""
                        viewModel.lastRefreshSuccess = false
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset All") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
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
    if (showOrbsCountModal) {
        OrbsCountModal(
            tempSize = tempOrbsCount,
            onChange = { tempOrbsCount = it },
            onConfirm = {
                val newSize = tempOrbsCount.toIntOrNull() ?: viewModel.orbsCount
                viewModel.orbsCount = newSize.coerceIn(MIN_ORB_COUNT, MAX_ORB_COUNT)
                showOrbsCountModal = false
            },
            onDismiss = { showOrbsCountModal = false },
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
    if (showHeaderButtonsColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { showHeaderButtonsColorWheel = false },
            onColorSelected = { color ->
                viewModel.headerButtonsColor = color
                showHeaderButtonsColorWheel = false
            },
            initialColor = viewModel.headerButtonsColor
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
                    Text("• Recopy all Bible versions from assets")
                    Text("• Recopy dictionary databases")
                    Text("• Recopy commentary databases")
                    Text("• Note: Your bookmarks and settings will not be affected")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This may take a few moments. Continue?", color = primary, fontWeight = FontWeight.Medium)
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
                ) { Text("Refresh") }
            },
            dismissButton = {
                TextButton(onClick = { showRefreshConfirmDialog = false }) { Text("Cancel") }
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Please wait...")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(viewModel.lastRefreshMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        if (viewModel.lastRefreshSuccess) {
                            Icon(Icons.Default.Refresh, contentDescription = "Success", tint = primary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(viewModel.lastRefreshMessage, style = MaterialTheme.typography.bodyMedium)
                        if (!viewModel.lastRefreshSuccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Try restarting the app if issues persist.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                if (!viewModel.isRefreshingDatabases) {
                    TextButton(onClick = {
                        showRefreshResultDialog = false
                        viewModel.lastRefreshMessage = ""
                    }) { Text("OK") }
                }
            }
        )
    }
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}