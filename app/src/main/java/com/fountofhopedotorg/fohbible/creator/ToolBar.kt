package com.fountofhopedotorg.fohbible.creator

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun CombinedToolbarSection(
    onAddShape: (shape: String) -> Unit,
    onCustomPolygon: () -> Unit,
    selectedInputMode: String,
    onModeSelected: (String) -> Unit,
    themeColors: ThemeColors,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onChooseFromGallery: () -> Unit,
    graphicsLayer: GraphicsLayer,
    isLandscape: Boolean = false
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
    val toolbarContent: @Composable () -> Unit = {
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Square") }
        ) {
            SquareShape(modifier = Modifier.size(30.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Circle") }
        ) {
            CircleShape(modifier = Modifier.size(30.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Triangle") }
        ) {
            TriangleShape(modifier = Modifier.size(30.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Pentagon") }
        ) {
            PolygonShape(
                points = pentagonPoints,
                modifier = Modifier.size(30.dp)
            )
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = { onAddShape("Line") }
        ) {
            LineShape(modifier = Modifier.size(18.dp).padding(top = 6.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier.size(40.dp),
            onClick = onCustomPolygon
        ) {
            Icon(
                imageVector = Icons.Default.ShapeLine,
                contentDescription = "Custom Polygon",
                modifier = Modifier.size(30.dp),
                tint = getRandomColor().copy(0.8f)
            )
        }
        Box {
            ShapeSelectionCard(
                modifier = Modifier.size(40.dp),
                onClick = { showMoreShapes = true }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More Shapes",
                    modifier = Modifier.size(40.dp),
                    tint = getRandomColor()
                )
            }

            // --- UPDATED DROPDOWN MENU GRID LOGIC ---
            DropdownMenu(
                expanded = showMoreShapes,
                onDismissRequest = { showMoreShapes = false },
                // Doubles width to 80.dp in landscape to fit two columns
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

                if (isLandscape) {
                    // Group elements into sub-lists of 2 for the row structure
                    val chunkedShapes = newShapes.chunked(2)
                    chunkedShapes.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { (name, preview) ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            showMoreShapes = false
                                            onAddShape(name)
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
                    // Default Single Column Portrait Layout
                    newShapes.forEach { (name, preview) ->
                        DropdownMenuItem(
                            text = {},
                            onClick = {
                                showMoreShapes = false
                                onAddShape(name)
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

        modes.forEach { (mode, icon, color) ->
            val isSelected = selectedInputMode == mode
            IconButton(
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        IconButton(onClick = onToggleFullScreen) {
            Icon(
                modifier = Modifier.size(26.dp),
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Enter Fullscreen",
                tint = fullscreenIconColor
            )
        }

        Box {
            IconButton(
                onClick = { viewModel.showSaveMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save As",
                    tint = saveIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            SaveAsMenu(
                expanded = viewModel.showSaveMenu,
                onDismiss = { viewModel.showSaveMenu = false },
                onSavePng = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "PNG") }
                },
                onSaveJpg = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "JPG") }
                },
                onSavePdf = {
                    coroutineScope.launch {
                        saveCanvasAsPDF(
                            graphicsLayer,
                            context
                        )
                    }
                },
                onSaveSvg = {
                    coroutineScope.launch {
                        saveCanvasAsSVG(graphicsLayer, context, viewModel.canvasNotes)
                    }
                }
            )
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            toolbarContent()
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            toolbarContent()
        }
    }
}