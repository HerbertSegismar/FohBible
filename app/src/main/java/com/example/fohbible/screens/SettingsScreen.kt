package com.example.fohbible.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.AppViewModel
import com.example.fohbible.ui.theme.PredefinedColorThemes
import com.example.fohbible.utils.BibleVersionUtils
import java.util.Locale


// TODO: Define font families similar to RN Fonts
val availableFontFamilies = listOf("system", "serif", "sans-serif", "oswald", "rubik-glitch", "poppins")

// TODO: Define bgTextures similar to RN

@Composable
fun SettingsScreen() {
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current

    // Additional states
    var fontFamily by remember { mutableStateOf("system") } // TODO: Move to viewModel and persist
    var showMultiVersion by remember { mutableStateOf(false) } // TODO: Move to viewModel and persist
    var secondaryVersionAbbr by remember { mutableStateOf("") } // TODO: Move to viewModel and persist
    var bgImageIndex by remember { mutableIntStateOf(0) } // TODO: Move to viewModel and persist
    var customTextureUri by remember { mutableStateOf<String?>(null) } // TODO: Move to viewModel and persist

    var showBgModal by remember { mutableStateOf(false) }
    var showFontModal by remember { mutableStateOf(false) }
    var tempFontSize by remember { mutableStateOf(viewModel.fontSize.toString()) }

    // Image picker for custom texture
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customTextureUri = it.toString()
            bgImageIndex = 34 // Assuming 34 is custom
            // TODO: Persist uri
        }
    }

    // TODO: Load settings from SharedPreferences in LaunchedEffect

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
            SettingsSection(title = "Reader Settings", subtitle = "Customize reading experience") {
                SettingsItem(title = "Dark Mode", subtitle = "Toggle between light and dark themes") {
                    Switch(
                        checked = viewModel.darkTheme,
                        onCheckedChange = { viewModel.darkTheme = it }
                    )
                }
                HorizontalDivider()
                Column {
                    Text("Color Scheme", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PredefinedColorThemes) { theme ->
                            ColorButton(
                                color = theme.primaryColor,
                                name = theme.name,
                                isSelected = viewModel.selectedColor == theme.primaryColor,
                                onClick = {
                                    viewModel.selectedColor = theme.primaryColor
                                    viewModel.isCustomColor = false
                                }
                            )
                        }
                        item {
                            // Custom color
                            ColorButton(
                                color = Color.Magenta, // Placeholder
                                name = "Custom",
                                isSelected = viewModel.isCustomColor,
                                onClick = { /* TODO: Open color picker */ }
                            )
                        }
                    }
                }
                HorizontalDivider()
                Column {
                    Text("Font Family", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableFontFamilies) { family ->
                            FontButton(
                                family = family,
                                isSelected = fontFamily == family,
                                onClick = {
                                    fontFamily = family
                                    /* TODO: Apply font */
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
                SettingsItem(title = "Custom Background", subtitle = "Add your own photo as background") {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                    }
                    // TODO: Show preview if customTextureUri != null
                }
                SettingsItem(title = "Background Texture", subtitle = "Choose from built-in textures or your custom one", onClick = { showBgModal = true }) {
                    Text("Texture $bgImageIndex") // TODO: Proper name
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Font Size")
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.fontSize = maxOf(8, viewModel.fontSize - 1) }) {
                        Text("A-")
                    }
                    Text(viewModel.fontSize.toString())
                    IconButton(onClick = { viewModel.fontSize = minOf(50, viewModel.fontSize + 1) }) {
                        Text("A+")
                    }
                }
                SettingsItem(title = "Multi-Version Display", subtitle = "Show two Bible versions side by side") {
                    Switch(
                        checked = showMultiVersion,
                        onCheckedChange = { showMultiVersion = it }
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Bible Version", subtitle = "Choose your preferred translation") {
                // Primary version selector
                VersionSelector(
                    currentAbbr = viewModel.currentVersionAbbr,
                    onSelect = { file, abbr ->
                        viewModel.currentDbName = file
                        viewModel.currentVersionAbbr = abbr
                    }
                )
                if (showMultiVersion) {
                    // Secondary version selector
                    VersionSelector(
                        currentAbbr = secondaryVersionAbbr,
                        onSelect = { _, abbr ->
                            secondaryVersionAbbr = abbr
                            // TODO: Set secondaryDbName
                        },
                        title = "Secondary Bible Version"
                    )
                }
            }
        }

        item {
            SettingsSection(title = "More Options", subtitle = "Additional preferences") {
                SettingsItem(title = "Data & Storage", subtitle = "Manage app data and cache", onClick = { /* TODO: Implement */ }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
                HorizontalDivider()
                SettingsItem(title = "About", subtitle = "App version and information", onClick = { /* TODO: Show about dialog */ }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            SettingsSection(title = "Quick Actions", subtitle = "Common tasks") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { /* TODO: Reset all settings */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text("Reset Settings")
                    }
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:fountofhopedevotionals@gmail.com".toUri()
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = null) // Placeholder
                        Text("Send Feedback")
                    }
                }
            }
        }
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
                viewModel.fontSize = tempFontSize.toIntOrNull() ?: viewModel.fontSize
                showFontModal = false
            },
            onDismiss = { showFontModal = false }
        )
    }
}

@Composable
fun SettingsSection(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        content()
    }
}

@Composable
fun HorizontalDivider() {
    Spacer(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
fun ColorButton(color: Color, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(32.dp).background(color))
        Text(name)
    }
}

@Composable
fun FontButton(family: String, isSelected: Boolean, onClick: () -> Unit) {
    // TODO: Apply font family to text
    Text(
        family.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
        modifier = Modifier
            .clickable { onClick() }
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    )
}

@Composable
fun VersionSelector(currentAbbr: String, onSelect: (String, String) -> Unit, title: String? = null) {
    var showDropdown by remember { mutableStateOf(false) }
    Column {
        title?.let { Text(it) }
        Button(onClick = { showDropdown = true }) {
            Text(currentAbbr)
        }
        DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
            BibleVersionUtils.versionMap.forEach { (file, abbr) ->
                DropdownMenuItem(text = { Text(abbr) }, onClick = {
                    onSelect(file, abbr)
                    showDropdown = false
                })
            }
        }
    }
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
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Background")
                LazyColumn {
                    // TODO: Add items for textures 0 to 33 and custom (34)
                    item {
                        Text("None", modifier = Modifier.clickable { onSelect(0) })
                    }
                    // ... add others
                    item {
                        Row {
                            Text("Custom")
                            if (customUri != null) {
                                IconButton(onClick = onRemoveCustom) {
                                    Icon(Icons.Default.Close, null)
                                }
                            } else {
                                IconButton(onClick = onPickCustom) {
                                    Icon(Icons.Default.Add, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FontModal(tempSize: String, onChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Font Size") },
        text = {
            OutlinedTextField(
                value = tempSize,
                onValueChange = onChange,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}