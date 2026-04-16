package com.fountofhopedotorg.fohbible.dropdowns

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.composables.DropdownMenuItemWithIcon
import com.fountofhopedotorg.fohbible.models.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowsLayoutDropdown(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "windowsLayoutIconRotation"
    )
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = !expanded }
        ) {
            Crossfade(
                targetState = expanded,
                animationSpec = tween(300),
                label = "windowsLayoutIconCrossfade"
            ) { isOpen ->
                Icon(
                    imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.AutoAwesomeMosaic,
                    contentDescription = if (isOpen) "Close MultiView" else "MultiView",
                    tint = Color.White,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                if (viewModel.darkTheme) viewModel.darkModalBackgroundColor
                else viewModel.lightModalBackgroundColor
            ),
            offset = DpOffset(x = 100.dp, y = 0.dp),
        ) {
            Text(
                "Windows Layout",
                modifier = Modifier.fillMaxWidth().height(25.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()

            listOf("Single", "Horizontal", "Vertical").forEach { layout ->
                val isActive = when (layout.lowercase()) {
                    "single" -> !viewModel.multiVersion
                    "horizontal" -> viewModel.multiVersion && viewModel.multiViewLayout == "horizontal"
                    "vertical" -> viewModel.multiVersion && viewModel.multiViewLayout == "vertical"
                    else -> false
                }
                val icon = when (layout.lowercase()) {
                    "single" -> Icons.Default.LooksOne
                    "horizontal", "vertical" -> Icons.Default.ViewStream
                    else -> Icons.AutoMirrored.Filled.Label
                }
                val iconModifier =
                    if (layout.lowercase() == "horizontal") Modifier.rotate(90f) else Modifier

                DropdownMenuItemWithIcon(
                    title = layout,
                    icon = icon,
                    isActive = isActive,
                    onClick = {
                        when (layout.lowercase()) {
                            "single" -> viewModel.multiVersion = false
                            "horizontal" -> {
                                viewModel.multiVersion = true
                                viewModel.multiViewLayout = "horizontal"
                            }

                            "vertical" -> {
                                viewModel.multiVersion = true
                                viewModel.multiViewLayout = "vertical"
                            }
                        }
                        expanded = false
                    },
                    iconModifier = iconModifier
                )
            }
        }
    }
}