package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun CustomPolygonDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Offset>) -> Unit
) {
    val points = remember { mutableStateListOf<Offset>() }
    var drawingAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
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
        title = { Text("Tap to add polygon points") },
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (points.isNotEmpty()) {
                                points.removeAt(points.lastIndex)
                                if (selectedIndex >= points.size) {
                                    selectedIndex = points.lastIndex
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Undo")
                    }
                    OutlinedButton(
                        onClick = {
                            points.clear()
                            selectedIndex = -1
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                }

                if (points.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (points.isNotEmpty()) {
                                    selectedIndex = (selectedIndex + 1) % points.size
                                }
                            }
                        ) {
                            Text("Cycle Point")
                        }

                        val nudgeStep = 0.01f
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { nudgePoint(-nudgeStep, 0f) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { nudgePoint(0f, -nudgeStep) }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                                }
                                IconButton(onClick = { nudgePoint(0f, nudgeStep) }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                                }
                            }
                            IconButton(onClick = { nudgePoint(nudgeStep, 0f) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
                            }
                        }
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}