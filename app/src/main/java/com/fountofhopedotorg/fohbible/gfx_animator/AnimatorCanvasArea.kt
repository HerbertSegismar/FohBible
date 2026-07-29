package com.fountofhopedotorg.fohbible.gfx_animator

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AnimatorCanvasArea(
    modifier: Modifier = Modifier,
    elements: List<CanvasElement>,
    selectedElementIds: Set<String>,
    selectedElementId: String?,
    selectedGroups: Set<String>,
    dragGroupDelta: Offset,
    onGroupDragDeltaChange: (Offset) -> Unit,
    onCanvasElementTap: (CanvasElement) -> Unit,
    onElementUpdatePosition: (CanvasElement, Offset, Float, Float, Float) -> Unit,
    onElementScaleChange: (String, Float, Float) -> Unit,
    onColorPickerRequested: (String) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onClearSelection: () -> Unit,
    themeColors: ThemeColors,
    isDark: Boolean,
    elementsGrouped: Map<String?, List<CanvasElement>>,
    graphicsLayer: GraphicsLayer,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit,
    currentTimeMs: Long,
    isPivotPlacementActive: Boolean = false,
    pivotTargetId: String? = null,
    onStartPivotPlacement: (String) -> Unit = {},
    onPlacePivotLocal: (Float, Float) -> Unit = { _, _ -> },
    canvasBackgroundColor: Color? = null,
    canvasBackgroundBrush: Brush? = null,
) {
    val viewModel: AppViewModel = viewModel()

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(isPivotPlacementActive, pivotTargetId) {
                detectTapGestures { tapOffset ->
                    if (isPivotPlacementActive && pivotTargetId != null) {
                        val target = elements.firstOrNull { it.id == pivotTargetId }
                        if (target != null) {
                            val px = tapOffset.x
                            val py = tapOffset.y
                            val w = target.width
                            val h = target.height
                            val dx = px - target.offset.x - target.pivotX * w
                            val dy = py - target.offset.y - target.pivotY * h
                            val rad = target.rotation * (PI / 180.0).toFloat()
                            val cosA = cos(rad)
                            val sinA = sin(rad)
                            val u = dx * cosA + dy * sinA
                            val v = -dx * sinA + dy * cosA
                            val localX = if (target.scaleX != 0f) u / target.scaleX + target.pivotX * w else 0f
                            val localY = if (target.scaleY != 0f) v / target.scaleY + target.pivotY * h else 0f
                            val normX = localX / w
                            val normY = localY / h

                            onPlacePivotLocal(normX, normY)
                        }
                    } else {
                        onClearSelection()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (canvasBackgroundBrush != null) {
                        drawRect(brush = canvasBackgroundBrush)
                    } else if (canvasBackgroundColor != null) {
                        if (canvasBackgroundColor != Color.Transparent) {
                            drawRect(color = canvasBackgroundColor)
                        }
                    } else {
                        val defaultBackgroundColor = if (isDark) Color(0xFF1E2937) else Color.White
                        drawRect(color = defaultBackgroundColor)
                    }

                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawContent()
                }
        ) {
            elements.forEach { element ->
                if (!element.isVisible) return@forEach
                if (currentTimeMs !in element.startTimeMs..element.endTimeMs) return@forEach

                key(element.id) {
                    val isInSelectedGroup = element.groupId in selectedGroups
                    val isItemSelected = selectedElementIds.contains(element.id) ||
                            (selectedElementId == element.id && selectedElementIds.isEmpty())

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
                            element.content.startsWith("Shape:") -> CanvasSvgItem(
                                element = element,
                                gradientConfig = viewModel.animatorGradientPairs[element.id],
                                isSelected = isItemSelected,
                                isLocked = element.isLocked,
                                onSelect = { if (!element.isLocked) onCanvasElementTap(element) },
                                onUpdatePosition = { offset, w, h, rot ->
                                    if (isInSelectedGroup) {
                                        onGroupDragDeltaChange(offset - element.offset)
                                    } else {
                                        onElementUpdatePosition(element, offset, w, h, rot)
                                    }
                                },
                                onColorPickerRequested = {
                                    if (!element.isLocked) onColorPickerRequested(element.id)
                                },
                                onDeleteRequested = { onDeleteRequested(element.id) },
                                onScaleChanged = { sx, sy -> onElementScaleChange(element.id, sx, sy) },
                                proportionalEditing = proportionalEditing,
                                onProportionalToggle = onProportionalToggle,
                                onStartPivotPlacement = { onStartPivotPlacement(element.id) },
                                isPivotPlacementActive = isPivotPlacementActive,
                                isActivePivotTarget = isPivotPlacementActive && pivotTargetId == element.id,
                                onPlacePivotLocal = onPlacePivotLocal
                            )
                            element.content.startsWith("Image:") -> CanvasImageItem(
                                element = element,
                                gradientConfig = viewModel.animatorGradientPairs[element.id],
                                isSelected = isItemSelected,
                                isLocked = element.isLocked,
                                onSelect = { if (!element.isLocked) onCanvasElementTap(element) },
                                onUpdatePosition = { offset, w, h, rot ->
                                    if (isInSelectedGroup) {
                                        onGroupDragDeltaChange(offset - element.offset)
                                    } else {
                                        onElementUpdatePosition(element, offset, w, h, rot)
                                    }
                                },
                                onColorPickerRequested = { onColorPickerRequested(element.id) },
                                onDeleteRequested = { onDeleteRequested(element.id) },
                                onScaleChanged = { sx, sy -> onElementScaleChange(element.id, sx, sy) },
                                proportionalEditing = proportionalEditing,
                                onProportionalToggle = onProportionalToggle,
                                onStartPivotPlacement = { onStartPivotPlacement(element.id) },
                                isPivotPlacementActive = isPivotPlacementActive,
                                isActivePivotTarget = isPivotPlacementActive && pivotTargetId == element.id,
                                onPlacePivotLocal = onPlacePivotLocal
                            )
                            else -> CanvasTextItem(
                                element = element,
                                gradientConfig = viewModel.animatorGradientPairs[element.id],
                                isSelected = isItemSelected,
                                isLocked = element.isLocked,
                                onSelect = { if (!element.isLocked) onCanvasElementTap(element) },
                                onUpdatePosition = { offset, w, h, rot ->
                                    if (isInSelectedGroup) {
                                        onGroupDragDeltaChange(offset - element.offset)
                                    } else {
                                        onElementUpdatePosition(element, offset, w, h, rot)
                                    }
                                },
                                onColorPickerRequested = {
                                    if (!element.isLocked) onColorPickerRequested(element.id)
                                },
                                onDeleteRequested = { onDeleteRequested(element.id) },
                                onScaleChanged = { sx, sy -> onElementScaleChange(element.id, sx, sy) },
                                proportionalEditing = proportionalEditing,
                                onProportionalToggle = onProportionalToggle,
                                onStartPivotPlacement = { onStartPivotPlacement(element.id) },
                                isPivotPlacementActive = isPivotPlacementActive,
                                isActivePivotTarget = isPivotPlacementActive && pivotTargetId == element.id,
                                onPlacePivotLocal = onPlacePivotLocal
                            )
                        }
                    }
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                selectedGroups.forEach { groupId ->
                    val groupElements = elementsGrouped[groupId] ?: return@forEach
                    val bbox = getGroupBoundingBox(groupElements) ?: return@forEach
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