package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material3.Icon
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
import com.fountofhopedotorg.fohbible.functions.getRandomColor


@Composable
fun AddSvgSection(
    onAddShape: (shape: String) -> Unit,
    onCustomPolygon: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val pentagonPoints = listOf(
            Offset(0.5000f, 0.0000f),
            Offset(0.9755f, 0.3455f),
            Offset(0.7939f, 0.9045f),
            Offset(0.2061f, 0.9045f),
            Offset(0.0245f, 0.3455f)
        )

        ShapeSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            onClick = { onAddShape("Square") }
        ) {
            SquareShape(modifier = Modifier.size(25.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            onClick = { onAddShape("Circle") }
        ) {
            CircleShape(modifier = Modifier.size(25.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            onClick = { onAddShape("Triangle") }
        ) {
            TriangleShape(modifier = Modifier.size(25.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            onClick = { onAddShape("Pentagon") }
        ) {
            PolygonShape(
                points = pentagonPoints,
                modifier = Modifier.size(26.dp)
            )
        }
        ShapeSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            onClick = { onAddShape("Line") }
        ) {
            LineShape(modifier = Modifier.size(18.dp).padding(top = 6.dp))
        }
        ShapeSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
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