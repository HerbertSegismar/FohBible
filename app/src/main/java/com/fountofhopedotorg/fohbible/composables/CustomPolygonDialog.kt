package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CustomPolygonDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Offset>) -> Unit
) {
    val points = remember { mutableStateListOf<Offset>() }
    // Add the redo stack here
    val redoStack = remember { mutableStateListOf<Offset>() }

    var drawingAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val nudgeAmounts = listOf(0.01f, 0.05f, 0.1f)
    var nudgeAmountIndex by remember { mutableIntStateOf(0) }
    val currentNudgeAmount = nudgeAmounts[nudgeAmountIndex]

    val nudgePoint = { dx: Float, dy: Float ->
        if (selectedIndex in points.indices) {
            val p = points[selectedIndex]
            points[selectedIndex] = Offset(
                x = (p.x + dx).coerceIn(0f, 1f),
                y = (p.y + dy).coerceIn(0f, 1f)
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tap to add Polygon points") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                        .onSizeChanged { drawingAreaSize = it }
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                if (drawingAreaSize.width > 0 && drawingAreaSize.height > 0) {
                                    val normX = tapOffset.x / drawingAreaSize.width
                                    val normY = tapOffset.y / drawingAreaSize.height
                                    val clamped = Offset(
                                        normX.coerceIn(0f, 1f),
                                        normY.coerceIn(0f, 1f)
                                    )
                                    points.add(clamped)
                                    selectedIndex = points.lastIndex

                                    // Clear redo stack on new user action
                                    redoStack.clear()
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasSize = this.size
                        if (points.size >= 3) {
                            val path = Path().apply {
                                moveTo(
                                    points[0].x * canvasSize.width,
                                    points[0].y * canvasSize.height
                                )
                                for (i in 1 until points.size) {
                                    lineTo(
                                        points[i].x * canvasSize.width,
                                        points[i].y * canvasSize.height
                                    )
                                }
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color(0x4000BCD4),
                                style = Fill
                            )
                            drawPath(
                                path = path,
                                color = Color(0xFF00BCD4),
                                style = Stroke(width = 3f)
                            )
                        }

                        if (points.size >= 2) {
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(
                                        points[i].x * canvasSize.width,
                                        points[i].y * canvasSize.height
                                    ),
                                    end = Offset(
                                        points[i + 1].x * canvasSize.width,
                                        points[i + 1].y * canvasSize.height
                                    ),
                                    strokeWidth = 2f
                                )
                            }
                        }

                        points.forEachIndexed { index, point ->
                            val isSelected = index == selectedIndex
                            drawCircle(
                                color = if (isSelected) Color.Blue else Color.Red,
                                radius = if (isSelected) 12f else 8f,
                                center = Offset(
                                    point.x * canvasSize.width,
                                    point.y * canvasSize.height
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ADD POINT
                    IconButton(
                        onClick = {
                            val newPoint = if (selectedIndex in points.indices) {
                                val p = points[selectedIndex]
                                Offset((p.x + 0.05f).coerceIn(0f, 1f), (p.y + 0.05f).coerceIn(0f, 1f))
                            } else {
                                Offset(0.5f, 0.5f)
                            }

                            val insertIndex = if (selectedIndex in points.indices) selectedIndex + 1 else points.size
                            points.add(insertIndex, newPoint)
                            selectedIndex = insertIndex
                            redoStack.clear() // Clear redo stack
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Point", tint = MaterialTheme.colorScheme.secondary)
                    }

                    // DELETE POINT
                    IconButton(
                        onClick = {
                            if (points.isNotEmpty() && selectedIndex in points.indices) {
                                points.removeAt(selectedIndex)
                                if (points.isEmpty()) {
                                    selectedIndex = -1
                                } else if (selectedIndex >= points.size) {
                                    selectedIndex = points.lastIndex
                                }
                                redoStack.clear() // Clear redo stack
                            }
                        },
                        enabled = points.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Point", tint = MaterialTheme.colorScheme.secondary)
                    }

                    // CYCLE LEFT
                    IconButton(
                        onClick = {
                            if (points.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1 + points.size) % points.size
                            }
                        },
                        enabled = points.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cycle Selected Point", tint = MaterialTheme.colorScheme.secondary)
                    }

                    // CYCLE RIGHT
                    IconButton(
                        onClick = {
                            if (points.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1) % points.size
                            }
                        },
                        enabled = points.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Cycle Selected Point", tint = MaterialTheme.colorScheme.secondary)
                    }

                    // UNDO
                    IconButton(
                        onClick = {
                            if (points.isNotEmpty()) {
                                val removedPoint = points.removeAt(points.lastIndex)
                                redoStack.add(removedPoint) // Save to redo stack

                                if (selectedIndex >= points.size) {
                                    selectedIndex = points.lastIndex
                                }
                            }
                        },
                        enabled = points.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.secondary)
                    }

                    // REDO
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val restoredPoint = redoStack.removeAt(redoStack.lastIndex)
                                points.add(restoredPoint) // Restore from redo stack
                                selectedIndex = points.lastIndex
                            }
                        },
                        enabled = redoStack.isNotEmpty() // Enable only if there's something to redo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                if (points.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HybridJoystick(
                            nudgeAmount = currentNudgeAmount,
                            onNudgeAmountClick = {
                                nudgeAmountIndex = (nudgeAmountIndex + 1) % nudgeAmounts.size
                            },
                            onDirectionClick = { dx, dy ->
                                nudgePoint(dx, dy)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (points.size >= 3) {
                        onConfirm(points.toList())
                    }
                },
                enabled = points.size >= 3
            ) {
                Text("Add to Canvas")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
                TextButton(
                    onClick = {
                        points.clear()
                        redoStack.clear()
                        selectedIndex = -1
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Text("Clear")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    )
}

@Composable
fun HybridJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    thumbSize: Dp = 60.dp,
    nudgeAmount: Float,
    onNudgeAmountClick: () -> Unit,
    onDirectionClick: (dx: Float, dy: Float) -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val thumbSizePx = with(density) { thumbSize.toPx() }

    val maxDisplacementPx = (sizePx - thumbSizePx) / 2f
    val allowedDisplacementPx = maxDisplacementPx * 1.3f

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val currentOffset by rememberUpdatedState(thumbOffset)
    val currentNudgeAmount by rememberUpdatedState(nudgeAmount)
    val currentOnDirectionClick by rememberUpdatedState(onDirectionClick)

    LaunchedEffect(isDragging) {
        while (isDragging) {
            if (currentOffset != Offset.Zero) {
                val normalizedX = currentOffset.x / maxDisplacementPx
                val normalizedY = currentOffset.y / maxDisplacementPx
                val frameSpeedFactor = 0.05f

                currentOnDirectionClick(
                    normalizedX * currentNudgeAmount * frameSpeedFactor,
                    normalizedY * currentNudgeAmount * frameSpeedFactor
                )
            }
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val currentDisplacement = newOffset.getDistance()
                        thumbOffset = if (currentDisplacement <= allowedDisplacementPx) {
                            newOffset
                        } else {
                            newOffset.times(allowedDisplacementPx / currentDisplacement)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        thumbOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        thumbOffset = Offset.Zero
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { onDirectionClick(0f, -nudgeAmount) },
            modifier = Modifier.align(Alignment.TopCenter).size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = MaterialTheme.colorScheme.primary.copy(0.5f))
        }
        IconButton(
            onClick = { onDirectionClick(0f, nudgeAmount) },
            modifier = Modifier.align(Alignment.BottomCenter).size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = MaterialTheme.colorScheme.primary.copy(0.5f))
        }
        IconButton(
            onClick = { onDirectionClick(-nudgeAmount, 0f) },
            modifier = Modifier.align(Alignment.CenterStart).size(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left", tint = MaterialTheme.colorScheme.primary.copy(0.5f))
        }
        IconButton(
            onClick = { onDirectionClick(nudgeAmount, 0f) },
            modifier = Modifier.align(Alignment.CenterEnd).size(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right", tint = MaterialTheme.colorScheme.primary.copy(0.5f))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(thumbSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { onNudgeAmountClick() },
            contentAlignment = Alignment.Center
        ) {
            val amountText = when(nudgeAmount) {
                0.01f -> "1%"
                0.05f -> "5%"
                0.1f -> "10%"
                else -> "${(nudgeAmount * 100).toInt()}%"
            }
            Text(
                text = amountText,
                color = Color.White,
                style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
        }
    }
}