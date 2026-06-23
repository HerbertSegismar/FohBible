package com.fountofhopedotorg.fohbible.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import java.util.Locale

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
fun VersionManagementDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = if (viewModel.darkTheme) viewModel.darkModalBackgroundColor
    else viewModel.lightModalBackgroundColor
    val textColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val activeVersionKeys = remember(viewModel.currentDbName, viewModel.secondaryDbName, viewModel.multiVersion) {
        mutableSetOf(viewModel.currentDbName).apply {
            if (viewModel.multiVersion) add(viewModel.secondaryDbName)
        }
    }
    val nonActiveKeys = remember(activeVersionKeys) {
        BibleVersionUtils.versionMap.keys - activeVersionKeys
    }
    val allNonActiveEnabled = nonActiveKeys.none { it in viewModel.disabledVersions }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(200)) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)) +
                    scaleOut(targetScale = 0.9f, animationSpec = tween(150))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .shadow(24.dp, RoundedCornerShape(10.dp), clip = false),
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(primary)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Bible Versions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Close", tint = Color.White)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Select All",
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            fontSize = 15.sp
                        )
                        Switch(
                            checked = allNonActiveEnabled,
                            onCheckedChange = { enableAll ->
                                viewModel.disabledVersions = if (enableAll) {
                                    viewModel.disabledVersions - nonActiveKeys
                                } else {
                                    viewModel.disabledVersions + nonActiveKeys
                                }
                            },
                            enabled = nonActiveKeys.isNotEmpty(),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primary,
                                checkedTrackColor = primary.copy(alpha = 0.5f),
                                disabledCheckedThumbColor = primary.copy(alpha = 0.3f),
                                disabledCheckedTrackColor = primary.copy(alpha = 0.1f),
                                disabledUncheckedThumbColor = mutedColor.copy(alpha = 0.3f),
                                disabledUncheckedTrackColor = mutedColor.copy(alpha = 0.1f)
                            )
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    val allVersions = remember { BibleVersionUtils.versionMap }
                    val grouped = remember {
                        val groups = mutableMapOf<String, MutableList<Pair<String, String>>>()
                        allVersions.forEach { (key, shortName) ->
                            val lang = BibleVersionUtils.getLanguageForVersion(key)
                            groups.getOrPut(lang) { mutableListOf() }.add(key to shortName)
                        }
                        groups.forEach { (_, list) -> list.sortBy { it.second } }
                        val order = listOf("English", "English Messianic") +
                                groups.keys.filter { it != "English" && it != "English Messianic" }.sorted()
                        groups.toSortedMap(compareBy { order.indexOf(it).takeIf { idx -> idx >= 0 } ?: Int.MAX_VALUE })
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        grouped.forEach { (language, versions) ->
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor)
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        language,
                                        fontWeight = FontWeight.Bold,
                                        color = primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            items(versions) { (key, shortName) ->
                                val enabled = key !in viewModel.disabledVersions
                                val isActiveVersion = key in activeVersionKeys
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            enabled = !isActiveVersion,
                                            onClick = {
                                                viewModel.disabledVersions = if (enabled) {
                                                    viewModel.disabledVersions + key
                                                } else {
                                                    viewModel.disabledVersions - key
                                                }
                                            }
                                        )
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            shortName,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isActiveVersion) mutedColor else textColor,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            BibleVersionUtils.descriptionMap[key] ?: "",
                                            color = mutedColor,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isActiveVersion) {
                                            Text(
                                                "Currently active",
                                                color = primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = if (isActiveVersion) true else enabled,
                                        onCheckedChange = { checked ->
                                            if (!isActiveVersion) {
                                                viewModel.disabledVersions = if (checked) {
                                                    viewModel.disabledVersions - key
                                                } else {
                                                    viewModel.disabledVersions + key
                                                }
                                            }
                                        },
                                        enabled = !isActiveVersion,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = primary,
                                            checkedTrackColor = primary.copy(alpha = 0.5f),
                                            disabledCheckedThumbColor = primary.copy(alpha = 0.3f),
                                            disabledCheckedTrackColor = primary.copy(alpha = 0.1f),
                                            disabledUncheckedThumbColor = mutedColor.copy(alpha = 0.3f),
                                            disabledUncheckedTrackColor = mutedColor.copy(alpha = 0.1f)
                                        )
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }
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
                    "Version: 1.2.1",
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