package com.fountofhopedotorg.fohbible.gfx_creator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.ThemeColors
import kotlin.math.roundToInt

@Composable
fun CanvasArea(
    modifier: Modifier = Modifier,
    notes: List<CanvasNote>,
    selectedNoteIds: Set<String>,
    selectedNoteId: String?,
    selectedGroups: Set<String>,
    dragGroupDelta: Offset,
    onGroupDragDeltaChange: (Offset) -> Unit,
    onCanvasNoteTap: (CanvasNote) -> Unit,
    onNoteUpdatePosition: (CanvasNote, Offset, Float, Float, Float) -> Unit,
    onNoteScaleChange: (String, Float, Float) -> Unit,
    onColorPickerRequested: (String) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onClearSelection: () -> Unit,
    themeColors: ThemeColors,
    isDark: Boolean,
    notesGrouped: Map<String?, List<CanvasNote>>,
    graphicsLayer: GraphicsLayer,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit,
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClearSelection() })
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val backgroundColor = if (isDark) Color(0xFF1E2937) else themeColors.primary.copy(0.1f)
                    drawRect(color = backgroundColor)
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawContent()
                }
        ) {
            notes.forEach { note ->
                if (!note.isVisible) return@forEach

                key(note.id) {
                    val isInSelectedGroup = note.groupId in selectedGroups
                    val isItemSelected = selectedNoteIds.contains(note.id) ||
                            (selectedNoteId == note.id && selectedNoteIds.isEmpty())
                    Box(
                        modifier = if (isInSelectedGroup) {
                            Modifier.offset {
                                IntOffset(dragGroupDelta.x.roundToInt(), dragGroupDelta.y.roundToInt())
                            }
                        } else {
                            Modifier
                        }
                    ) {
                        when {
                            note.content.startsWith("Shape:") -> CanvasSvgItem(
                                note = note,
                                isSelected = isItemSelected,
                                isLocked = note.isLocked,
                                onSelect = { if (!note.isLocked) onCanvasNoteTap(note) },
                                onUpdatePosition = { offset, w, h, rot ->
                                    if (isInSelectedGroup) {
                                        onGroupDragDeltaChange(offset - note.offset)
                                    } else {
                                        onNoteUpdatePosition(note, offset, w, h, rot)
                                    }
                                },
                                onColorPickerRequested = {
                                    if (!note.isLocked) onColorPickerRequested(note.id)
                                },
                                onDeleteRequested = { onDeleteRequested(note.id) },
                                onScaleChanged = { sx, sy -> onNoteScaleChange(note.id, sx, sy) },
                                proportionalEditing = proportionalEditing,
                                onProportionalToggle = onProportionalToggle
                            )
                            note.content.startsWith("Image:") -> CanvasImageItem(
                                note = note,
                                isSelected = isItemSelected,
                                isLocked = note.isLocked,
                                onSelect = { if (!note.isLocked) onCanvasNoteTap(note) },
                                onUpdatePosition = { offset, w, h, rot ->
                                    if (isInSelectedGroup) {
                                        onGroupDragDeltaChange(offset - note.offset)
                                    } else {
                                        onNoteUpdatePosition(note, offset, w, h, rot)
                                    }
                                },
                                onColorPickerRequested = { onColorPickerRequested(note.id) },
                                onDeleteRequested = { onDeleteRequested(note.id) },
                                onScaleChanged = { sx, sy -> onNoteScaleChange(note.id, sx, sy) },
                                proportionalEditing = proportionalEditing,
                                onProportionalToggle = onProportionalToggle
                            )
                            else -> CanvasTextItem(
                                note = note,
                                isSelected = isItemSelected,
                                isLocked = note.isLocked,
                                onSelect = { if (!note.isLocked) onCanvasNoteTap(note) },
                                onUpdatePosition = { offset, w, h, rot ->
                                    if (isInSelectedGroup) {
                                        onGroupDragDeltaChange(offset - note.offset)
                                    } else {
                                        onNoteUpdatePosition(note, offset, w, h, rot)
                                    }
                                },
                                onColorPickerRequested = {
                                    if (!note.isLocked) onColorPickerRequested(note.id)
                                },
                                onDeleteRequested = { onDeleteRequested(note.id) },
                                onScaleChanged = { sx, sy -> onNoteScaleChange(note.id, sx, sy) },
                                proportionalEditing = proportionalEditing,
                                onProportionalToggle = onProportionalToggle
                            )
                        }
                    }
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                selectedGroups.forEach { groupId ->
                    val groupNotes = notesGrouped[groupId] ?: return@forEach
                    val bbox = getGroupBoundingBox(groupNotes) ?: return@forEach
                    val left = bbox.minX + dragGroupDelta.x
                    val top = bbox.minY + dragGroupDelta.y
                    val right = bbox.maxX + dragGroupDelta.x
                    val bottom = bbox.maxY + dragGroupDelta.y
                    val rect = Rect(left, top, right, bottom)

                    drawRect(
                        color = themeColors.primary.copy(alpha = 0.8f),
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }
            }
        }
    }
}