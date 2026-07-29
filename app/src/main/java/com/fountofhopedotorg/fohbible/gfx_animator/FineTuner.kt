package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.models.AppViewModel

@Composable
fun FineTunerPanel(
    viewModel: AppViewModel,
    selectedElementId: String?,
    elements: List<CanvasElement>,
    themeColors: ThemeColors
) {
    val element = elements.find { it.id == selectedElementId }

    val displayName: (CanvasElement) -> String = { elem ->
        getElementDisplayName(elem, elements.indexOf(elem), elements)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                viewModel.updateAnimatorElementScale(
                    id = element.id,
                    scaleX = sx ?: element.scaleX,
                    scaleY = sy ?: element.scaleY
                )
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPropertyField(
                    label = "Scale X", value = element.scaleX, step = 0.1f,
                    valueRange = 0.1f..25f, format = ::formatScale,
                    onValueChange = { updateScale(it, null) },
                    modifier = Modifier.weight(1f)
                )
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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
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
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).height(52.dp),
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

        IconButton(
            onClick = { onValueChange((value - step).coerceIn(valueRange)) },
            modifier = Modifier.size(25.dp)
        ) {
            Text("-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        IconButton(
            onClick = { onValueChange((value + step).coerceIn(valueRange)) },
            modifier = Modifier.size(25.dp)
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}