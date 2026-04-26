package com.fountofhopedotorg.fohbible.composables

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.ColorWheelDialog
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.allScreens
import com.fountofhopedotorg.fohbible.data.ColorTheme
import com.fountofhopedotorg.fohbible.dropdowns.ReaderAppBarMenu
import com.fountofhopedotorg.fohbible.dropdowns.WindowsLayoutDropdown
import com.fountofhopedotorg.fohbible.modals.FontModal
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.ui.theme.PredefinedColorThemes
import com.fountofhopedotorg.fohbible.ui.theme.ThemeManager.primaryColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading verses...")
    }
}

@Composable
fun InteractiveLoadingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Loading...")
    }
}

@Composable
fun <T> SavePreference(
    getValue: () -> T,
    key: Preferences.Key<T>,
    dataStore: DataStore<Preferences>
) {
    LaunchedEffect(Unit) {
        snapshotFlow { getValue() }.collectLatest { value ->
            dataStore.edit { prefs -> prefs[key] = value }
        }
    }
}
@Composable
fun SaveNullableStringPreference(
    getValue: () -> String?,
    key: Preferences.Key<String>,
    dataStore: DataStore<Preferences>
) {
    LaunchedEffect(Unit) {
        snapshotFlow { getValue() }.collectLatest { uri ->
            dataStore.edit { prefs ->
                if (uri != null) prefs[key] = uri
                else prefs.remove(key)
            }
        }
    }
}

@Composable
fun FontSizeControls(viewModel: AppViewModel) {
    val minFontSize = 1
    val maxFontSize = 100
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var tempFontSize by remember { mutableStateOf(viewModel.fontSize.toString()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Font Size", style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
            Text(
                "1-100",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.fontSize = maxOf(minFontSize, viewModel.fontSize - 1)
                }, modifier = Modifier.size(32.dp)) {
                    Text("A-", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${viewModel.fontSize}",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { showFontSizeDialog = true },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    viewModel.fontSize = minOf(maxFontSize, viewModel.fontSize + 1)
                }, modifier = Modifier.size(32.dp)) {
                    Text("A+", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showFontSizeDialog) {
        FontModal(
            tempSize = tempFontSize,
            onChange = { tempFontSize = it },
            onConfirm = {
                val newSize = tempFontSize.toIntOrNull()?.coerceIn(minFontSize, maxFontSize) ?: viewModel.fontSize
                viewModel.fontSize = newSize
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false },
            appViewModel = viewModel
        )
    }
    if (viewModel.showLightOverlayColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { viewModel.showLightOverlayColorWheel = false },
            onColorSelected = { color ->
                viewModel.lightOverlayColor = color
                viewModel.showLightOverlayColorWheel = false
            },
            initialColor = viewModel.lightOverlayColor
        )
    }
    if (viewModel.showDarkOverlayColorWheel) {
        ColorWheelDialog(
            onDismissRequest = { viewModel.showDarkOverlayColorWheel = false },
            onColorSelected = { color ->
                viewModel.darkOverlayColor = color
                viewModel.showDarkOverlayColorWheel = false
            },
            initialColor = viewModel.darkOverlayColor
        )
    }
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    tint: Color = viewModel.headerButtonsColor
) {
    var targetRotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(300),
        label = "iconRotation"
    )
    IconButton(
        onClick = {
            targetRotation += rotation
            onClick()
        },
        modifier = modifier
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.rotate(animatedRotation))
    }
}
@Composable
fun ColorPickerRow(
    label: String,
    iconSize: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = { Text(label, modifier = Modifier.fillMaxWidth()) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
            )
        },
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOpacitySlider(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reader BG Overlay",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(viewModel.overlayOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "Adjust overlay opacity with slider and set color with button",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Slider(
                value = viewModel.overlayOpacity,
                onValueChange = { viewModel.overlayOpacity = it },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .shadow(2.dp, shape = CircleShape)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            )

            if (viewModel.darkTheme) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(viewModel.darkOverlayColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        .clickable { viewModel.showDarkOverlayColorWheel = true }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(viewModel.lightOverlayColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        .clickable { viewModel.showLightOverlayColorWheel = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayOpacitySlider(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Background Overlay", style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
            Text("${(viewModel.overlayOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((-20).dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(0.3f)) {
                if (viewModel.darkTheme) {
                    ColorPickerRow(
                        label = "",
                        iconSize = 22,
                        color = viewModel.darkOverlayColor,
                        onClick = { viewModel.showDarkOverlayColorWheel = true })
                } else {
                    ColorPickerRow(
                        label = "",
                        iconSize = 22,
                        color = viewModel.lightOverlayColor,
                        onClick = { viewModel.showLightOverlayColorWheel = true })
                }
            }
            Box(modifier = Modifier.fillMaxWidth(1f)) {
                Slider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp),
                    value = viewModel.overlayOpacity,
                    onValueChange = { viewModel.overlayOpacity = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        inactiveTickColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .shadow(2.dp, shape = CircleShape)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ScrollSyncButton(viewModel: AppViewModel, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val pendingJob = remember { mutableStateOf<Job?>(null) }
    val icon = if (viewModel.scrollSync) Icons.Filled.Link else Icons.Filled.LinkOff

    AnimatedIconButton(
        onClick = {
            if (pendingJob.value?.isActive == true) return@AnimatedIconButton
            pendingJob.value = scope.launch {
                delay(250L)
                viewModel.scrollSync = !viewModel.scrollSync
                pendingJob.value = null
                viewModel.scrollSyncAction = true
            }
        },
        icon = icon,
        contentDescription = "Toggle Scroll Sync",
        modifier = modifier,
        rotation = 180f,
        viewModel = viewModel
    )
}

@Composable
fun DropdownMenuItemWithIcon(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(200),
        label = "dropdownBackground"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "dropdownTextColor"
    )
    DropdownMenuItem(
        text = { Text(title, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = textColor) },
        onClick = onClick,
        modifier = modifier.background(backgroundColor),
        leadingIcon = {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (title == "Notes") iconModifier.rotate(90f) else iconModifier
            )
        }
    )
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
    onBack: (() -> Unit)? = null,
    appViewModel: AppViewModel
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val topAppBarHeight = if (isLandscape) 40.dp else 80.dp
    val iconSize = if (isLandscape) 45.dp else 40.dp
    var showNavigationDropdown by remember { mutableStateOf(false) }
    val viewModel: AppViewModel = viewModel()
    val rotation by animateFloatAsState(targetValue = if (showNavigationDropdown) 180f else 0f, animationSpec = tween(300), label = "menuIconRotation")
    val screenTitle = when (currentScreen) {
        is Screen.Home -> "Home"
        is Screen.Reader -> "Reader"
        is Screen.Bookmarks -> "Bookmarks"
        is Screen.Notes -> "Notes"
        is Screen.Settings -> "Settings"
        is Screen.Search -> "Search"
    }
    TopAppBar(
        title = { Text(text = screenTitle, color = viewModel.headerButtonsColor, fontWeight = FontWeight.Bold, modifier = Modifier
            .padding(start = 0.dp), textAlign = TextAlign.Start) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = modifier
            .height(topAppBarHeight)
            .background(Brush.verticalGradient(0.6f to LocalAppTheme.current.primaryColor, 0.85f to LocalAppTheme.current.primaryColor, 1.0f to Color.Transparent)),
        navigationIcon = {
            if (onBack != null) {
                AnimatedIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    viewModel = viewModel
                )
            }
        },
        actions = {
            Row(modifier = Modifier.padding( horizontal = 15.dp)) {
                AnimatedIconButton(
                    onClick = onBibleIconClick,
                    icon = Icons.Filled.Book,
                    contentDescription = "Bible Navigation",
                    modifier = Modifier
                        .size(iconSize),
                    rotation = 360f,
                    viewModel = viewModel
                )
                AnimatedIconButton(
                    onClick = onThemeToggle,
                    icon = if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2,
                    contentDescription = "Toggle Theme",
                    modifier = Modifier
                        .size(iconSize),
                    rotation = 180f,
                    viewModel = viewModel
                )
                AnimatedIconButton(
                    onClick = onColorLensClick,
                    icon = Icons.Filled.ColorLens,
                    contentDescription = "Color Scheme",
                    modifier = Modifier
                        .size(iconSize),
                    rotation = 180f,
                    viewModel = viewModel
                )
                IconButton(onClick = { showNavigationDropdown = !showNavigationDropdown }, modifier = Modifier.rotate(rotation).size(iconSize)) {
                    Crossfade(targetState = showNavigationDropdown, animationSpec = tween(300), label = "iconCrossfade") { isOpen ->
                        Icon(imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Menu, contentDescription = if (isOpen) "Close Navigation" else "Open Navigation", tint = viewModel.headerButtonsColor)
                    }
                }
            }
            DropdownMenu(
                expanded = showNavigationDropdown,
                onDismissRequest = { showNavigationDropdown = false },
                modifier = Modifier.background(if (appViewModel.darkTheme) appViewModel.darkModalBackgroundColor else appViewModel.lightModalBackgroundColor)
            ) {
                allScreens.forEach { (title, icon) ->
                    val isActive = when (title) {
                        "Home" -> currentScreen is Screen.Home
                        "Reader" -> currentScreen is Screen.Reader
                        "Bookmarks" -> currentScreen == Screen.Bookmarks
                        "Notes" -> currentScreen == Screen.Notes
                        "Search" -> currentScreen == Screen.Search
                        "Settings" -> currentScreen == Screen.Settings
                        else -> false
                    }
                    DropdownMenuItemWithIcon(
                        title = title,
                        icon = icon,
                        isActive = isActive,
                        onClick = {
                            val targetScreen = when (title) {
                                "Home" -> Screen.Home
                                "Reader" -> Screen.Reader()
                                "Bookmarks" -> Screen.Bookmarks
                                "Notes" -> Screen.Notes
                                "Search" -> Screen.Search
                                "Settings" -> Screen.Settings
                                else -> Screen.Home
                            }
                            onScreenChange(targetScreen)
                            showNavigationDropdown = false
                        }
                    )
                }
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
    onBack: (() -> Unit)? = null,
) {
    val viewModel: AppViewModel = viewModel()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val topAppBarHeight = if (isLandscape) 40.dp else 80.dp
    val iconSize = if (isLandscape) 45.dp else 40.dp

    TopAppBar(
        title = {
            if (!viewModel.multiVersion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBibleIconClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(25.dp)
                            .weight(if (isLandscape) 2f else 1f)
                            .padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentScreen.passage?.bookName ?: "Reader",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = viewModel.headerButtonsColor,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = currentScreen.passage?.chapter?.let { " $it" } ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = viewModel.headerButtonsColor,
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.showPrimaryVersionDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(25.dp)
                            .weight(if (isLandscape) 1f else 0.8f)
                            .padding(end = if (onBack == null) 8.dp else 2.dp)
                    ) {
                        Text(
                            text = currentVersionAbbr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = viewModel.headerButtonsColor,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = modifier
            .height(topAppBarHeight)
            .background(
                Brush.verticalGradient(
                    0.6f to LocalAppTheme.current.primaryColor,
                    0.85f to LocalAppTheme.current.primaryColor,
                    1.0f to Color.Transparent
                )
            ),
        navigationIcon = {
            if (onBack != null) {
                AnimatedIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    viewModel = viewModel
                )
            }
        },
        actions = {
            if (!viewModel.multiVersion || !isLandscape && viewModel.multiViewLayout == "horizontal") {
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    if (isLandscape) {
                        Spacer(modifier = Modifier.width(300.dp))
                    } else {
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    AnimatedIconButton(
                        onClick = onThemeToggle,
                        icon = if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2,
                        contentDescription = "Toggle Theme",
                        modifier = Modifier.size(iconSize),
                        rotation = 180f,
                        viewModel = viewModel
                    )
                    AnimatedIconButton(
                        onClick = onColorLensClick,
                        icon = Icons.Filled.ColorLens,
                        contentDescription = "Color Scheme",
                        modifier = Modifier.size(iconSize),
                        rotation = 180f,
                        viewModel = viewModel
                    )
                    if (viewModel.multiVersion) {
                        ScrollSyncButton(viewModel = viewModel, modifier = Modifier.size(iconSize))
                    }
                    WindowsLayoutDropdown(
                        viewModel = viewModel,
                        modifier = Modifier.size(iconSize)
                    )
                    ReaderAppBarMenu(
                        isLandscape = isLandscape,
                        viewModel = viewModel,
                        onScreenChange = onScreenChange,
                        coroutineScope = rememberCoroutineScope(),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    )
}

@Composable
fun ReaderDropdownContent(
    isLandscape: Boolean,
    viewModel: AppViewModel,
    onScreenChange: (Screen) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val commonItems = @Composable {
        allScreens.forEach { (title, icon) ->
            val isActive = when (title) {
                "Reader" -> true
                else -> false
            }
            DropdownMenuItemWithIcon(
                title = title,
                icon = icon,
                isActive = isActive,
                onClick = {
                    val targetScreen = when (title) {
                        "Home" -> Screen.Home
                        "Reader" -> Screen.Reader()
                        "Bookmarks" -> Screen.Bookmarks
                        "Notes" -> Screen.Notes
                        "Search" -> Screen.Search
                        "Settings" -> Screen.Settings
                        else -> Screen.Home
                    }
                    onScreenChange(targetScreen)
                }
            )
        }
        if (!isLandscape) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Background Texture", modifier = Modifier.fillMaxWidth()) },
                leadingIcon = { Icon(Icons.Outlined.Texture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { viewModel.showBgModal = true }
            )
            HorizontalDivider()
            if (viewModel.bgImageIndex != 0) {
                OverlayOpacitySlider(viewModel)
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(text = if (viewModel.isDictionaryMode) "Dictionary Mode On" else "Word Marker On", modifier = Modifier.fillMaxWidth()) },
                leadingIcon = { Icon(if (viewModel.isDictionaryMode) Icons.AutoMirrored.Filled.Label else Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    viewModel.isDictionaryMode = !viewModel.isDictionaryMode
                    coroutineScope.launch { delay(400) }
                }
            )
            if (!viewModel.isDictionaryMode) {
                HorizontalDivider()
                ColorPickerRow(label = "Word Marker Color", iconSize = 22, color = viewModel.wordMarkerColor, onClick = { viewModel.showWordMarkerColorWheelDialog = true })
            }
            else {
                HorizontalDivider()
                ColorPickerRow(label = "Verse Marker Color", iconSize = 22, color = viewModel.verseMarkerColor, onClick = { viewModel.showVerseMarkerColorWheelDialog = true })
            }
            HorizontalDivider()
            ColorPickerRow(label = "Jesus' Words Color", iconSize = 22, color = viewModel.wordsOfJesus, onClick = { viewModel.showJesusWordsColorWheelDialog = true })
            HorizontalDivider()
            if (viewModel.darkTheme) {
                ColorPickerRow(
                    label = "Font Color",
                    iconSize = 22,
                    color = viewModel.darkThemeReaderFontColor,
                    onClick = { viewModel.showDarkReaderFontColorWheelDialog= true }
                )
            }
            else {
                ColorPickerRow(
                    label = "Font Color",
                    iconSize = 22,
                    color = viewModel.lightThemeReaderFontColor,
                    onClick = { viewModel.showLightReaderFontColorWheelDialog= true }
                )
            }
            HorizontalDivider()
            FontSizeControls(viewModel)
        }
    }
    if (isLandscape) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)) {
                commonItems()
                if (viewModel.bgImageIndex != 0) {
                    DropdownMenuItem(
                        text = { Text("Background Texture", modifier = Modifier.fillMaxWidth()) },
                        leadingIcon = { Icon(Icons.Outlined.Texture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { viewModel.showBgModal = true }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            VerticalDivider()
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)) {
                ExtraReaderControls(viewModel, coroutineScope)
            }
        }
    } else {
        Column { commonItems() }
    }
}
@Composable
fun ExtraReaderControls(viewModel: AppViewModel, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    if (viewModel.bgImageIndex != 0) {
        OverlayOpacitySlider(viewModel)
        HorizontalDivider()
    } else {
        DropdownMenuItem(
            text = { Text("Background Texture", modifier = Modifier.fillMaxWidth()) },
            leadingIcon = { Icon(Icons.Outlined.Texture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            onClick = { viewModel.showBgModal = true }
        )
        HorizontalDivider()
    }

    DropdownMenuItem(
        text = { Text(text = if (viewModel.isDictionaryMode) "Dictionary Mode On" else "Word Marker On", modifier = Modifier.fillMaxWidth()) },
        leadingIcon = { Icon(if (viewModel.isDictionaryMode) Icons.AutoMirrored.Filled.Label else Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        onClick = {
            viewModel.isDictionaryMode = !viewModel.isDictionaryMode
            coroutineScope.launch { delay(400) }
        }
    )
    if (!viewModel.isDictionaryMode) {
        HorizontalDivider()
        ColorPickerRow(label = "Word Marker Color", iconSize = 22, color = viewModel.wordMarkerColor, onClick = { viewModel.showWordMarkerColorWheelDialog = true })
    }
    else {
        HorizontalDivider()
        ColorPickerRow(label = "Verse Marker Color", iconSize = 22, color = viewModel.verseMarkerColor, onClick = { viewModel.showVerseMarkerColorWheelDialog = true })
    }
    HorizontalDivider()
    ColorPickerRow(label = "Jesus' Words Color", iconSize = 22, color = viewModel.wordsOfJesus, onClick = { viewModel.showJesusWordsColorWheelDialog = true })
    HorizontalDivider()
    if (viewModel.darkTheme) {
        ColorPickerRow(
            label = "Font Color",
            iconSize = 22,
            color = viewModel.darkThemeReaderFontColor,
            onClick = { viewModel.showDarkReaderFontColorWheelDialog= true }
        )
    }
    else {
        ColorPickerRow(
            label = "Font Color",
            iconSize = 22,
            color = viewModel.lightThemeReaderFontColor,
            onClick = { viewModel.showLightReaderFontColorWheelDialog= true }
        )
    }
    HorizontalDivider()
    FontSizeControls(viewModel)
}

@Composable
fun ColorThemeDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    appViewModel: AppViewModel
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Card(
        modifier = Modifier
            .fillMaxWidth().fillMaxHeight(if (isLandscape) 1f else 0.75f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appViewModel.darkTheme) appViewModel.darkModalBackgroundColor else appViewModel.lightModalBackgroundColor
        )
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)) {
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 20.dp).background(LocalAppTheme.current.primaryColor),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Choose Theme Color",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) { Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    ) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(PredefinedColorThemes) { theme ->
                    ColorOptionItem(theme = theme, onClick = { onColorSelected(theme.primaryColor); onDismiss() })
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Custom Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable(onClick = onCustomColorClick),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                Text("Custom Color Picker", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Choose any color with color wheel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(end = 20.dp, bottom = 20.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)) {
                    Text("Cancel")
                }
            }
        }
    }
}
@Composable
fun ColorOptionItem(theme: ColorTheme, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.primaryColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    theme.primaryColor,
                                    theme.secondaryColor
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(theme.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Primary & Secondary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun FeedbackPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun ColorSplashCanvas() {
    val splashes = remember {
        List(6) {
            val baseColor = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            Triple(
                baseColor,
                Offset(Random.nextFloat(), Random.nextFloat()),
                Random.nextFloat() * 0.8f + 0.2f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            splashes.forEach { (color, pos, scale) ->
                val center = Offset(pos.x * size.width, pos.y * size.height)
                val radius = size.minDimension * scale

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.8f), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
    }
}