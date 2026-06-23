package com.fountofhopedotorg.fohbible.modals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.fountofhopedotorg.fohbible.app_composables.FloatingOrbsBackground
import com.fountofhopedotorg.fohbible.modal_functions.ColorSplashCanvas
import com.fountofhopedotorg.fohbible.modal_functions.SelectableBox

@Composable
fun BgModal(
    currentIndex: Int,
    customUri: String?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onPickCustom: () -> Unit,
    onRemoveCustom: () -> Unit
) {
    val noneIndex = 0
    val firstIndex = 1
    val lastIndex = 33
    val randomIndex = 34
    val floatingOrbsIndex = 35
    val customIndex = 36

    val lazyListState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        val targetPosition = when (currentIndex) {
            in firstIndex..lastIndex -> currentIndex
            randomIndex -> randomIndex
            floatingOrbsIndex -> floatingOrbsIndex
            customIndex -> customIndex
            else -> 0
        }
        lazyListState.scrollToItem(targetPosition)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Background",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                LazyRow(
                    state = lazyListState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SelectableBox(
                            selected = currentIndex == noneIndex,
                            onClick = { onSelect(noneIndex) }
                        ) {
                            Text(
                                text = "None",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (currentIndex == noneIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            )
                        }
                    }
                    items(lastIndex) { i ->
                        val index = i + 1
                        SelectableBox(
                            selected = currentIndex == index,
                            onClick = { onSelect(index) }
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/textures/$index.jpg",
                                contentDescription = "Texture $index",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            )
                        }
                    }
                    item {
                        SelectableBox(
                            selected = currentIndex == randomIndex,
                            onClick = { onSelect(randomIndex) }
                        ) {
                            ColorSplashCanvas()
                        }
                    }

                    item {
                        SelectableBox(
                            selected = currentIndex == floatingOrbsIndex,
                            onClick = { onSelect(floatingOrbsIndex) }
                        ) {
                            FloatingOrbsBackground(orbCount = 2)
                        }
                    }

                    item {
                        if (customUri != null) {
                            SelectableBox(
                                selected = currentIndex == customIndex,
                                onClick = { onSelect(customIndex) }
                            ) {
                                AsyncImage(
                                    model = if (customUri.startsWith("/")) "file://$customUri" else customUri,
                                    contentDescription = "Custom texture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center
                                )
                            }
                        } else {
                            SelectableBox(
                                selected = currentIndex == customIndex,
                                onClick = onPickCustom
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add custom",
                                    tint = if (currentIndex == customIndex)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onPickCustom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = "Add Custom Image", color = Color.White)
                }

                if (customUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onRemoveCustom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Remove Custom Image",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text(text = "Close", color = Color.White)
                    }
                }
            }
        }
    }
}