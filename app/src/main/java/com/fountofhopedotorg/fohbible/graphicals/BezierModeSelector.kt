package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.composables.ActiveControl
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme


@Composable
fun BezierModeSelector(
    activeControl: ActiveControl,
    onControlSelected: (ActiveControl) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAppTheme.current
    Canvas(
        modifier = modifier
            .size(180.dp, 40.dp)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val w = size.width
                    val h = size.height

                    val anchorPos = Offset(w * 0.5f, h * 0.5f)
                    val handleInPos = Offset(w * 0.1f, h * 0.5f)
                    val handleOutPos = Offset(w * 0.9f, h * 0.5f)

                    val distAnchor = (tapOffset - anchorPos).getDistance()
                    val distIn = (tapOffset - handleInPos).getDistance()
                    val distOut = (tapOffset - handleOutPos).getDistance()
                    val threshold = 64f

                    when {
                        distIn < threshold -> onControlSelected(ActiveControl.HANDLE_IN)
                        distOut < threshold -> onControlSelected(ActiveControl.HANDLE_OUT)
                        distAnchor < threshold -> onControlSelected(ActiveControl.ANCHOR)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val anchor = Offset(w * 0.5f, h * 0.5f)
        val handleIn = Offset(w * 0.1f, h * 0.5f)
        val handleOut = Offset(w * 0.9f, h * 0.5f)

        val path = Path().apply {
            moveTo(handleIn.x, handleIn.y)
            cubicTo(
                w * 0.2f, h * 0.1f,
                w * 0.4f, h * 0.1f,
                anchor.x, anchor.y
            )
            cubicTo(
                w * 0.6f, h * 0.9f,
                w * 0.8f, h * 0.9f,
                handleOut.x, handleOut.y
            )
        }

        drawPath(
            path = path,
            color = Color(0xFF00BCD4),
            style = Stroke(width = 5.5f)
        )

        val isAnchorSelected = activeControl == ActiveControl.ANCHOR
        val isInSelected = activeControl == ActiveControl.HANDLE_IN
        val isOutSelected = activeControl == ActiveControl.HANDLE_OUT

        drawCircle(
            color = if (isAnchorSelected) theme.primaryColor else Color.Gray,
            radius = if (isAnchorSelected) 30f else 24f,
            center = anchor
        )
        drawCircle(
            color = if (isInSelected) Color.Yellow else Color.Gray,
            radius = if (isInSelected) 20f else 18f,
            center = handleIn
        )
        drawCircle(
            color = if (isOutSelected) Color.Magenta else Color.Gray,
            radius = if (isOutSelected) 20f else 18f,
            center = handleOut
        )
    }
}