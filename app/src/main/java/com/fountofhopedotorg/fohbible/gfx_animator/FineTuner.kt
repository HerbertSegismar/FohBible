package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FineTunerPanel(
    viewModel: AppViewModel,
    selectedElementId: String?,
    elements: List<CanvasElement>,
    themeColors: ThemeColors
) {
    val element = elements.find { it.id == selectedElementId }
    var proportionalScaling by remember { mutableStateOf(true) }

    val displayName: (CanvasElement) -> String = { elem ->
        getElementDisplayName(elem, elements.indexOf(elem), elements)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (elements.isEmpty()) {
            Text(
                "No elements on canvas",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(elements) { elem ->
                    val isSelected = elem.id == selectedElementId
                    val bgColor = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

                    val gradientConfig = viewModel.animatorGradientPairs[elem.id]

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                            .clickable {
                                viewModel.animatorSelectedElementIds = emptySet()
                                viewModel.animatorSelectedElementId = elem.id
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ElementThumbnail(
                                    element = elem,
                                    themeColors = themeColors,
                                    gradientConfig = gradientConfig
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = displayName(elem),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (element == null) {
            if (elements.isNotEmpty()) {
                Text(
                    "Select an element to fine‑tune",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            val updateProps = { x: Float?, y: Float?, rot: Float? ->
                viewModel.updateAnimatorElementProperties(
                    id = element.id,
                    x = x ?: element.offset.x,
                    y = y ?: element.offset.y,
                    width = element.width,
                    height = element.height,
                    rotation = rot ?: element.rotation
                )
            }

            val updateScale = { sx: Float?, sy: Float? ->
                if (proportionalScaling) {
                    if (sx != null) {
                        val ratio = if (element.scaleX != 0f) sx / element.scaleX else 1f
                        viewModel.updateAnimatorElementScale(element.id, sx, element.scaleY * ratio)
                    } else if (sy != null) {
                        val ratio = if (element.scaleY != 0f) sy / element.scaleY else 1f
                        viewModel.updateAnimatorElementScale(element.id, element.scaleX * ratio, sy)
                    }
                } else {
                    viewModel.updateAnimatorElementScale(
                        id = element.id,
                        scaleX = sx ?: element.scaleX,
                        scaleY = sy ?: element.scaleY
                    )
                }
            }

            val updatePivot = { px: Float?, py: Float? ->
                val newPivotX = px ?: element.pivotX
                val newPivotY = py ?: element.pivotY
                viewModel.updateAnimatorElementPivot(
                    id = element.id,
                    newPivotX = newPivotX,
                    newPivotY = newPivotY
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPropertyField(
                    label = "X", value = element.offset.x, format = ::formatPosition,
                    onValueChange = { updateProps(it, null, null) },
                    modifier = Modifier.weight(1f)
                )
                CompactPropertyField(
                    label = "Y", value = element.offset.y, format = ::formatPosition,
                    onValueChange = { updateProps(null, it, null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactPropertyField(
                    label = "Scale X", value = element.scaleX, step = 0.1f,
                    valueRange = 0.1f..25f, format = ::formatScale,
                    onValueChange = { updateScale(it, null) },
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .offset(y = 4.dp)
                        .clip(CircleShape)
                        .clickable { proportionalScaling = !proportionalScaling },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (proportionalScaling) Icons.Default.Link else Icons.Default.LinkOff,
                        contentDescription = "Toggle Proportional Scaling",
                        modifier = Modifier.size(18.dp),
                        tint = if (proportionalScaling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                CompactPropertyField(
                    label = "Scale Y", value = element.scaleY, step = 0.1f,
                    valueRange = 0.1f..25f, format = ::formatScale,
                    onValueChange = { updateScale(null, it) },
                    modifier = Modifier.weight(1f)
                )
            }

            CompactPropertyField(
                label = "Rotation°", value = element.rotation, format = ::formatPosition,
                onValueChange = { updateProps(null, null, it) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPropertyField(
                    label = "Pivot X", value = element.pivotX,
                    step = 0.05f,
                    format = ::formatPivot,
                    onValueChange = { updatePivot(it, null) },
                    modifier = Modifier.weight(1f)
                )
                CompactPropertyField(
                    label = "Pivot Y", value = element.pivotY,
                    step = 0.05f,
                    format = ::formatPivot,
                    onValueChange = { updatePivot(null, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactPropertyField(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    step: Float = 1f,
    valueRange: ClosedFloatingPointRange<Float> = -Float.MAX_VALUE..Float.MAX_VALUE,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(format(value)) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText
            newText.toFloatOrNull()?.let {
                onValueChange(it.coerceIn(valueRange))
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        modifier = modifier.height(56.dp),
        leadingIcon = {
            val minusInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .repeatingClickable(
                        interactionSource = minusInteractionSource,
                        onClick = { multiplier -> onValueChange((value - (step * multiplier)).coerceIn(valueRange)) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        trailingIcon = {
            val plusInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .repeatingClickable(
                        interactionSource = plusInteractionSource,
                        onClick = { multiplier -> onValueChange((value + (step * multiplier)).coerceIn(valueRange)) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.secondary,
            unfocusedTextColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

fun Modifier.repeatingClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    initialDelay: Long = 400L,
    repeatDelay: Long = 50L,
    timeToScale: Long = 300L,
    maxMultiplier: Int = 20,
    onClick: (multiplier: Int) -> Unit
): Modifier = composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val coroutineScope = rememberCoroutineScope()

    this
        .pointerInput(interactionSource, enabled) {
            detectTapGestures(
                onPress = { offset ->
                    if (!enabled) return@detectTapGestures

                    val press = PressInteraction.Press(offset)
                    coroutineScope.launch { interactionSource.emit(press) }

                    val job = coroutineScope.launch {
                        var currentMultiplier = 1

                        currentOnClick(currentMultiplier)
                        delay(initialDelay.milliseconds)

                        var timeHeld = 0L

                        while (isActive) {
                            currentOnClick(currentMultiplier)
                            delay(repeatDelay.milliseconds)

                            timeHeld += repeatDelay
                            currentMultiplier = (1 + (timeHeld / timeToScale).toInt()).coerceAtMost(maxMultiplier)
                        }
                    }

                    val success = tryAwaitRelease()

                    job.cancel()
                    val endInteraction = if (success) {
                        PressInteraction.Release(press)
                    } else {
                        PressInteraction.Cancel(press)
                    }
                    coroutineScope.launch { interactionSource.emit(endInteraction) }
                }
            )
        }
        .indication(interactionSource, LocalIndication.current)
}