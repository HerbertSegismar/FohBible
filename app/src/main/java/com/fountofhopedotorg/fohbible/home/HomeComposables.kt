package com.fountofhopedotorg.fohbible.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.creator.getRandomColor
import com.fountofhopedotorg.fohbible.models.AppViewModel

@Composable
fun UsefulSpaceGrid(
    onCreateSermonMaterialsClick: () -> Unit,
    onTakeBibleQuizClick: () -> Unit,
    onLearnHebrewClick: () -> Unit,
    onLearnGreekClick: () -> Unit,
) {
    val viewModel: AppViewModel = viewModel()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val iconSize = if (isLandscape) 48.dp else 24.dp
    val comingSoonIconSize = if (isLandscape) 48.dp else 24.dp
    val gridSpacing = if (isLandscape) 14.dp else 10.dp

    val textStyle = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isLandscape) Modifier.widthIn(max = 600.dp) else Modifier)
                .background(
                    if (viewModel.darkTheme) Color.Black.copy(0.5f) else MaterialTheme.colorScheme.primary.copy(0.1f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Useful Space",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val isOccupied = index < 4

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            getRandomColor().copy(0.4f),
                                            getRandomColor().copy(0.02f)
                                        )
                                    )
                                )
                                .then(
                                    if (isOccupied) Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color.White),
                                        onClick = {
                                            when (index) {
                                                0 -> onCreateSermonMaterialsClick()
                                                1 -> onTakeBibleQuizClick()
                                                2 -> onLearnHebrewClick()
                                                else -> onLearnGreekClick()
                                            }
                                        }
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isOccupied) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.AutoMirrored.Filled.Note
                                            1 -> Icons.Filled.QuestionAnswer
                                            2 -> Icons.Filled.School
                                            else -> Icons.Filled.School
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .size(iconSize)
                                            .then(if (index == 0) Modifier.rotate(90f) else Modifier)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = when (index) {
                                            0 -> "Create Sermon"
                                            1 -> "Bible Quiz"
                                            2 -> "Learn Hebrew"
                                            else -> "Learn Greek"
                                        },
                                        style = textStyle,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "Coming Soon",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(comingSoonIconSize)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Coming Soon",
                                        style = textStyle,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(gridSpacing))
            }
        }
    }
}