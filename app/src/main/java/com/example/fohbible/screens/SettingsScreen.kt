package com.example.fohbible.screens

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.AppViewModel
import com.example.fohbible.ColorWheelDialog
import com.example.fohbible.ui.theme.DefaultPrimaryColor
import com.example.fohbible.ui.theme.PredefinedColorThemes
import com.example.fohbible.utils.BibleVersionUtils
import java.util.Locale

private const val MAX_FONT_SIZE = 50
private const val MIN_FONT_SIZE = 8

val availableFontFamilies = listOf("system", "oswald", "rubik-glitch", "poppins")

// TODO: Define bgTextures similar to RN

@Composable
fun getFontFamily(family: String): FontFamily {
    val context = LocalContext.current
    return when (family) {
        "system" -> FontFamily.Default
        "oswald" -> remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Oswald.ttf")) }
        "rubik-glitch" -> remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/RubikGlitch.ttf")) }
        "poppins" -> remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Poppins.ttf")) }
        else -> FontFamily.Default
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
fun SettingsScreen() {
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current
    var showVersionInfoDialog by remember { mutableStateOf(false) }
    var selectedVersionInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var bgImageIndex by remember { mutableIntStateOf(0) }
    var customTextureUri by remember { mutableStateOf<String?>(null) }
    var showBgModal by remember { mutableStateOf(false) }
    var showFontModal by remember { mutableStateOf(false) }
    var tempFontSize by remember { mutableStateOf(viewModel.fontSize.toString()) }
    var showColorWheel by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(viewModel.customColor) }
    var isUsingCustomColor by remember { mutableStateOf(viewModel.isCustomColor) }

    LaunchedEffect(viewModel.isCustomColor, viewModel.customColor) {
        isUsingCustomColor = viewModel.isCustomColor
        customColor = viewModel.customColor
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customTextureUri = it.toString()
            bgImageIndex = 34 // TODO: Persist uri
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Customize your Bible reading experience",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            SettingsSection(title = "Bible Version", subtitle = "Choose your preferred translation") {
                BibleVersionSelector(
                    title = "Primary Bible Version",
                    currentAbbr = viewModel.currentVersionAbbr,
                    description = BibleVersionUtils.descriptionMap[viewModel.currentDbName] ?: "Bible translation",
                    onVersionSelected = { file, abbr ->
                        viewModel.currentDbName = file
                        viewModel.currentVersionAbbr = abbr
                    },
                    onInfoClick = { file, abbr ->
                        selectedVersionInfo = Pair(
                            abbr,
                            BibleVersionUtils.descriptionMap[file] ?: "No description available"
                        )
                        showVersionInfoDialog = true
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
                        },
                        onInfoClick = { file, abbr ->
                            selectedVersionInfo = Pair(
                                abbr,
                                BibleVersionUtils.descriptionMap[file] ?: "No description available"
                            )
                            showVersionInfoDialog = true
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
                                    .background(if (viewModel.multiViewLayout == "horizontal") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(8.dp)
                            )
                            Text(
                                text = "Vertical",
                                modifier = Modifier
                                    .clickable { viewModel.multiViewLayout = "vertical" }
                                    .background(if (viewModel.multiViewLayout == "vertical") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
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
            }
        }
        item {
            SettingsSection(title = "Reader Settings", subtitle = "Customize reading experience") {
                SettingsItem(title = "Dark Mode", subtitle = "Toggle between light and dark themes") {
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
                            modifier = Modifier.padding(horizontal = 8.dp)
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
            }
        }
        item {
            SettingsSection(title = "More Options", subtitle = "Additional preferences") {
                SettingsItem(
                    title = "Data & Storage",
                    subtitle = "Manage app data and cache",
                    onClick = { /* TODO: Implement */ }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
                HorizontalDivider()
                SettingsItem(
                    title = "About",
                    subtitle = "App version and information",
                    onClick = { /* TODO: Show about dialog */ }
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
                        onClick = {
                            // Reset all settings to default
                            viewModel.fontSize = 18
                            viewModel.darkTheme = false
                            viewModel.selectedColor = DefaultPrimaryColor
                            viewModel.isCustomColor = false
                            viewModel.selectedFontFamily = "system"
                            viewModel.currentDbName = "kj2.sqlite3"
                            viewModel.currentVersionAbbr = BibleVersionUtils.versionMap["kj2.sqlite3"] ?: "KJ2"
                            viewModel.multiVersion = false
                            viewModel.secondaryDbName = "nkjv.sqlite3"
                            viewModel.secondaryVersionAbbr = BibleVersionUtils.versionMap["kj2.sqlite3"] ?: "KJ2"
                            viewModel.multiViewLayout = "horizontal"
                            viewModel.scrollSync = true
                            customTextureUri = null
                            bgImageIndex = 0
                        },
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

    if (showVersionInfoDialog && selectedVersionInfo != null) {
        VersionInfoDialog(
            versionName = selectedVersionInfo!!.first,
            versionDescription = selectedVersionInfo!!.second,
            onDismiss = {
                showVersionInfoDialog = false
                selectedVersionInfo = null
            }
        )
    }

    if (showBgModal) {
        BgModal(
            currentIndex = bgImageIndex,
            customUri = customTextureUri,
            onSelect = { index ->
                bgImageIndex = index
                showBgModal = false
            },
            onDismiss = { showBgModal = false },
            onPickCustom = { imagePickerLauncher.launch("image/*") },
            onRemoveCustom = { customTextureUri = null }
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
            onDismiss = { showFontModal = false }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVersionSelector(
    title: String,
    currentAbbr: String,
    description: String,
    onVersionSelected: (String, String) -> Unit,
    onInfoClick: (String, String) -> Unit
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
                    .menuAnchor(enabled = true, type = MenuAnchorType.PrimaryEditable)
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
                    // Info button
                    IconButton(
                        onClick = {
                            val selectedFile = BibleVersionUtils.versionMap.entries
                                .find { it.value == currentAbbr }
                                ?.key
                            if (selectedFile != null) {
                                onInfoClick(selectedFile, currentAbbr)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Version info",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Dropdown arrow
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
                // TODO: Add LazyRow for texture previews
                Text("Texture previews will appear here")
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
fun FontModal(
    tempSize: String,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Font Size",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Enter font size ($MIN_FONT_SIZE-$MAX_FONT_SIZE):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempSize,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            onChange(newValue)
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter font size") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = tempSize.toIntOrNull()?.let { it in MIN_FONT_SIZE..MAX_FONT_SIZE } ?: false
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}