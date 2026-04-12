package com.fountofhopedotorg.fohbible.modals
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun VerseOptionsModal(
    show: Boolean,
    onDismiss: () -> Unit,
    passage: PassageSelection,
    verse: Verse,
    chapterVerses: List<Verse> = emptyList(),
    databaseHelper: DatabaseHelper?,
    onAddBookmark: () -> Unit,
    onAddHighlight: () -> Unit,
    onShare: (List<Verse>) -> Unit,
    onAddNote: (List<Verse>) -> Unit = {},
    appViewModel: AppViewModel
) {
    var currentStart by remember { mutableIntStateOf(verse.verseNumber) }
    var currentEnd by remember { mutableIntStateOf(verse.verseNumber) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isHighlighted by remember { mutableStateOf(false) }
    val minV = min(currentStart, currentEnd)
    val maxV = max(currentStart, currentEnd)
    val currentVerses = chapterVerses.filter { it.verseNumber in minV..maxV }.sortedBy { it.verseNumber }
    LaunchedEffect(currentVerses) {
        isBookmarked = currentVerses.all {
            databaseHelper?.isBookmarked(it.copy(bookName = passage.bookName, chapter = passage.chapter)) ?: false
        }
        isHighlighted = currentVerses.all {
            databaseHelper?.isHighlighted(it.copy(bookName = passage.bookName, chapter = passage.chapter)) ?: false
        }
    }
    val displayText = if (currentVerses.isNotEmpty()) {
        currentVerses.joinToString("\n") { "${it.verseNumber} ${SimpleVerseProcessor.stripXmlTags(it.text)}" }
    } else {
        SimpleVerseProcessor.stripXmlTags(verse.text)
    }
    val rangeString = if (minV == maxV) "$minV" else "$minV-$maxV"
    val maxVerse = remember(chapterVerses) { chapterVerses.maxOfOrNull { it.verseNumber } ?: verse.verseNumber }

    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(10.dp), clip = true),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (appViewModel.darkTheme) appViewModel.darkModalBackgroundColor else appViewModel.lightModalBackgroundColor
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    0.0f to LocalAppTheme.current.primaryColor,
                                    0.25f to LocalAppTheme.current.primaryColor,
                                    1.0f to Color.Transparent
                                )
                            )
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Verse Options",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${passage.bookName} ${passage.chapter}:$rangeString",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .size(30.dp),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    if (chapterVerses.isNotEmpty()) {
                        Text(
                            text = "Edit Range",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                        )
                        RangeSliderWithControls(
                            currentStart = currentStart,
                            currentEnd = currentEnd,
                            maxVerse = maxVerse,
                            onStartChange = { currentStart = it },
                            onEndChange = { currentEnd = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        )
                        Text(
                            text = "Selected ${currentEnd - currentStart + 1} verse${if (currentEnd - currentStart + 1 != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = if (isBookmarked) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                            title = if (isBookmarked) "Bookmarked" else "Add Bookmark",
                            subtitle = if (isBookmarked) "Tap to remove" else "Save this verse for later",
                            isActive = isBookmarked,
                            onClick = {
                                if (isBookmarked) {
                                    currentVerses.forEach {
                                        databaseHelper?.removeBookmark(it.copy(bookName = passage.bookName, chapter = passage.chapter))
                                    }
                                } else {
                                    currentVerses.forEach {
                                        databaseHelper?.addBookmark(it.copy(bookName = passage.bookName, chapter = passage.chapter))
                                    }
                                }
                                onAddBookmark()
                                onDismiss()
                            }
                        )
                        ActionButton(
                            icon = if (isHighlighted) Icons.Default.Star else Icons.Outlined.StarBorder,
                            title = if (isHighlighted) "Highlighted" else "Add Highlight",
                            subtitle = if (isHighlighted) "Tap to remove" else "Mark this verse as important",
                            isActive = isHighlighted,
                            onClick = {
                                if (isHighlighted) {
                                    currentVerses.forEach {
                                        databaseHelper?.removeHighlight(it.copy(bookName = passage.bookName, chapter = passage.chapter))
                                    }
                                    onAddHighlight()
                                    onDismiss()
                                } else {
                                    val highlightColor = appViewModel.verseMarkerColor.toArgb()
                                    currentVerses.forEach {
                                        databaseHelper?.addHighlight(
                                            it.copy(bookName = passage.bookName, chapter = passage.chapter),
                                            highlightColor
                                        )
                                    }
                                    onAddHighlight()
                                    onDismiss()
                                }
                            }
                        )
                        ActionButton(
                            icon = Icons.Outlined.Share,
                            title = if (currentStart == currentEnd) "Share Verse" else "Share Verses",
                            subtitle = "Share with friends or social media",
                            onClick = {
                                onShare(currentVerses)
                                onDismiss()
                            }
                        )
                        ActionButton(
                            icon = Icons.AutoMirrored.Filled.Note,
                            title = if (currentStart == currentEnd) "Add Note" else "Add Notes",
                            subtitle = "Add personal notes",
                            onClick = {
                                onAddNote(currentVerses)
                                onDismiss()
                            }
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSliderWithControls(
    currentStart: Int,
    currentEnd: Int,
    maxVerse: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RangeSlider(
            value = currentStart.toFloat()..currentEnd.toFloat(),
            onValueChange = { range ->
                onStartChange(range.start.roundToInt())
                onEndChange(range.endInclusive.roundToInt())
            },
            valueRange = 1f..maxVerse.toFloat(),
            steps = maxVerse - 1,
            modifier = Modifier.fillMaxWidth(),
            startThumb = { Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) },
            endThumb = { Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) },
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { if (currentStart > 1) onStartChange(currentStart - 1) }, enabled = currentStart > 1) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease start")
                }
                Text(text = currentStart.toString(), modifier = Modifier.padding(horizontal = 4.dp), textAlign = TextAlign.Center)
                IconButton(onClick = { if (currentStart < currentEnd) onStartChange(currentStart + 1) }, enabled = currentStart < currentEnd) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase start")
                }
            }
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { if (currentEnd > currentStart) onEndChange(currentEnd - 1) }, enabled = currentEnd > currentStart) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease end")
                }
                Text(text = currentEnd.toString(), modifier = Modifier.padding(horizontal = 4.dp), textAlign = TextAlign.Center)
                IconButton(onClick = { if (currentEnd < maxVerse) onEndChange(currentEnd + 1) }, enabled = currentEnd < maxVerse) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase end")
                }
            }
        }
    }
}
@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(70.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}