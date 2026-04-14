package com.fountofhopedotorg.fohbible.dropdowns

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.fountofhopedotorg.fohbible.ReaderDropdownContent
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.models.AppViewModel

@Composable
fun ReaderAppBarMenu(
    isLandscape: Boolean,
    viewModel: AppViewModel,
    onScreenChange: (Screen) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (showMenu) 180f else 0f,
        animationSpec = tween(300),
        label = "menuIconRotation"
    )

    IconButton(
        onClick = { showMenu = !showMenu },
        modifier = modifier
    ) {
        Crossfade(
            targetState = showMenu,
            animationSpec = tween(300),
            label = "iconCrossfade"
        ) { isOpen ->
            Icon(
                imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Menu,
                contentDescription = if (isOpen) "Close Navigation" else "Open Navigation",
                tint = Color.White,
                modifier = Modifier.rotate(rotation)
            )
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        modifier = Modifier.background(
            if (viewModel.darkTheme) viewModel.darkModalBackgroundColor
            else viewModel.lightModalBackgroundColor
        )
    ) {
        ReaderDropdownContent(
            isLandscape = isLandscape,
            viewModel = viewModel,
            onScreenChange = onScreenChange,
            coroutineScope = coroutineScope
        )
    }
}