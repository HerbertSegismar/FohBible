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
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.ThemeColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CanvasArea(
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
    // Pivot placement – global control
    isPivotPlacementActive: Boolean = false,
    pivotTargetId: String? = null,
    onStartPivotPlacement: (String) -> Unit = {},
    onPlacePivotLocal: (Float, Float) -> Unit = { _, _ -> },
    // Gradient support
    gradientConfigs: Map<String, GradientConfig> = emptyMap()
) {
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

                            // 1. Shift to the element’s transform origin (current pivot)
                            val dx = px - target.offset.x - target.pivotX * w
                            val dy = py - target.offset.y - target.pivotY * h

                            // 2. Inverse rotation
                            val rad = target.rotation * (PI / 180.0).toFloat()
                            val cosA = cos(rad)
                            val sinA = sin(rad)
                            val u = dx * cosA + dy * sinA
                            val v = -dx * sinA + dy * cosA

                            // 3. Inverse scale and add back the current pivot
                            val localX = if (target.scaleX != 0f) u / target.scaleX + target.pivotX * w else 0f
                            val localY = if (target.scaleY != 0f) v / target.scaleY + target.pivotY * h else 0f

                            // 4. Normalize without clamping – allows pivots outside the element
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
                    val backgroundColor = if (isDark) Color(0xFF1E2937) else themeColors.primary.copy(0.1f)
                    drawRect(color = backgroundColor)
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawContent()
                }
        ) {
            elements.forEach { element ->
                if (!element.isVisible) return@forEach

                key(element.id) {
                    val isInSelectedGroup = element.groupId in selectedGroups
                    val isItemSelected = selectedElementIds.contains(element.id) ||
                            (selectedElementId == element.id && selectedElementIds.isEmpty())

                    val gradientConfig = gradientConfigs[element.id]  // Retrieve gradient for this element

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
                                onPlacePivotLocal = onPlacePivotLocal,
                                gradientConfig = gradientConfig
                            )
                            element.content.startsWith("Image:") -> CanvasImageItem(
                                element = element,
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
                                onPlacePivotLocal = onPlacePivotLocal,
                                gradientConfig = gradientConfig
                            )
                            else -> CanvasTextItem(
                                element = element,
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
                                onPlacePivotLocal = onPlacePivotLocal,
                                gradientConfig = gradientConfig
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