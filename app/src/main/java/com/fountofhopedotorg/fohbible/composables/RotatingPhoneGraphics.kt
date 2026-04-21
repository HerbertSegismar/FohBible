package com.fountofhopedotorg.fohbible.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RotatingPhoneGraphics(
    isSquareAspect: Boolean,
    primaryColor: Color,
    viewColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "Orientation preview")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2500,
                    easing = FastOutSlowInEasing,
                    delayMillis = 1000
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "progress"
        )

        val animationMultiplier by animateFloatAsState(
            targetValue = if (isSquareAspect) 1f else 0f,
            animationSpec = tween(500),
            label = "animationControl"
        )

        val effectiveProgress = progress * animationMultiplier

        Text(
            text = "Portrait↔Landscape",
            style = MaterialTheme.typography.labelSmall,
            color = primaryColor,
            fontSize = 12.sp
        )

        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            val parentRotation = effectiveProgress * -90f
            val childRotation = effectiveProgress * 90f

            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 90.dp)
                    .graphicsLayer {
                        rotationZ = parentRotation
                        transformOrigin = TransformOrigin.Center
                        shape = RoundedCornerShape(6.dp)
                        clip = true
                    }
                    .background(viewColor)
            ) {
                OrientationInternalContent(
                    childRotation = childRotation,
                    viewColor = viewColor,
                    accentColor = primaryColor
                )
            }
        }

        Text(
            text = "Orientation Changes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun OrientationInternalContent(
    childRotation: Float,
    viewColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 5.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider()
        repeat(2) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .graphicsLayer { rotationZ = childRotation }
                    .clip(RoundedCornerShape(2.dp))
                    .background(viewColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(viewColor)
                )
            }
        }
        HorizontalDivider()
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(accentColor.copy(alpha = 0.3f), shape = CircleShape)
        )
    }
}