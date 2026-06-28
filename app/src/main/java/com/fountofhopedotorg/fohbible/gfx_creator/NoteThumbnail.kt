package com.fountofhopedotorg.fohbible.gfx_creator

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.ThemeColors

@Composable
fun NoteThumbnail(note: CanvasNote, themeColors: ThemeColors, gradientConfig: GradientConfig? = null) {
    when {
        note.content.startsWith("Shape:") -> {
            val shapeContent = note.content.removePrefix("Shape:").removePrefix(" ").trim()
            val shapeColor = note.backgroundColor
            when {
                shapeContent.startsWith("Square") ->
                    SquareShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Circle") ->
                    CircleShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Triangle") ->
                    TriangleShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Pentagon") ->
                    PolygonShape(
                        points = listOf(
                            Offset(0.5f, 0f), Offset(1f, 0.4f), Offset(0.8f, 0.9f),
                            Offset(0.2f, 0.9f), Offset(0f, 0.4f)
                        ),
                        modifier = Modifier.size(18.dp),
                        color = shapeColor,
                        gradientConfig = gradientConfig
                    )
                shapeContent.startsWith("Line") ->
                    LineShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Hexagon") ->
                    HexagonShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Star") ->
                    StarShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Diamond") ->
                    DiamondShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Heart") ->
                    HeartShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("ArrowRight") ->
                    ArrowRightShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Octagon") ->
                    OctagonShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Cross") ->
                    CrossShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("ThornCrown") ->
                    ThornCrownShape(modifier = Modifier.size(22.dp), thornColor = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Moon") ->
                    MoonShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("DavidStar") ->
                    DavidStarShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("Gear") ->
                    GearShape(modifier = Modifier.size(18.dp), color = shapeColor, gradientConfig = gradientConfig)
                shapeContent.startsWith("CustomLine:") -> {
                    val pointsData = shapeContent.removePrefix("CustomLine:")
                    CustomPathPreview(
                        pointsData = pointsData,
                        isClosed = false,
                        color = shapeColor,
                        modifier = Modifier.size(18.dp),
                        gradientConfig = gradientConfig
                    )
                }
                shapeContent.startsWith("CustomPolygon:") -> {
                    val pointsData = shapeContent.removePrefix("CustomPolygon:")
                    CustomPathPreview(
                        pointsData = pointsData,
                        isClosed = true,
                        color = shapeColor,
                        modifier = Modifier.size(18.dp),
                        gradientConfig = gradientConfig
                    )
                }
                else -> Icon(Icons.Default.ShapeLine, null, modifier = Modifier.size(18.dp), tint = shapeColor)
            }
        }
        note.content.startsWith("Image:") -> Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp), tint = themeColors.primary)
        else -> Icon(Icons.Default.TextFields, null, modifier = Modifier.size(18.dp), tint = themeColors.primary)
    }
}