package com.fountofhopedotorg.fohbible.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable
fun ScrollSyncButton(
    scrollSyncEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(300),
        label = "scrollSyncRotation"
    )

    Box {
        IconButton(
            onClick = {
                rotation += 180f
                onToggle()
            },
            modifier = modifier
        ) {
            Crossfade(
                targetState = scrollSyncEnabled,
                animationSpec = tween(300),
                label = "scrollSyncIconCrossfade"
            ) { enabled ->
                Icon(
                    imageVector = if (enabled) Icons.Filled.Link else Icons.Filled.LinkOff,
                    contentDescription = "Toggle Scroll Sync",
                    tint = Color.White,
                    modifier = Modifier.rotate(animatedRotation)
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading verses...")
    }
}