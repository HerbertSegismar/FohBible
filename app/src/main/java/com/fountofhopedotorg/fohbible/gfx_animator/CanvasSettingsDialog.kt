package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.color_wheel.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.GradientConfig

@Composable
fun CanvasSettingsDialog(
    initialWidth: Int,
    initialHeight: Int,
    onDismiss: () -> Unit,
    onConfirmSize: (width: Int, height: Int) -> Unit,
    onSolidColorSelected: (Color) -> Unit,
    onGradientSelected: (Brush) -> Unit,
    onTransparentSelected: () -> Unit
) {
    var selectedMainTab by remember { mutableIntStateOf(0) }
    val mainTabs = listOf("Size", "Background")

    var widthText by remember { mutableStateOf(initialWidth.toString()) }
    var heightText by remember { mutableStateOf(initialHeight.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedBgTab by remember { mutableIntStateOf(0) }
    val bgTabs = listOf("Color", "Gradient", "Clear")

    var showAdvancedColorPicker by remember { mutableStateOf(false) }
    var advancedPickerIsGradient by remember { mutableStateOf(false) }

    val dummyGradientConfig = GradientConfig(
        startColor = Color.White,
        endColor = Color.Black,
        startOffset = Offset(0f, 0f),
        endOffset = Offset(1f, 1f)
    )

    val colorPresets = listOf(
        Color.White, Color.Black, Color.DarkGray, Color.LightGray,
        Color(0xFFF44336), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFEB3B),
        Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFFE91E63)
    )

    val gradientPresets = listOf(
        Brush.linearGradient(listOf(Color(0xFFFF5F6D), Color(0xFFFFC371))), // Sunset
        Brush.linearGradient(listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))), // Cool Blue
        Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))), // Neon Green
        Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFE100FF)))  // Royal Purple
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Canvas Settings") },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
            ) {
                PrimaryTabRow(selectedTabIndex = selectedMainTab) {
                    mainTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedMainTab == index,
                            onClick = { selectedMainTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedMainTab) {
                    0 -> {
                        Column {
                            OutlinedTextField(
                                value = widthText,
                                onValueChange = { widthText = it.filter { c -> c.isDigit() } },
                                label = { Text("Width") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it.filter { c -> c.isDigit() } },
                                label = { Text("Height") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    1 -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SecondaryTabRow(selectedTabIndex = selectedBgTab) {
                                bgTabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedBgTab == index,
                                        onClick = { selectedBgTab = index },
                                        text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            when (selectedBgTab) {
                                0 -> {
                                    Column {
                                        Text("Select a Solid Color:", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val rows = colorPresets.chunked(2)
                                        for (row in rows) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                row.forEach { color ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(50.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(color)
                                                            .clickable {
                                                                onSolidColorSelected(color)
                                                                onDismiss()
                                                            }
                                                    )
                                                }
                                                if (row.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                advancedPickerIsGradient = false
                                                showAdvancedColorPicker = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Custom Color", color = Color.White)
                                        }
                                    }
                                }
                                1 -> {
                                    Column {
                                        Text("Select a Gradient:", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.height(120.dp)
                                        ) {
                                            items(gradientPresets) { brush ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(brush)
                                                        .clickable {
                                                            onGradientSelected(brush)
                                                            onDismiss()
                                                        }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                advancedPickerIsGradient = true
                                                showAdvancedColorPicker = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Custom Gradient", color = Color.White)
                                        }
                                    }
                                }
                                2 -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Remove background styling?", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                onTransparentSelected()
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Make Transparent", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedMainTab == 0) {
                TextButton(onClick = {
                    val w = widthText.toIntOrNull()
                    val h = heightText.toIntOrNull()
                    if (w != null && h != null && w > 0) {
                        onConfirmSize(w, h)
                        onDismiss()
                    } else {
                        errorMessage = "Please enter positive integers."
                    }
                }) {
                    Text("Apply Size")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAdvancedColorPicker) {
        ColorWheelDialog(
            onDismissRequest = { showAdvancedColorPicker = false },
            onColorSelected = { color ->
                showAdvancedColorPicker = false
                onSolidColorSelected(color)
                onDismiss()
            },
            onGradientSelected = { start, end, startOff, endOff ->
                showAdvancedColorPicker = false

                val customBrush = object : ShaderBrush() {
                    override fun createShader(size: Size): Shader {
                        return LinearGradientShader(
                            colors = listOf(start, end),
                            from = Offset(startOff.x * size.width, startOff.y * size.height),
                            to = Offset(endOff.x * size.width, endOff.y * size.height)
                        )
                    }
                }

                onGradientSelected(customBrush)
                onDismiss()
            },
            enableGradient = true,
            initialGradientConfig = if (advancedPickerIsGradient) dummyGradientConfig else null
        )
    }
}