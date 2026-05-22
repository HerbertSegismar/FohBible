package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CanvasNoteItem(
    note: CanvasNote,
    onUpdatePosition: (androidx.compose.ui.geometry.Offset, Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit
) {
    var currentOffset by remember(note.id) { mutableStateOf(note.offset) }
    var currentWidth by remember(note.id) { mutableFloatStateOf(note.width) }
    var currentHeight by remember(note.id) { mutableFloatStateOf(note.height) }
    var currentRotation by remember(note.id) { mutableFloatStateOf(0f) }
    var startTouchAngle by remember(note.id) { mutableFloatStateOf(0f) }
    var startCardRotation by remember(note.id) { mutableFloatStateOf(0f) }

    var cardCoordinates: LayoutCoordinates? by remember(note.id) { mutableStateOf(null) }
    var resizeHandleCoords: LayoutCoordinates? by remember(note.id) { mutableStateOf(null) }
    var rotateHandleCoords: LayoutCoordinates? by remember(note.id) { mutableStateOf(null) }

    val density = LocalDensity.current

    val contentLines = remember(note.content) { note.content.lines() }
    val referenceLine = contentLines.firstOrNull() ?: "Note"
    val bodyText = remember(contentLines) {
        if (contentLines.size > 1) contentLines.drop(1).joinToString("\n") else ""
    }
    val contentColor = remember(note.backgroundColor) {
        if (note.backgroundColor.luminance() < 0.5f) Color.White else Color.Black
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt()) }
            .width(with(density) { currentWidth.toDp() })
            .height(with(density) { currentHeight.toDp() })
            .graphicsLayer { rotationZ = currentRotation }
            .pointerInput(note.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val rad = Math.toRadians(currentRotation.toDouble())
                    val cosR = cos(rad).toFloat()
                    val sinR = sin(rad).toFloat()
                    val correctedDrag = androidx.compose.ui.geometry.Offset(
                        x = dragAmount.x * cosR - dragAmount.y * sinR,
                        y = dragAmount.x * sinR + dragAmount.y * cosR
                    )
                    currentOffset += correctedDrag
                    onUpdatePosition(currentOffset, currentWidth, currentHeight)
                }
            }
            .onGloballyPositioned { cardCoordinates = it }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = note.backgroundColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(note.backgroundColor.copy(alpha = 0.95f))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = referenceLine,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = contentColor
                    )

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Brush.horizontalGradient(listOf(note.backgroundColor, note.backgroundColor.copy(alpha = 0.5f))), CircleShape)
                            .border(1.5.dp, color = contentColor, CircleShape)
                            .clickable { onColorPickerRequested() }
                    )
                    Spacer(Modifier.width(24.dp))

                    IconButton(onClick = onDeleteRequested) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 0.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = bodyText.ifBlank { note.content },
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .onGloballyPositioned { resizeHandleCoords = it }
                .pointerInput(note.id) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val cardCoords = cardCoordinates
                        val handleCoords = resizeHandleCoords

                        if (cardCoords != null && handleCoords != null) {
                            val touchInWindow = handleCoords.localToWindow(change.position)
                            val cardCenterInWindow = cardCoords.localToWindow(
                                androidx.compose.ui.geometry.Offset(cardCoords.size.width / 2f, cardCoords.size.height / 2f)
                            )

                            val deltaX = touchInWindow.x - cardCenterInWindow.x
                            val deltaY = touchInWindow.y - cardCenterInWindow.y
                            val rad = Math.toRadians(currentRotation.toDouble())
                            val cosR = cos(rad).toFloat()
                            val sinR = sin(rad).toFloat()

                            val localX = deltaX * cosR + deltaY * sinR
                            val localY = -deltaX * sinR + deltaY * cosR

                            currentWidth = (currentWidth / 2f + localX).coerceAtLeast(160f)
                            currentHeight = (currentHeight / 2f + localY).coerceAtLeast(120f)
                            onUpdatePosition(currentOffset, currentWidth, currentHeight)
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(32.dp)
                .onGloballyPositioned { rotateHandleCoords = it }
                .pointerInput(note.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cardCoords = cardCoordinates
                            val handleCoords = rotateHandleCoords
                            if (cardCoords != null && handleCoords != null) {
                                val touchInWindow = handleCoords.localToWindow(offset)
                                val cardCenterInWindow = cardCoords.localToWindow(
                                    androidx.compose.ui.geometry.Offset(cardCoords.size.width / 2f, cardCoords.size.height / 2f)
                                )
                                startTouchAngle = Math.toDegrees(atan2((touchInWindow.y - cardCenterInWindow.y).toDouble(), (touchInWindow.x - cardCenterInWindow.x).toDouble())).toFloat()
                                startCardRotation = currentRotation
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val cardCoords = cardCoordinates
                            val handleCoords = rotateHandleCoords
                            if (cardCoords != null && handleCoords != null) {
                                val touchInWindow = handleCoords.localToWindow(change.position)
                                val cardCenterInWindow = cardCoords.localToWindow(
                                    androidx.compose.ui.geometry.Offset(cardCoords.size.width / 2f, cardCoords.size.height / 2f)
                                )
                                val currentTouchAngle = Math.toDegrees(atan2((touchInWindow.y - cardCenterInWindow.y).toDouble(), (touchInWindow.x - cardCenterInWindow.x).toDouble())).toFloat()
                                currentRotation = (startCardRotation + (currentTouchAngle - startTouchAngle)) % 360f
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.tertiary, shape = CircleShape)
            )
        }
    }
}