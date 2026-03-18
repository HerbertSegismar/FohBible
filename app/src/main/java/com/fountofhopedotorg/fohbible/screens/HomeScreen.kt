package com.fountofhopedotorg.fohbible.screens

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.Footer
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.R
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import com.fountofhopedotorg.fohbible.MatrixNative
import com.fountofhopedotorg.fohbible.Screen
import kotlin.random.Random

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

data class PopularDevotional(
    val title: String,
    val preview: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToReader: (PassageSelection) -> Unit,
    onNavigateToScreen: (Screen) -> Unit,
    databaseHelper: DatabaseHelper? = null
) {
    val context = LocalContext.current
    var dailyVerses by remember { mutableStateOf<List<Verse>?>(null) }

    LaunchedEffect(Unit) {
        if (dailyVerses == null) {
            loadRandomVerses(context, databaseHelper) { verses ->
                dailyVerses = verses
            }
        }
    }

    var popularDevotionals by remember { mutableStateOf<List<PopularDevotional>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (popularDevotionals.isEmpty()) {
            popularDevotionals = getRandomDevotionals()
        }
    }

    val quickActions = listOf(
        QuickAction(
            title = "Reader",
            icon = Icons.Filled.Book,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onNavigateToScreen(Screen.Reader()) }
        ),
        QuickAction(
            title = "Bookmarks",
            icon = Icons.Filled.Bookmark,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onNavigateToScreen(Screen.Bookmarks) }
        ),
        QuickAction(
            title = "Notes",
            icon = Icons.AutoMirrored.Filled.Note,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onNavigateToScreen(Screen.Notes) }
        ),
        QuickAction(
            title = "Search",
            icon = Icons.Filled.Search,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onNavigateToScreen(Screen.Search) }
        ),
        QuickAction(
            title = "Settings",
            icon = Icons.Filled.Settings,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onNavigateToScreen(Screen.Settings) }
        )
    )


    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fount of Hope",
                    fontSize = 25.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    fontFamily = getFontFamily("rubikglitch")
                )

                Image(
                    painter = painterResource(id = R.drawable.foh),
                    contentDescription = "Fount of Hope Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Bible App",
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    fontFamily = getFontFamily("rubikglitch")
                )

                Text(
                    text = "Your Daily Source of Inspiration",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    fontFamily = getFontFamily("oswald")
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        item {
            DailyVerseCard(
                verses = dailyVerses,
                onRefresh = {
                    loadRandomVerses(context, databaseHelper) { verses ->
                        dailyVerses = verses
                    }
                },
                onClick = { verses ->
                    if (verses.isNotEmpty()) {
                        val first = verses.first()
                        val bookNumber = BibleData.getBookByName(first.bookName ?: "")?.customNumber ?: 1
                        val passage = PassageSelection(
                            bookNumber = bookNumber,
                            bookName = first.bookName ?: "Genesis",
                            chapter = first.chapter ?: 1,
                            verse = first.verseNumber
                        )
                        onNavigateToReader(passage)
                    }
                },
                databaseHelper = databaseHelper ?: DatabaseHelper(context as MainActivity, "kj2.sqlite3")
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

        item { Spacer(modifier = Modifier.height(40.dp)) }

        item {
            val isMatrixVisible by remember { mutableStateOf(true) }
            if (isMatrixVisible) {
                key("matrix_component") {
                    MatrixNative()
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }

        item { Footer() }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun QuickAccessCarousel(actions: List<QuickAction>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
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
        colors = CardDefaults.cardColors(
            containerColor = action.color.copy(alpha = 0.1f)
        )
    ) {
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
                tint = action.color,
                modifier = Modifier.size(32.dp).then(if (action.title == "Notes") Modifier.rotate(90f) else Modifier)
            )
            Spacer(modifier = Modifier.height(8.dp))
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
fun DailyVerseCard(
    verses: List<Verse>? = null,
    onRefresh: () -> Unit = {},
    onClick: (List<Verse>) -> Unit = {},
    databaseHelper: DatabaseHelper
) {
    val context = LocalContext.current
    val viewModel = viewModel<AppViewModel>()
    val isLoading = remember { mutableStateOf(false) }
    var isBookmarked by remember(verses) { mutableStateOf(false) }

    val systemFont = FontFamily.Default
    val oswaldFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Oswald.ttf")) }
    val poppinsFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Poppins.ttf")) }
    val rubikGlitchFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/RubikGlitch.ttf")) }
    val rubikLinesFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/RubikLines.ttf")) }
    val cookieFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Cookie.ttf")) }
    val emilysCandyFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/EmilysCandy.ttf")) }
    val googleSansCodeFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/GoogleSansCode.ttf")) }
    val pirataOneFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/PirataOne.ttf")) }
    val quintessentialFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Quintessential.ttf")) }
    val rougeScriptFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/RougeScript.ttf")) }
    val sairaStencilOneFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/SairaStencilOne.ttf")) }
    val shadowsIntoLightFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/ShadowsIntoLight.ttf")) }
    val smoochSansFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/SmoochSans.ttf")) }
    val truculentaFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/Truculenta.ttf")) }
    val honkFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "fonts/HonkVariable.ttf")) }

    val currentFontFamily = when (viewModel.selectedFontFamily) {
        "system" -> systemFont
        "oswald" -> oswaldFont
        "rubikglitch" -> rubikGlitchFont
        "rubiklines" -> rubikLinesFont
        "poppins" -> poppinsFont
        "cookie" -> cookieFont
        "emilyscandy" -> emilysCandyFont
        "googlesanscode" -> googleSansCodeFont
        "pirataone" -> pirataOneFont
        "quintessential" -> quintessentialFont
        "rougescript" -> rougeScriptFont
        "sairastencilone" -> sairaStencilOneFont
        "shadowsintolight" -> shadowsIntoLightFont
        "smoochsans" -> smoochSansFont
        "truculenta" -> truculentaFont
        "honk" -> honkFont
        else -> systemFont
    }

    LaunchedEffect(verses) {
        if (!verses.isNullOrEmpty()) {
            val isSaved = checkIfBookmarked(verses.first(), databaseHelper)
            isBookmarked = isSaved
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
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
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = currentFontFamily
                )
                IconButton(
                    onClick = {
                        isLoading.value = true
                        onRefresh()
                        Handler(Looper.getMainLooper()).postDelayed({
                            isLoading.value = false
                        }, 500)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!verses.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(verses) }
                ) {
                    Column {
                        val reference = SimpleVerseProcessor.extractVerseReference(verses)
                        Text(
                            text = reference,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontFamily = currentFontFamily
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        verses.forEach { verse ->
                            val annotatedText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                ) {
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
                                fontFamily = currentFontFamily
                            )
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        verses?.let { verseList ->
                            if (isBookmarked) {
                                removeFromBookmarks(verseList, databaseHelper)
                            } else {
                                saveToBookmarks(verseList, databaseHelper)
                            }
                            isBookmarked = !isBookmarked
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Share",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            verses?.let {
                                val shareText = buildString {
                                    it.forEach { verse ->
                                        val cleanedText = SimpleVerseProcessor.stripXmlTags(verse.text)
                                        append("${verse.bookName ?: ""} ${verse.chapter ?: 0}:${verse.verseNumber} $cleanedText\n")
                                    }
                                }
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share verses via"))
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun PopularDevotionalsSection(
    devotionals: List<PopularDevotional>,
    onNavigateToReader: (PassageSelection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Popular Devotionals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        devotionals.forEachIndexed { index, devotional ->
            DevotionalItem(
                devotional = devotional,
                onNavigateToReader = onNavigateToReader
            )

            if (index < devotionals.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
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
                val passage = PassageSelection(
                    bookNumber = bookNumber,
                    bookName = devotional.bookName,
                    chapter = devotional.chapter,
                    verse = devotional.verse
                )
                onNavigateToReader(passage)
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
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = devotional.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = devotional.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = {}) {
            Icon(
                Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun loadRandomVerses(
    context: Context,
    databaseHelper: DatabaseHelper?,
    onComplete: (List<Verse>) -> Unit
) {
    if (databaseHelper != null) {
        Thread {
            val verses = databaseHelper.getRandomVerses()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    } else {
        Thread {
            val dbHelper = DatabaseHelper(
                context as MainActivity,
                databaseName = "kj2.sqlite3"
            )
            val verses = dbHelper.getRandomVerses()
            dbHelper.close()
            Handler(Looper.getMainLooper()).post {
                onComplete(verses)
            }
        }.start()
    }
}

fun getRandomDevotionals(): List<PopularDevotional> {
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

private fun saveToBookmarks(verses: List<Verse>, databaseHelper: DatabaseHelper) {
    Thread {
        verses.forEach { verse ->
            databaseHelper.addBookmark(verse)
        }
    }.start()
}

private fun removeFromBookmarks(verses: List<Verse>, databaseHelper: DatabaseHelper) {
    Thread {
        verses.forEach { verse ->
            databaseHelper.removeBookmark(verse)
        }
    }.start()
}

private fun checkIfBookmarked(verse: Verse, databaseHelper: DatabaseHelper): Boolean {
    return databaseHelper.isBookmarked(verse)
}