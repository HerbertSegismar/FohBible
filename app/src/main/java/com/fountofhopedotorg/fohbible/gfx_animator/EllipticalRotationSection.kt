package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun EllipticalRotationSection(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    stretchX: Float,
    onStretchXChange: (Float) -> Unit,
    stretchY: Float,
    onStretchYChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Elliptical rotation",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.size(24.dp)
            )
        }

        if (isEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            CompactSliderWithLabel(
                label = "Stretch X",
                value = stretchX,
                onValueChange = onStretchXChange,
                valueRange = 0.1f..10f
            )
            Spacer(modifier = Modifier.height(2.dp))
            CompactSliderWithLabel(
                label = "Stretch Y",
                value = stretchY,
                onValueChange = onStretchYChange,
                valueRange = 0.1f..10f
            )
        }
    }
}

@Composable
fun CompactSliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(50.dp)
        )
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
        )
        Text(
            text = String.format(Locale.US, "%.2f", value),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(40.dp)
        )
    }
}