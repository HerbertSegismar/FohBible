package com.fountofhopedotorg.fohbible.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

@Composable
fun BgModal(
    currentIndex: Int,
    customUri: String?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onPickCustom: () -> Unit,
    onRemoveCustom: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        val targetPosition = when (currentIndex) {
            in 1..33 -> currentIndex
            34 -> 34
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                LazyRow(
                    state = lazyListState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SelectableBox(
                            selected = currentIndex == 0,
                            onClick = { onSelect(0) }
                        ) {
                            Text(
                                text = "None",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (currentIndex == 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            )
                        }
                    }
                    items(33) { i ->
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
                        if (customUri != null) {
                            SelectableBox(
                                selected = currentIndex == 34,
                                onClick = { onSelect(34) }
                            ) {
                                AsyncImage(
                                    model = customUri,
                                    contentDescription = "Custom texture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center
                                )
                            }
                        } else {
                            SelectableBox(
                                selected = currentIndex == 34,
                                onClick = onPickCustom
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add custom",
                                    tint = if (currentIndex == 34)
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
                    Text(
                        text = "Add Custom Image",
                        color = Color.White
                    )
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
                        Text(text = "Cancel", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableBox(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    Color.Transparent
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}