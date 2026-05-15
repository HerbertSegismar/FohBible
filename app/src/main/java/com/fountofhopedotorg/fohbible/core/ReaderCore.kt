package com.fountofhopedotorg.fohbible.core

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.data.BibleBook
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.ProcessingOptions
import com.fountofhopedotorg.fohbible.data.SelectedWord
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.VerseContent
import com.fountofhopedotorg.fohbible.data.getVersesWithSubheadings
import com.fountofhopedotorg.fohbible.functions.ColorSplashCanvas
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.composables.FloatingOrbsBackground
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ChapterView(
    passage: PassageSelection,
    content: List<VerseContent>,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    viewModel: AppViewModel,
    isCurrentPage: Boolean,
    targetVerse: Int?,
    versionAbbr: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    lazyState: LazyListState,
    onInitialScrollComplete: () -> Unit = {},
    onWordPress: ((String) -> Unit)? = null,
    onStrongsPress: ((String, Int, Boolean) -> Unit)? = null,
    onTagPress: ((String, Int, Int, Int, Boolean) -> Unit)? = null,
    onVerseLongPress: ((Verse, PassageSelection) -> Unit)? = null,
    databaseHelper: DatabaseHelper? = null,
    crossRefHelper: DatabaseHelper? = null,
    onCrossRefClick: ((Int, Int, Int, Boolean) -> Unit)? = null,
    refreshKey: Int = 0,
    onVerseCommentaryClick: ((Int, Int, Int) -> Unit)? = null,
    markerColor: Color,
    onWordHighlightAction: (() -> Unit)? = null,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScreenChange: (Screen) -> Unit,
    scrollSyncEnabled: Boolean
) {
    val readerFontColor = if (viewModel.darkTheme)
        viewModel.darkThemeReaderFontColor
    else
        viewModel.lightThemeReaderFontColor
    val verseProcessor = remember(databaseHelper?.databaseName) { VerseTextProcessor() }
    val isOldTestamentForThisVersion = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament
    var highlightedVerse by remember { mutableStateOf<Int?>(null) }

    val bookmarkIconSize = viewModel.fontSize
    val bookmarkInlineContent = InlineTextContent(
        Placeholder(bookmarkIconSize.sp, bookmarkIconSize.sp, PlaceholderVerticalAlign.TextCenter)
    ) {
        Icon(Icons.Default.Bookmark, "Bookmarked", tint = themeColors.verseNumber, modifier = Modifier.fillMaxSize())
    }
    val noteInlineContent = InlineTextContent(
        Placeholder(bookmarkIconSize.sp, bookmarkIconSize.sp, PlaceholderVerticalAlign.TextCenter)
    ) {
        Icon(Icons.AutoMirrored.Filled.Note, "Has Note", tint = themeColors.verseNumber, modifier = Modifier.fillMaxSize().rotate(90f))
    }

    val crossRefCounts = remember { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(passage.bookNumber, passage.chapter, crossRefHelper) {
        val counts = crossRefHelper?.getCrossReferenceCountsForChapter(passage.bookNumber, passage.chapter) ?: emptyMap()
        crossRefCounts.clear()
        crossRefCounts.putAll(counts)
    }

    var selectedWords by remember(passage.bookNumber, passage.chapter, refreshKey, databaseHelper?.databaseName) {
        mutableStateOf(buildSet {
            if (databaseHelper != null) content.forEach { item ->
                if (item is VerseContent.VerseVal) {
                    val fullVerse = item.verse.copy(bookName = passage.bookName, chapter = passage.chapter)
                    addAll(databaseHelper.getWordHighlightsForVerse(fullVerse))
                }
            }
        })
    }
    val groupedHighlights = remember(selectedWords) { selectedWords.groupBy { it.verseNumber } }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSingleView = !viewModel.multiVersion
    val isPortraitHorizontalMulti = !isLandscape && viewModel.multiVersion && viewModel.multiViewLayout == "horizontal"
    val isPortraitVerticalMulti = !isLandscape && viewModel.multiVersion && viewModel.multiViewLayout == "vertical"
    val shouldShowFullHeader = (isPortraitVerticalMulti || (isLandscape && !isSingleView))

    LaunchedEffect(isLandscape, viewModel.multiVersion, viewModel.squareAspectViews) {
        if (viewModel.multiVersion && viewModel.squareAspectViews) {
            viewModel.multiViewLayout = if (isLandscape) "horizontal" else "vertical"
        }
    }

    @Composable
    fun VerseItem(verseContent: VerseContent.VerseVal) {
        val verse = verseContent.verse
        val fullVerse = verse.copy(bookName = passage.bookName, chapter = passage.chapter)
        val isPersistentHighlighted = databaseHelper?.isHighlighted(fullVerse) ?: false
        val persistentHighlightColor = if (isPersistentHighlighted) databaseHelper.getHighlightColor(fullVerse) ?: themeColors.searchHighlightBg else null
        val processingOptions = remember(viewModel.showStrongs) {
            ProcessingOptions(showStrongs = viewModel.showStrongs)
        }

        val processedVerse = remember(verse.text, viewModel.fontSize, themeColors, isPersistentHighlighted, isOldTestamentForThisVersion, refreshKey, viewModel.showStrongs, verseProcessor) {
            val onStrongsLocal: ((String) -> Unit)? = onStrongsPress?.let { { strong -> it(strong, passage.bookNumber, isPrimary) } }
            val onTagLocal: ((String) -> Unit)? = onTagPress?.let { { marker -> it(marker, passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary) } }
            val onWordLocal: ((String) -> Unit)? = onWordPress?.let { { word -> it(word) } }
            verseProcessor.processVerse(
                verseText = verse.text,
                baseFontSize = viewModel.fontSize.sp,
                themeColors = themeColors,
                textColor = readerFontColor,
                onTagPress = onTagLocal,
                onWordPress = onWordLocal,
                onStrongsPress = onStrongsLocal,
                isHighlighted = isPersistentHighlighted,
                isOldTestament = isOldTestamentForThisVersion,
                options = processingOptions
            )
        }

        val isTemporaryHighlighted = verse.verseNumber == highlightedVerse
        val isBookmarked = databaseHelper?.isBookmarked(fullVerse) ?: false
        val isNote = databaseHelper?.hasNote(fullVerse) ?: false
        val refCount = crossRefCounts[verse.verseNumber] ?: 0

        val backgroundModifier = when {
            persistentHighlightColor != null -> Modifier.background(persistentHighlightColor)
            isTemporaryHighlighted -> Modifier.background(themeColors.searchHighlightBg)
            else -> Modifier
        }

        val currentMarkerColor by rememberUpdatedState(markerColor)
        val wordHighlightsForVerse = groupedHighlights[verse.verseNumber] ?: emptyList()
        val highlightsHash = wordHighlightsForVerse.hashCode()

        key("verse_${verse.verseNumber}_${isPersistentHighlighted}_${isBookmarked}_${isNote}_$highlightsHash") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).then(backgroundModifier)) {
                processedVerse.header?.let { header ->
                    if (header.text.isNotEmpty()) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.tagColor,
                            modifier = Modifier.padding(bottom = 4.dp),
                            fontFamily = currentFontFamily
                        )
                    }
                }

                val annotatedString = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = themeColors.verseNumber, fontSize = bookmarkIconSize.sp * 0.8f)) {
                        append("${verse.verseNumber} ")
                    }
                    append(processedVerse.body)
                    if (isBookmarked) appendInlineContent("bookmark", "[bookmark]")
                    if (isNote) appendInlineContent("note", "[note]")
                    if (viewModel.isStudyMode) {
                        if (refCount > 0 && onCrossRefClick != null) appendInlineContent("crossref_${verse.verseNumber}", "[$refCount]")
                        if (onVerseCommentaryClick != null) appendInlineContent("commentary_${verse.verseNumber}", "[C]")
                    }
                }

                val inlineContentMap = remember(verse.verseNumber, refCount, isNote, isBookmarked, viewModel.fontSize, themeColors) {
                    buildMap {
                        if (isBookmarked) put("bookmark", bookmarkInlineContent)
                        if (isNote) put("note", noteInlineContent)
                        if (viewModel.isStudyMode) {
                            if (refCount > 0 && onCrossRefClick != null) put("crossref_${verse.verseNumber}", InlineTextContent(
                                Placeholder((viewModel.fontSize * 1.4).sp, viewModel.fontSize.sp, PlaceholderVerticalAlign.TextCenter)
                            ) {
                                Box(
                                    modifier = Modifier.clickable {
                                        onCrossRefClick(passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary)
                                    }.background(themeColors.primary.copy(alpha = 0.15f), RoundedCornerShape(2.dp)).fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$refCount", fontSize = (viewModel.fontSize * 0.7f).sp, color = themeColors.verseNumber, fontWeight = FontWeight.Bold, style = TextStyle(
                                        lineHeight = (viewModel.fontSize).sp)
                                    )
                                }
                            })
                            if (onVerseCommentaryClick != null) put("commentary_${verse.verseNumber}", InlineTextContent(
                                Placeholder((viewModel.fontSize * 1.4f).sp, viewModel.fontSize.sp, PlaceholderVerticalAlign.TextCenter)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().clickable {
                                        onVerseCommentaryClick(passage.bookNumber, passage.chapter, verse.verseNumber)
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("*", fontSize = (viewModel.fontSize * 1.2f).sp, color = themeColors.verseNumber, fontWeight = FontWeight.Bold, style = TextStyle(lineHeight = (viewModel.fontSize * 1.4f).sp))
                                }
                            })
                        }
                    }
                }

                val finalAnnotatedString = remember(annotatedString, wordHighlightsForVerse) {
                    if (wordHighlightsForVerse.isEmpty()) annotatedString else {
                        val builder = AnnotatedString.Builder().apply { append(annotatedString) }
                        wordHighlightsForVerse.sortedByDescending { it.start }.forEach { word ->
                            if (word.start in 0 until builder.length && word.end in word.start..builder.length) {
                                builder.addStyle(SpanStyle(background = word.color), word.start, word.end)
                            }
                        }
                        builder.toAnnotatedString()
                    }
                }

                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                Text(
                    text = finalAnnotatedString,
                    modifier = Modifier.fillMaxWidth().pointerInput(viewModel.isDictionaryMode) {
                        detectTapGestures(
                            onTap = { offset ->
                                textLayoutResult?.let { layout ->
                                    val position = layout.getOffsetForPosition(offset)
                                    val annotations = finalAnnotatedString.getStringAnnotations(start = position, end = position)
                                    val wordAnnotation = annotations.find { it.tag == "word" }
                                    if (wordAnnotation != null) {
                                        if (viewModel.isDictionaryMode) onWordPress?.invoke(wordAnnotation.item) else {
                                            val word = SelectedWord(verse.verseNumber, wordAnnotation.start, wordAnnotation.end, currentMarkerColor)
                                            val wasSelected = selectedWords.contains(word)
                                            selectedWords = if (wasSelected) selectedWords - word else selectedWords + word
                                            if (databaseHelper != null) {
                                                if (wasSelected) databaseHelper.removeWordHighlight(fullVerse, word.start, word.end)
                                                else databaseHelper.addWordHighlight(fullVerse, word.start, word.end, currentMarkerColor.toArgb())
                                            }
                                            onWordHighlightAction?.invoke()
                                        }
                                    } else {
                                        annotations.forEach { annotation ->
                                            when (annotation.tag) {
                                                "strong" -> onStrongsPress?.invoke(annotation.item, passage.bookNumber, isPrimary)
                                                "tag" -> onTagPress?.invoke(annotation.item, passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary)
                                            }
                                        }
                                    }
                                }
                            },
                            onLongPress = { onVerseLongPress?.invoke(verse, passage) }
                        )
                    },
                    fontSize = viewModel.fontSize.sp,
                    lineHeight = (viewModel.fontSize * 1.333f).sp,
                    fontFamily = currentFontFamily,
                    inlineContent = inlineContentMap,
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }
    }

    Box(modifier = modifier) {
        when (viewModel.bgImageIndex) {
            0 -> {  }
            34 -> ColorSplashCanvas()
            35 -> FloatingOrbsBackground(orbCount = viewModel.orbsCount)
            36 -> {
                val customUri = viewModel.customTextureUri
                if (customUri != null) {
                    AsyncImage(
                        model = if (customUri.startsWith("/")) "file://$customUri" else customUri,
                        contentDescription = "Custom background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            else -> {
                val texture = "file:///android_asset/textures/${viewModel.bgImageIndex}.jpg"
                AsyncImage(
                    model = texture,
                    contentDescription = "Texture background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        val overlayColor = (if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor)
            .copy(alpha = viewModel.overlayOpacity)
        if (viewModel.bgImageIndex != 0) {
            Box(modifier = Modifier.fillMaxSize().background(overlayColor))
        }

        Column {
            if (isPortraitHorizontalMulti) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(themeColors.primary).padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (scrollSyncEnabled || isPrimary) viewModel.showNavigationModal = true
                            else viewModel.showSecondaryNavigationModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(20.dp).weight(0.7f).padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = passage.bookName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = viewModel.headerButtonsColor,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = passage.chapter.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = viewModel.headerButtonsColor,
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (isPrimary) viewModel.showPrimaryVersionDropdown = true
                            else viewModel.showSecondaryVersionDropdown = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(20.dp).weight(0.5f)
                    ) {
                        Text(
                            text = versionAbbr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = viewModel.headerButtonsColor,
                            maxLines = 1
                        )
                    }
                }
            }

            if (shouldShowFullHeader) {
                ChapterHeader(
                    passage = passage,
                    versionAbbr = versionAbbr,
                    onBookChapterClick = {
                        if (scrollSyncEnabled || isPrimary) viewModel.showNavigationModal = true
                        else viewModel.showSecondaryNavigationModal = true
                    },
                    onVersionClick = {
                        if (isPrimary) viewModel.showPrimaryVersionDropdown = true
                        else viewModel.showSecondaryVersionDropdown = true
                    },
                    onThemeToggle = onThemeToggle,
                    onColorLensClick = onColorLensClick,
                    viewModel = viewModel,
                    onScreenChange = onScreenChange,
                    backgroundColor = themeColors.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LazyColumn(
                state = lazyState,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                items(
                    content,
                    key = { if (it is VerseContent.SubheadingVal) "sub_${it.subheading.text.hashCode()}" else "verse_${(it as VerseContent.VerseVal).verse.verseNumber}" }
                ) { item ->
                    when (item) {
                        is VerseContent.SubheadingVal -> Text(
                            text = item.subheading.text,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primary,
                            fontSize = (viewModel.fontSize + 1).sp,
                            lineHeight = ((viewModel.fontSize + 1) * 1.2f).sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            textAlign = TextAlign.Left,
                            fontFamily = currentFontFamily
                        )
                        is VerseContent.VerseVal -> VerseItem(item)
                    }
                }
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    if (isCurrentPage) {
        LaunchedEffect(targetVerse, content) {
            if (targetVerse != null && content.isNotEmpty() && targetVerse > 0) {
                delay(100)
                val targetIndex = content.indexOfFirst { it is VerseContent.VerseVal && it.verse.verseNumber == targetVerse }
                if (targetIndex >= 0) {
                    lazyState.scrollToItem(targetIndex)
                    if (!viewModel.scrollSyncAction) {
                        highlightedVerse = targetVerse
                        delay(2000)
                        highlightedVerse = null
                    }
                }
            }
            onInitialScrollComplete()
        }
    }
}

fun createCommentaryHelperIfExists(context: Context, baseDbName: String?): DatabaseHelper? {
    if (baseDbName.isNullOrEmpty()) return null
    val comName = baseDbName.replace(".sqlite3", "com.sqlite3")
    val assetPath = "databases/$comName"
    return try {
        context.assets.open(assetPath).use { }
        DatabaseHelper(context, comName)
    } catch (_: Exception) {
        null
    }
}

fun getPreviousChapter(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters == 1 && current.chapter == 1) return current
    return if (current.chapter == 1) {
        current.copy(chapter = currentBook.chapters, verse = null)
    } else {
        current.copy(chapter = current.chapter - 1, verse = null)
    }
}

fun getNextChapter(current: PassageSelection, currentBook: BibleBook?): PassageSelection {
    if (currentBook == null) return current
    if (currentBook.chapters == 1 && current.chapter == 1) return current
    return if (current.chapter == currentBook.chapters) {
        current.copy(chapter = 1, verse = null)
    } else {
        current.copy(chapter = current.chapter + 1, verse = null)
    }
}

suspend fun preloadChapter(
    passage: PassageSelection,
    loadedMap: MutableMap<Pair<Int, Int>, List<VerseContent>>,
    dbHelper: DatabaseHelper?,
    subheadingsDbHelper: DatabaseHelper
) {
    val key = passage.bookNumber to passage.chapter
    if (key !in loadedMap) {
        loadedMap[key] = withContext(Dispatchers.IO) {
            dbHelper?.let { getVersesWithSubheadings(it, subheadingsDbHelper, passage.bookNumber, passage.chapter) } ?: emptyList()
        }
    }
}