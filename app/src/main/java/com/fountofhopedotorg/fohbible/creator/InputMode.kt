package com.fountofhopedotorg.fohbible.creator

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.functions.getRandomColor
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsImage
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsPDF
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsSVG
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun InputModeSelector(
    selectedInputMode: String,
    onModeSelected: (String) -> Unit,
    themeColors: ThemeColors,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onChooseFromGallery: () -> Unit,
    graphicsLayer: GraphicsLayer
) {
    val viewModel: AppViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val textIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val bookIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val imageIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val fullscreenIconColor = remember { getRandomColor().copy(alpha = 0.8f) }
    val saveIconColor = remember { getRandomColor().copy(alpha = 0.8f) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modes = listOf(
                Triple("Add Text", Icons.Default.TextFields, textIconColor),
                Triple("Fetch Verse", Icons.Default.Book, bookIconColor),
                Triple("Add Image", Icons.Default.Image, imageIconColor)
            )

            modes.forEach { (mode, icon, color) ->
                val isSelected = selectedInputMode == mode
                IconButton(
                    onClick = {
                        if (mode == "Add Image") {
                            onChooseFromGallery()
                        }
                        onModeSelected(mode)
                    },
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode,
                        tint = if (isSelected) themeColors.primary else color
                    )
                }
            }
            IconButton(onClick = onToggleFullScreen) {
                Icon(
                    modifier = Modifier.size(40.dp),
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullScreen) "Exit Fullscreen" else "Enter Fullscreen",
                    tint = fullscreenIconColor
                )
            }
            Box {
                IconButton(
                    onClick = { viewModel.showSaveMenu = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save As",
                        tint = saveIconColor
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
                    onSavePdf = { coroutineScope.launch { saveCanvasAsPDF(graphicsLayer, context) } },
                    onSaveSvg = {
                        coroutineScope.launch {
                            saveCanvasAsSVG(graphicsLayer, context, viewModel.canvasNotes)
                        }
                    }
                )
            }
        }
    }
}