package com.fountofhopedotorg.fohbible.home

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fountofhopedotorg.fohbible.R
import com.fountofhopedotorg.fohbible.creator.getRandomColor
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.PopularDevotional
import com.fountofhopedotorg.fohbible.data.QuickAction
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun UsefulSpaceGrid(
    onCreateSermonMaterialsClick: () -> Unit,
    onTakeBibleQuizClick: () -> Unit,
    onLearnHebrewClick: () -> Unit,
    onLearnGreekClick: () -> Unit,
    onOpenDictionaryClick: () -> Unit = {},
    onOpenVideoEditorClick: () -> Unit = {}
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
                        val isOccupied = index < 6

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
                                                3 -> onLearnGreekClick()
                                                4 -> onOpenDictionaryClick()
                                                5 -> onOpenVideoEditorClick()
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
                                            3 -> Icons.Filled.School
                                            4 -> Icons.Filled.Book
                                            5 -> Icons.Filled.PlayArrow
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
                                            3 -> "Learn Greek"
                                            4 -> "Dictionary"
                                            5 -> "Video Editor"
                                            else -> ""
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

@Composable
fun HomeHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fount of Hope",
            fontSize = 25.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            fontFamily = getFontFamily("rubikglitch")
        )
        Image(
            painter = painterResource(id = R.drawable.foh),
            contentDescription = "Fount of Hope Logo",
            modifier = Modifier.size(160.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
        Text(
            text = "Study Bible",
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            fontFamily = getFontFamily("rubikglitch")
        )
        Text(
            text = "Your Daily Source of Inspiration",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            fontFamily = getFontFamily("oswald")
        )
    }
    Spacer(Modifier.height(30.dp))
}

@Composable
fun QuickAccessCarousel(actions: List<QuickAction>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(actions) { action ->
            QuickActionCarouselItem(action = action)
        }
    }
}

@Composable
fun QuickActionCarouselItem(action: QuickAction) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp)
            .clickable(onClick = action.onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(action.backgroundImage)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                alpha = 0.9f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (action.title == "Notes") Modifier.rotate(90f) else Modifier)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
            }
        }
    }
}
@Composable
fun DailyVerseCard(
    verses: List<Verse>?,
    onRefresh: () -> Unit,
    onClick: (List<Verse>) -> Unit,
    databaseHelper: DatabaseHelper,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentFontFamily = getFontFamily(viewModel.selectedFontFamily)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val imageSelector = remember(viewModel) { RandomImageSelector(viewModel) }

    var imageSrc by remember { mutableStateOf(imageSelector.getRandomImage(isLandscape)) }
    var imageLoaded by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isBookmarked by remember(verses) { mutableStateOf(false) }

    LaunchedEffect(isLandscape) {
        imageSrc = imageSelector.getRandomImage(isLandscape)
        imageLoaded = false
        imageError = false
    }

    LaunchedEffect(verses) {
        if (!verses.isNullOrEmpty()) {
            isBookmarked = BookmarkHelper.isBookmarked(verses.first(), databaseHelper)
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageSrc)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Background image",
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = { imageLoaded = true },
                    onError = { imageError = false }
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fresh Revelations",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = currentFontFamily
                        )
                        IconButton(
                            onClick = {
                                isLoading = true
                                onRefresh()
                                scope.launch {
                                    delay(500.milliseconds)
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White, modifier = Modifier.padding(top = 2.dp, bottom = 25.dp))

                    if (!verses.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().clickable { onClick(verses) }) {
                            Column {
                                val reference = SimpleVerseProcessor.extractVerseReference(verses)
                                Text(
                                    text = reference,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontFamily = currentFontFamily
                                )
                                Spacer(Modifier.height(8.dp))
                                verses.forEach { verse ->
                                    val annotatedText = buildAnnotatedString {
                                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                                            append("${verse.verseNumber} ")
                                        }
                                        append(SimpleVerseProcessor.stripXmlTags(verse.text))
                                    }
                                    Text(
                                        text = annotatedText,
                                        fontSize = 18.sp,
                                        lineHeight = 22.sp,
                                        textAlign = TextAlign.Justify,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        fontFamily = currentFontFamily,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                verses?.let { verseList ->
                                    scope.launch {
                                        if (isBookmarked) {
                                            BookmarkHelper.removeBookmarks(verseList, databaseHelper)
                                        } else {
                                            BookmarkHelper.addBookmarks(verseList, databaseHelper)
                                        }
                                        isBookmarked = !isBookmarked
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Share",
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable {
                                    verses?.let { VerseShareHelper.shareVerses(context, it) }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PopularDevotionalsSection(
    devotionals: List<PopularDevotional>,
    onNavigateToReader: (PassageSelection) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Popular Devotionals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        devotionals.forEachIndexed { index, devotional ->
            DevotionalItem(devotional = devotional, onNavigateToReader = onNavigateToReader)
            if (index < devotionals.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun DevotionalItem(
    devotional: PopularDevotional,
    onNavigateToReader: (PassageSelection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val bookNumber = BibleData.getBookByName(devotional.bookName)?.customNumber ?: 1
                onNavigateToReader(
                    PassageSelection(
                        bookNumber = bookNumber,
                        bookName = devotional.bookName,
                        chapter = devotional.chapter,
                        verse = devotional.verse
                    )
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            getRandomColor().copy(0.4f),
                            getRandomColor().copy(0.02f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = devotional.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = devotional.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = {}) {
            Icon(Icons.Filled.BookmarkBorder, contentDescription = "Bookmark", tint = MaterialTheme.colorScheme.primary)
        }
    }
}