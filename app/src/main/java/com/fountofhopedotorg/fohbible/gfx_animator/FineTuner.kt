package com.fountofhopedotorg.fohbible.gfx_animator

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.models.AppViewModel

@Composable
fun FineTunerPanel(
    viewModel: AppViewModel,
    selectedElementId: String?,
    elements: List<CanvasElement>
) {
    val element = elements.find { it.id == selectedElementId }
    if (element == null) {
        Text(
            text = "Select an element to fine‑tune",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }

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

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
    }
}

@Composable
private fun CompactPropertyField(
    label: String,
    value: Float,
    step: Float = 1f,
    valueRange: ClosedFloatingPointRange<Float> = -Float.MAX_VALUE..Float.MAX_VALUE,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
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