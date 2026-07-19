package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.KeyframeAnimationContent

val GradientConfigNullableSaver = Saver<GradientConfig?, String>(
    save = { config ->
        if (config != null) {
            listOf(
                config.startColor.toArgb(),
                config.endColor.toArgb(),
                config.startOffset.x,
                config.startOffset.y,
                config.endOffset.x,
                config.endOffset.y
            ).joinToString(",")
        } else {
            "NULL"
        }
    },
    restore = { str ->
        if (str == "NULL") null
        else {
            val parts = str.split(",").map { it.toFloatOrNull() ?: 0f }
            if (parts.size < 6) null
            else GradientConfig(
                startColor = Color(parts[0].toInt()),
                endColor = Color(parts[1].toInt()),
                startOffset = Offset(parts[2], parts[3]),
                endOffset = Offset(parts[4], parts[5])
            )
        }
    }
)

const val MAX_VISIBLE_DURATION_MS = 10_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyframeAnimationDialog(
    element: CanvasElement?,
    allElements: List<CanvasElement>,
    onElementSelected: (CanvasElement) -> Unit,
    onDismiss: () -> Unit,
    onSaveKeyframes: (String, List<CanvasKeyframe>, Long, Long) -> Unit,
    timeMultiplier: Float,
    initialGradientConfig: GradientConfig? = null,
    canvasWidth: Int,
    canvasHeight: Int
) {
    if (element == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        title = {
            Text(
                "Timeline Animation",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
        KeyframeAnimationContent(
            element = element,
            allElements = allElements,
            onElementSelected = onElementSelected,
            onCancel = onDismiss,
            onSave = { elementId, keyframes, startMs, endMs ->
                onSaveKeyframes(elementId, keyframes, startMs, endMs)
                onDismiss()
            },
            timeMultiplier = timeMultiplier,
            initialGradientConfig = initialGradientConfig,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight
        )
        },
        confirmButton = {},
        dismissButton = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}