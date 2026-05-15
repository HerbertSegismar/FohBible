package com.fountofhopedotorg.fohbible.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.composables.Footer
import com.fountofhopedotorg.fohbible.composables.ImageSection
import com.fountofhopedotorg.fohbible.composables.MatrixNative
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.PopularDevotional
import com.fountofhopedotorg.fohbible.data.QuickAction
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private object BookmarkHelper {
    suspend fun isBookmarked(verse: Verse, dbHelper: DatabaseHelper): Boolean =
        withContext(Dispatchers.IO) { dbHelper.isBookmarked(verse) }

    suspend fun addBookmarks(verses: List<Verse>, dbHelper: DatabaseHelper) =
        withContext(Dispatchers.IO) { verses.forEach { dbHelper.addBookmark(it) } }

    suspend fun removeBookmarks(verses: List<Verse>, dbHelper: DatabaseHelper) =
        withContext(Dispatchers.IO) { verses.forEach { dbHelper.removeBookmark(it) } }
}

private object VerseShareHelper {
    fun buildShareText(verses: List<Verse>): String = buildString {
        verses.forEach { verse ->
            val cleanedText = SimpleVerseProcessor.stripXmlTags(verse.text)
            append("${verse.bookName ?: ""} ${verse.chapter ?: 0}:${verse.verseNumber} $cleanedText\n")
        }
    }

    fun shareVerses(context: Context, verses: List<Verse>) {
        val shareText = buildShareText(verses)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share verses via"))
    }
}

private suspend fun loadRandomVerses(context: Context, dbHelper: DatabaseHelper?): List<Verse> =
    withContext(Dispatchers.IO) {
        val helper = dbHelper ?: DatabaseHelper(
            context as com.fountofhopedotorg.fohbible.MainActivity,
            databaseName = "kj2.sqlite3"
        )
        val verses = helper.getRandomVerses()
        if (dbHelper == null) helper.close()
        verses
    }

private class RandomImageSelector(private val viewModel: AppViewModel) {
    fun getRandomImage(isLandscape: Boolean): String {
        val assetPath = if (!isLandscape) "images/" else "images-md/"
        val fileList = if (!isLandscape) viewModel.imageFilesSm else viewModel.imageFilesMd
        val randomFile = fileList.random()
        return "file:///android_asset/$assetPath$randomFile"
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToReader: (PassageSelection) -> Unit,
    onNavigateToScreen: (Screen) -> Unit,
    databaseHelper: DatabaseHelper? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<AppViewModel>()

    var dailyVerses by remember { mutableStateOf<List<Verse>?>(null) }
    var popularDevotionals by remember { mutableStateOf<List<PopularDevotional>>(emptyList()) }

    LaunchedEffect(Unit) {
        dailyVerses = loadRandomVerses(context, databaseHelper)
    }
    LaunchedEffect(Unit) {
        popularDevotionals = getRandomDevotionals()
    }

    val quickActions = listOf(
        QuickAction("Reader", Icons.Filled.Book, MaterialTheme.colorScheme.primary) {
            onNavigateToScreen(Screen.Reader())
        },
        QuickAction("Bookmarks", Icons.Filled.Bookmark, MaterialTheme.colorScheme.primary) {
            onNavigateToScreen(Screen.Bookmarks)
        },
        QuickAction("Notes", Icons.AutoMirrored.Filled.Note, MaterialTheme.colorScheme.primary) {
            onNavigateToScreen(Screen.Notes)
        },
        QuickAction("Search", Icons.Filled.Search, MaterialTheme.colorScheme.primary) {
            onNavigateToScreen(Screen.Search)
        },
        QuickAction("Settings", Icons.Filled.Settings, MaterialTheme.colorScheme.primary) {
            onNavigateToScreen(Screen.Settings)
        }
    )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item { HomeHeader() }

        item {  ImageSection(onNavigateToReader = onNavigateToReader) }

        item {
            DailyVerseCard(
                verses = dailyVerses,
                onRefresh = {
                    scope.launch {
                        dailyVerses = loadRandomVerses(context, databaseHelper)
                    }
                },
                onClick = { verses ->
                    if (verses.isNotEmpty()) {
                        val first = verses.first()
                        val bookNumber = BibleData.getBookByName(first.bookName ?: "")?.customNumber ?: 1
                        onNavigateToReader(
                            PassageSelection(
                                bookNumber = bookNumber,
                                bookName = first.bookName ?: "Genesis",
                                chapter = first.chapter ?: 1,
                                verse = first.verseNumber
                            )
                        )
                    }
                },
                databaseHelper = databaseHelper ?: DatabaseHelper(
                    context as com.fountofhopedotorg.fohbible.MainActivity,
                    "kj2.sqlite3"
                ),
                viewModel = viewModel
            )
        }

        item {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            QuickAccessCarousel(actions = quickActions)
        }

        item {
            PopularDevotionalsSection(
                devotionals = popularDevotionals,
                onNavigateToReader = onNavigateToReader
            )
        }

        item { Spacer(Modifier.height(40.dp)) }

        item {
            val isMatrixVisible by remember { mutableStateOf(true) }
            if (isMatrixVisible) {
                key("matrix_component") { MatrixNative() }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
        item { Footer() }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun HomeHeader() {
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
private fun QuickAccessCarousel(actions: List<QuickAction>) {
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
private fun QuickActionCarouselItem(action: QuickAction) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp)
            .clickable(onClick = action.onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = action.color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.color,
                modifier = Modifier.size(32.dp).then(
                    if (action.title == "Notes") Modifier.rotate(90f) else Modifier
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyVerseCard(
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
                                    delay(500)
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
private fun PopularDevotionalsSection(
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
private fun DevotionalItem(
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
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
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

private fun getRandomDevotionals(): List<PopularDevotional> {
    val allDevotionals = listOf(
        PopularDevotional("Psalm 23", "The Lord is my shepherd...", "Psalms", 23, 1),
        PopularDevotional("The Lord's Prayer", "Our Father in heaven...", "Matthew", 6, 9),
        PopularDevotional("The Love Chapter", "Love is patient, love is kind...", "1 Corinthians", 13, 1),
        PopularDevotional("John 3:16", "For God so loved the world...", "John", 3, 16),
        PopularDevotional("The Beatitudes", "Blessed are the poor in spirit...", "Matthew", 5, 3),
        PopularDevotional("The Good Samaritan", "A man was going down from Jerusalem...", "Luke", 10, 30),
        PopularDevotional("The Prodigal Son", "There was a man who had two sons...", "Luke", 15, 11),
        PopularDevotional("The Sermon on the Mount", "Seeing the crowds, he went up on the mountain...", "Matthew", 5, 1),
        PopularDevotional("The Great Commandment", "You shall love the Lord your God...", "Matthew", 22, 37),
        PopularDevotional("The Golden Rule", "So whatever you wish that others would do to you...", "Matthew", 7, 12),
        PopularDevotional("I Can Do All Things", "I can do all things through him who strengthens me...", "Philippians", 4, 13),
        PopularDevotional("God's Plans for You", "For I know the plans I have for you...", "Jeremiah", 29, 11),
        PopularDevotional("Trust in the Lord", "Trust in the Lord with all your heart...", "Proverbs", 3, 5),
        PopularDevotional("All Things Work Together for Good", "And we know that for those who love God all things work together for good...", "Romans", 8, 28),
        PopularDevotional("Wait for the Lord", "But they who wait for the Lord shall renew their strength...", "Isaiah", 40, 31),
        PopularDevotional("Saved by Grace", "For by grace you have been saved through faith...", "Ephesians", 2, 8),
        PopularDevotional("Lamp to My Feet", "Your word is a lamp to my feet...", "Psalms", 119, 105),
        PopularDevotional("Be Strong and Courageous", "Have I not commanded you? Be strong and courageous...", "Joshua", 1, 9),
        PopularDevotional("Come to Me", "Come to me, all who labor and are heavy laden...", "Matthew", 11, 28),
        PopularDevotional("Rejoice Always", "Rejoice always, pray without ceasing...", "1 Thessalonians", 5, 16),
        PopularDevotional("The Alpha & Omega", "Behold, He comes with clouds...", "Revelation", 1, 7),
        PopularDevotional("The Armor of God", "Put on the whole armor of God...", "Ephesians", 6, 11),
        PopularDevotional("The Fruit of the Spirit", "But the fruit of the Spirit is love, joy, peace...", "Galatians", 5, 22),
        PopularDevotional("The Great Commission", "Go therefore and make disciples of all nations...", "Matthew", 28, 19),
        PopularDevotional("The New Commandment", "A new commandment I give to you, that you love one another...", "John", 13, 34),
        PopularDevotional("The Resurrection", "But in fact Christ has been raised from the dead...", "1 Corinthians", 15, 20),
        PopularDevotional("The Shema", "Hear, O Israel: The Lord our God, the Lord is one...", "Deuteronomy", 6, 4),
        PopularDevotional("The Ten Commandments", "And God spoke all these words, saying...", "Exodus", 20, 1),
        PopularDevotional("The Birth of Jesus", "And she gave birth to her firstborn son...", "Luke", 2, 7),
        PopularDevotional("The Crucifixion", "And when they had crucified him...", "Matthew", 27, 35),
        PopularDevotional("The Ascension", "And when he had said these things...", "Acts", 1, 9),
        PopularDevotional("The Holy Spirit Comes", "And suddenly there came from heaven a sound...", "Acts", 2, 2),
        PopularDevotional("The New Heaven and Earth", "Then I saw a new heaven and a new earth...", "Revelation", 21, 1),
        PopularDevotional("Faith Hall of Fame", "Now faith is the assurance of things hoped for...", "Hebrews", 11, 1),
        PopularDevotional("Nothing Can Separate Us", "For I am sure that neither death nor life...", "Romans", 8, 38),
        PopularDevotional("The Word Became Flesh", "In the beginning was the Word...", "John", 1, 1),
        PopularDevotional("The Parable of the Sower", "A sower went out to sow...", "Matthew", 13, 3),
        PopularDevotional("The Feeding of the 5,000", "Then Jesus took the loaves...", "John", 6, 11),
        PopularDevotional("The Good Shepherd", "I am the good shepherd...", "John", 10, 11),
        PopularDevotional("The Way and the Truth", "Jesus said to him, 'I am the way...'", "John", 14, 6),
        PopularDevotional("The Vine and Branches", "I am the vine; you are the branches...", "John", 15, 5),
        PopularDevotional("The Light of the World", "Again Jesus spoke to them, saying...", "John", 8, 12),
        PopularDevotional("The Resurrection and Life", "Jesus said to her, 'I am the resurrection...'", "John", 11, 25),
        PopularDevotional("The Transfiguration", "And he was transfigured before them...", "Matthew", 17, 2),
        PopularDevotional("The Widow's Offering", "And he called his disciples to him...", "Mark", 12, 43),
        PopularDevotional("The Suffering Servant", "He was despised and rejected by men, a man of sorrows...", "Isaiah", 53, 3),
        PopularDevotional("The Crucifixion Psalm", "My God, my God, why have you forsaken me?", "Psalms", 22, 1),
        PopularDevotional("The First Gospel", "He will crush your head, and you will strike his heel.", "Genesis", 3, 15),
        PopularDevotional("Prophecy of Bethlehem", "But you, Bethlehem Ephrathah... out of you will come for me one who will be ruler over Israel.", "Micah", 5, 2),
        PopularDevotional("The Triumphal Entry", "See, your king comes to you, righteous and victorious, lowly and riding on a donkey.", "Zechariah", 9, 9),
        PopularDevotional("The Virgin Birth", "The virgin will conceive and give birth to a son, and will call him Immanuel.", "Isaiah", 7, 14),
        PopularDevotional("The Messenger of the Covenant", "The Lord you are seeking will suddenly come to his temple.", "Malachi", 3, 1),
        PopularDevotional("The Son of Man", "He was given authority, glory and sovereign power; all nations worshiped him.", "Daniel", 7, 14),
        PopularDevotional("The Righteous Branch", "I will raise up for David a righteous Branch, a King who will reign wisely.", "Jeremiah", 23, 5),
        PopularDevotional("The Priest Forever", "You are a priest forever, in the order of Melchizedek.", "Psalms", 110, 4),
        PopularDevotional("The Passover Lamb", "The animals you choose must be year-old males without defect...", "Exodus", 12, 5),
        PopularDevotional("The Bronze Serpent", "So Moses made a bronze serpent and put it up on a pole...", "Numbers", 21, 9)
    )
    return allDevotionals.shuffled(Random).take(5)
}