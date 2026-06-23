package com.fountofhopedotorg.fohbible.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.footer.Footer
import com.fountofhopedotorg.fohbible.app_composables.ImageSection
import com.fountofhopedotorg.fohbible.app_composables.FountRain
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.PopularDevotional
import com.fountofhopedotorg.fohbible.data.QuickAction
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.SimpleVerseProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

object BookmarkHelper {
    suspend fun isBookmarked(verse: Verse, dbHelper: DatabaseHelper): Boolean =
        withContext(Dispatchers.IO) { dbHelper.isBookmarked(verse) }

    suspend fun addBookmarks(verses: List<Verse>, dbHelper: DatabaseHelper) =
        withContext(Dispatchers.IO) { verses.forEach { dbHelper.addBookmark(it) } }

    suspend fun removeBookmarks(verses: List<Verse>, dbHelper: DatabaseHelper) =
        withContext(Dispatchers.IO) { verses.forEach { dbHelper.removeBookmark(it) } }
}

object VerseShareHelper {
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
            context as MainActivity,
            databaseName = "kj2.sqlite3"
        )
        val verses = helper.getRandomVerses()
        if (dbHelper == null) helper.close()
        verses
    }

class RandomImageSelector(private val viewModel: AppViewModel) {
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
    databaseHelper: DatabaseHelper? = null,
    onCreateSermonMaterialsClick: () -> Unit = {},
    onTakeBibleQuizClick: () -> Unit,
    onLearnHebrewClick: () -> Unit,
    onLearnGreekClick: () -> Unit,
    onOpenDictionaryClick: () -> Unit = {},
    onOpenVideoEditorClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<AppViewModel>()

    val uniqueQuickActionImages = remember {
        val totalImages = viewModel.textures
        totalImages.shuffled().take(5)
    }

    var dailyVerses by remember { mutableStateOf<List<Verse>?>(null) }
    var popularDevotionals by remember { mutableStateOf<List<PopularDevotional>>(emptyList()) }

    LaunchedEffect(Unit) {
        dailyVerses = loadRandomVerses(context, databaseHelper)
    }

    LaunchedEffect(Unit) {
        popularDevotionals = getRandomDevotionals()
    }

    val quickActions = listOf(
        QuickAction(
            title = "Reader",
            icon = Icons.Filled.Book,
            color = MaterialTheme.colorScheme.primary,
            backgroundImage = getAssetImagePath(uniqueQuickActionImages[0]),
            onClick = { onNavigateToScreen(Screen.Reader()) }
        ),
        QuickAction(
            title = "Bookmarks",
            icon = Icons.Filled.Bookmark,
            color = MaterialTheme.colorScheme.primary,
            backgroundImage = getAssetImagePath(uniqueQuickActionImages[1]),
            onClick = { onNavigateToScreen(Screen.Bookmarks) }
        ),
        QuickAction(
            title = "Notes",
            icon = Icons.AutoMirrored.Filled.Note,
            color = MaterialTheme.colorScheme.primary,
            backgroundImage = getAssetImagePath(uniqueQuickActionImages[2]),
            onClick = { onNavigateToScreen(Screen.Notes) }
        ),
        QuickAction(
            title = "Search",
            icon = Icons.Filled.Search,
            color = MaterialTheme.colorScheme.primary,
            backgroundImage = getAssetImagePath(uniqueQuickActionImages[3]),
            onClick = { onNavigateToScreen(Screen.Search) }
        ),
        QuickAction(
            title = "Settings",
            icon = Icons.Filled.Settings,
            color = MaterialTheme.colorScheme.primary,
            backgroundImage = getAssetImagePath(uniqueQuickActionImages[4]),
            onClick = { onNavigateToScreen(Screen.Settings) }
        )
    )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item { HomeHeader() }

        item {  ImageSection(
            onNavigateToReader = onNavigateToReader,
            databaseHelper = DatabaseHelper(context, viewModel.currentDbName)
        ) }

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
                    context as MainActivity,
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
        item {
            Spacer(Modifier.height(16.dp))
            UsefulSpaceGrid(
                onCreateSermonMaterialsClick = onCreateSermonMaterialsClick,
                onTakeBibleQuizClick = onTakeBibleQuizClick,
                onLearnHebrewClick = onLearnHebrewClick,
                onLearnGreekClick = onLearnGreekClick,
                onOpenDictionaryClick = onOpenDictionaryClick,
                onOpenVideoEditorClick = onOpenVideoEditorClick
            )
        }

        item { Spacer(Modifier.height(17.dp)) }

        item {
            val isMatrixVisible by remember { mutableStateOf(true) }
            if (isMatrixVisible) {
                key("matrix_component") { FountRain() }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
        item { Footer() }
        item { Spacer(Modifier.height(20.dp)) }
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

private fun getAssetImagePath(fileName: String): String {
    return "file:///android_asset/textures/$fileName"
}