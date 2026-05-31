package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.ThemeColors

@Composable
fun InputModeSelector(
    selectedInputMode: String,
    onModeSelected: (String) -> Unit,
    themeColors: ThemeColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val modes = listOf(
            Triple("Add Text", Icons.Default.TextFields, "Add Text"),
            Triple("Fetch Verse", Icons.Default.Book, "Fetch Verse"),
            Triple("Add SVG", Icons.Default.FormatShapes, "Add SVG"),
            Triple("Add Image", Icons.Default.Image, "Add Image")
        )

        modes.forEach { (mode, icon, desc) ->
            val isSelected = selectedInputMode == mode
            IconButton(
                onClick = { onModeSelected(mode) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .background(
                        color = if (isSelected) themeColors.primary.copy(alpha = 0.2f) else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    tint = if (isSelected) themeColors.primary else themeColors.textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}