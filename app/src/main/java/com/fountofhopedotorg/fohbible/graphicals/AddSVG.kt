package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.composables.CircleShape
import com.fountofhopedotorg.fohbible.composables.LineShape
import com.fountofhopedotorg.fohbible.composables.PolygonShape
import com.fountofhopedotorg.fohbible.composables.ShapeSelectionCard
import com.fountofhopedotorg.fohbible.composables.SquareShape
import com.fountofhopedotorg.fohbible.composables.TriangleShape
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.functions.getRandomColor


@Composable
fun AddSvgSection(
    onAddShape: (shape: String) -> Unit,
    onCustomPolygon: () -> Unit,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tap a shape to add to canvas:",
                style = MaterialTheme.typography.titleSmall,
                color = themeColors.textColor
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val pentagonPoints = listOf(
                    Offset(0.5f, 0f),
                    Offset(1f, 0.4f),
                    Offset(0.8f, 0.9f),
                    Offset(0.2f, 0.9f),
                    Offset(0f, 0.4f)
                )
                ShapeSelectionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddShape("Square") }
                ) {
                    SquareShape(modifier = Modifier.size(25.dp))
                }
                ShapeSelectionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddShape("Circle") }
                ) {
                    CircleShape(modifier = Modifier.size(25.dp))
                }
                ShapeSelectionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddShape("Triangle") }
                ) {
                    TriangleShape(modifier = Modifier.size(25.dp))
                }
                ShapeSelectionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddShape("Pentagon") }
                ) {
                    PolygonShape(
                        points = pentagonPoints,
                        modifier = Modifier.size(26.dp)
                    )
                }
                ShapeSelectionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onAddShape("Line") }
                ) {
                    LineShape(modifier = Modifier.size(18.dp).padding(top = 6.dp))
                }
                ShapeSelectionCard(
                    modifier = Modifier.weight(1f),
                    onClick = onCustomPolygon
                ) {
                    Icon(
                        imageVector = Icons.Default.ShapeLine,
                        contentDescription = "Custom Polygon",
                        modifier = Modifier.size(25.dp),
                        tint = getRandomColor().copy(0.8f)
                    )
                }
            }
        }
    }
}