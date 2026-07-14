package com.fountofhopedotorg.fohbible.gfx_animator

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.gfx_creator.ArrowRightShape
import com.fountofhopedotorg.fohbible.gfx_creator.CircleShape
import com.fountofhopedotorg.fohbible.gfx_creator.CrossShape
import com.fountofhopedotorg.fohbible.gfx_creator.DavidStarShape
import com.fountofhopedotorg.fohbible.gfx_creator.DiamondShape
import com.fountofhopedotorg.fohbible.gfx_creator.GearShape
import com.fountofhopedotorg.fohbible.gfx_creator.HeartShape
import com.fountofhopedotorg.fohbible.gfx_creator.HexagonShape
import com.fountofhopedotorg.fohbible.gfx_creator.LineShape
import com.fountofhopedotorg.fohbible.gfx_creator.MoonShape
import com.fountofhopedotorg.fohbible.gfx_creator.OctagonShape
import com.fountofhopedotorg.fohbible.gfx_creator.PolygonShape
import com.fountofhopedotorg.fohbible.gfx_creator.ShapeSelectionCard
import com.fountofhopedotorg.fohbible.gfx_creator.SquareShape
import com.fountofhopedotorg.fohbible.gfx_creator.StarShape
import com.fountofhopedotorg.fohbible.gfx_creator.ThornCrownShape
import com.fountofhopedotorg.fohbible.gfx_creator.TriangleShape
import com.fountofhopedotorg.fohbible.gfx_creator.getRandomColor
import com.fountofhopedotorg.fohbible.gfx_creator.saveCanvasAsImage
import com.fountofhopedotorg.fohbible.gfx_creator.saveCanvasAsPDF
import com.fountofhopedotorg.fohbible.gfx_creator.saveCanvasAsSVG
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnimatorToolbar(
    onAddShape: (shape: String) -> Unit,
    onCustomPolygon: () -> Unit,
    selectedInputMode: String,
    onModeSelected: (String) -> Unit,
    themeColors: ThemeColors,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onChooseFromGallery: () -> Unit,
    graphicsLayer: GraphicsLayer,
    isLandscape: Boolean = false,
    onSaveVideo: () -> Unit = {},
    isPlayingAnimation: Boolean = false,
    onPlayPause: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    enablePlayStop: Boolean = false,
    onCanvasSettingsClick: () -> Unit = {}
) {
    var showMoreShapes by remember { mutableStateOf(false) }
    val viewModel: AppViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val textIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val bookIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val imageIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val fullscreenIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val saveIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val canvasSizeIconColor = remember { getRandomColor().copy(alpha = 0.8f) }

    val modes = listOf(
        Triple("Add Text", Icons.Default.TextFields, textIconColor),
        Triple("Fetch Verse", Icons.Default.Book, bookIconColor),
        Triple("Add Image", Icons.Default.Image, imageIconColor)
    )

    val pentagonPoints = listOf(
        Offset(0.5000f, 0.0000f),
        Offset(0.9755f, 0.3455f),
        Offset(0.7939f, 0.9045f),
        Offset(0.2061f, 0.9045f),
        Offset(0.0245f, 0.3455f)
    )

    val itemButtonSize = Modifier.size(40.dp)
    val standardIconSize = Modifier.size(20.dp)

    val firstItems: @Composable () -> Unit = {
        ShapeSelectionCard(
            modifier = itemButtonSize,
            onClick = { onAddShape("Square") }
        ) {
            SquareShape(modifier = Modifier.size(16.dp))
        }
        ShapeSelectionCard(
            modifier = itemButtonSize,
            onClick = { onAddShape("Circle") }
        ) {
            CircleShape(modifier = Modifier.size(18.dp))
        }
        ShapeSelectionCard(
            modifier = itemButtonSize,
            onClick = { onAddShape("Triangle") }
        ) {
            TriangleShape(modifier = Modifier.size(18.dp))
        }
        ShapeSelectionCard(
            modifier = itemButtonSize,
            onClick = { onAddShape("Pentagon") }
        ) {
            PolygonShape(
                points = pentagonPoints,
                modifier = Modifier.size(18.dp)
            )
        }
        ShapeSelectionCard(
            modifier = itemButtonSize,
            onClick = { onAddShape("Line") }
        ) {
            LineShape(modifier = Modifier.size(14.dp))
        }
        ShapeSelectionCard(
            modifier = itemButtonSize,
            onClick = onCustomPolygon
        ) {
            Icon(
                imageVector = Icons.Default.ShapeLine,
                contentDescription = "Custom Polygon",
                modifier = Modifier.size(16.dp),
                tint = getRandomColor().copy(0.8f)
            )
        }
        Box {
            ShapeSelectionCard(
                modifier = itemButtonSize,
                onClick = { showMoreShapes = true }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More Shapes",
                    modifier = standardIconSize,
                    tint = getRandomColor()
                )
            }

            DropdownMenu(
                expanded = showMoreShapes,
                onDismissRequest = {},
                properties = PopupProperties(
                    focusable = false,
                    dismissOnClickOutside = false,
                    dismissOnBackPress = false
                ),
                modifier = Modifier
                    .width(if (isLandscape) 80.dp else 40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(0.05f))
            ) {
                val newShapes = listOf(
                    "Hexagon" to @Composable { HexagonShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Star" to @Composable { StarShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Diamond" to @Composable { DiamondShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Heart" to @Composable { HeartShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Octagon" to @Composable { OctagonShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Cross" to @Composable { CrossShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "ThornCrown" to @Composable { ThornCrownShape(modifier = Modifier.fillMaxSize(), thornColor = getRandomColor().copy(0.8f)) },
                    "Moon" to @Composable { MoonShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "DavidStar" to @Composable { DavidStarShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "Gear" to @Composable { GearShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) },
                    "ArrowRight" to @Composable { ArrowRightShape(modifier = Modifier.fillMaxSize(), color = getRandomColor().copy(0.8f)) }
                )

                val allMenuItems = newShapes + ("Close" to @Composable {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.error
                    )
                })

                if (isLandscape) {
                    val chunkedShapes = allMenuItems.chunked(2)
                    chunkedShapes.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { (name, preview) ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            if (name == "Close") {
                                                showMoreShapes = false
                                            } else {
                                                onAddShape(name)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.size(18.dp)) {
                                        preview()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    allMenuItems.forEach { (name, preview) ->
                        DropdownMenuItem(
                            text = {},
                            onClick = {
                                if (name == "Close") {
                                    showMoreShapes = false
                                } else {
                                    onAddShape(name)
                                }
                            },
                            leadingIcon = {
                                Box(Modifier.size(18.dp)) {
                                    preview()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val secondItems: @Composable () -> Unit = {
        modes.forEach { (mode, icon, color) ->
            val isSelected = selectedInputMode == mode
            IconButton(
                modifier = itemButtonSize,
                onClick = {
                    if (mode == "Add Image") {
                        onChooseFromGallery()
                    }
                    onModeSelected(mode)
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode,
                    tint = if (isSelected) themeColors.primary else color,
                    modifier = standardIconSize
                )
            }
        }

        Box {
            IconButton(
                modifier = itemButtonSize,
                onClick = { viewModel.showSaveMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save As",
                    tint = saveIconColor,
                    modifier = standardIconSize
                )
            }
            SaveAsMenuWithVideo(
                expanded = viewModel.showSaveMenu,
                onDismiss = { viewModel.showSaveMenu = false },
                onSavePng = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "PNG") }
                },
                onSaveJpg = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "JPG") }
                },
                onSavePdf = {
                    coroutineScope.launch { saveCanvasAsPDF(graphicsLayer, context) }
                },
                onSaveSvg = {
                    coroutineScope.launch {
                        saveCanvasAsSVG(graphicsLayer, context, viewModel.animatorCanvasElements)
                    }
                },
                onSaveVideo = onSaveVideo
            )
        }

        IconButton(
            modifier = itemButtonSize,
            onClick = onToggleFullScreen
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Enter Fullscreen",
                tint = fullscreenIconColor
            )
        }

        IconButton(
            modifier = itemButtonSize,
            onClick = onCanvasSettingsClick
        ) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = "Canvas Settings",
                modifier = Modifier.size(18.dp),
                tint = canvasSizeIconColor
            )
        }

        val selectedElementId = viewModel.animatorSelectedElementId

        IconButton(
            modifier = itemButtonSize,
            onClick = onTimelineClick,
            enabled = selectedElementId != null && !isPlayingAnimation
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "Keyframe Animation",
                modifier = standardIconSize,
                tint = if (selectedElementId != null && !isPlayingAnimation)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        IconButton(
            modifier = itemButtonSize,
            onClick = onPlayPause,
            enabled = enablePlayStop
        ) {
            Icon(
                imageVector = if (isPlayingAnimation) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlayingAnimation) "Stop Animation" else "Play All Animations",
                modifier = standardIconSize,
                tint = if (isPlayingAnimation) Color.Red
                else if (enablePlayStop) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            firstItems()
            secondItems()
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            firstItems()
            secondItems()
        }
    }
}