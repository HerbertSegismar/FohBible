package com.fountofhopedotorg.fohbible.core

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import com.fountofhopedotorg.fohbible.data.BibleBook
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.ProcessedVerse
import com.fountofhopedotorg.fohbible.data.SelectedWord
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.VerseContent
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading verses...")
    }
}

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
    scrollState: ScrollState? = null,
    lazyState: LazyListState? = null,
    onInitialScrollComplete: () -> Unit = {},
    onWordPress: ((String, Boolean) -> Unit)? = null,
    onStrongsPress: ((String, Int, Boolean) -> Unit)? = null,
    onTagPress: ((String, Int, Int, Int, Boolean) -> Unit)? = null,
    onVerseLongPress: ((Verse, PassageSelection) -> Unit)? = null,
    databaseHelper: DatabaseHelper? = null,
    crossRefHelper: DatabaseHelper? = null,
    onCrossRefClick: ((Int, Int, Int, Boolean) -> Unit)? = null,
    refreshKey: Int = 0,
    onVerseCommentaryClick: ((Int, Int, Int) -> Unit)? = null,
    markerColor: Color,
    onWordHighlightAction: (() -> Unit)? = null
) {
    val useLazy = lazyState != null
    require((useLazy && scrollState == null) || (!useLazy && scrollState != null)) {
        "Provide exactly one of scrollState (for non-lazy Column) or lazyState (for LazyColumn)"
    }

    val isOldTestamentForThisVersion = if (isPrimary) viewModel.isOldTestament else viewModel.isSecondaryOldTestament

    val processedVerses: Map<Int, ProcessedVerse> = if (useLazy) {
        produceState(
            initialValue = emptyMap(),
            content,
            viewModel.fontSize,
            themeColors,
            refreshKey,
            isOldTestamentForThisVersion
        ) {
            value = withContext(Dispatchers.Default) {
                val result = mutableMapOf<Int, ProcessedVerse>()
                val processor = VerseTextProcessor()
                content.forEach { item ->
                    if (item is VerseContent.VerseVal) {
                        val verse = item.verse
                        val fullVerse = verse.copy(bookName = passage.bookName, chapter = passage.chapter)
                        val isPersistentHighlighted = databaseHelper?.isHighlighted(fullVerse) ?: false
                        val onStrongsLocal: ((String) -> Unit)? = onStrongsPress?.let { callback ->
                            { strong -> callback(strong, passage.bookNumber, isPrimary) }
                        }
                        val onTagLocal: ((String) -> Unit)? = onTagPress?.let { callback ->
                            { marker -> callback(marker, passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary) }
                        }
                        val onWordLocal: ((String) -> Unit)? = onWordPress?.let { callback ->
                            { word -> callback(word, isPrimary) }
                        }
                        val processed = processor.processVerse(
                            verseText = verse.text,
                            baseFontSize = viewModel.fontSize.sp,
                            themeColors = themeColors,
                            textColor = themeColors.textColor,
                            onTagPress = onTagLocal,
                            onWordPress = onWordLocal,
                            onStrongsPress = onStrongsLocal,
                            isHighlighted = isPersistentHighlighted,
                            isOldTestament = isOldTestamentForThisVersion
                        )
                        result[verse.verseNumber] = processed
                    }
                }
                result
            }
        }.value
    } else {
        remember(
            content,
            viewModel.fontSize,
            themeColors,
            refreshKey,
            isOldTestamentForThisVersion
        ) {
            val result = mutableMapOf<Int, ProcessedVerse>()
            val processor = VerseTextProcessor()
            content.forEach { item ->
                if (item is VerseContent.VerseVal) {
                    val verse = item.verse
                    val fullVerse = verse.copy(bookName = passage.bookName, chapter = passage.chapter)
                    val isPersistentHighlighted = databaseHelper?.isHighlighted(fullVerse) ?: false
                    val onStrongsLocal: ((String) -> Unit)? = onStrongsPress?.let { callback ->
                        { strong -> callback(strong, passage.bookNumber, isPrimary) }
                    }
                    val onTagLocal: ((String) -> Unit)? = onTagPress?.let { callback ->
                        { marker -> callback(marker, passage.bookNumber, passage.chapter, verse.verseNumber, isPrimary) }
                    }
                    val onWordLocal: ((String) -> Unit)? = onWordPress?.let { callback ->
                        { word -> callback(word, isPrimary) }
                    }
                    val processed = processor.processVerse(
                        verseText = verse.text,
                        baseFontSize = viewModel.fontSize.sp,
                        themeColors = themeColors,
                        textColor = themeColors.textColor,
                        onTagPress = onTagLocal,
                        onWordPress = onWordLocal,
                        onStrongsPress = onStrongsLocal,
                        isHighlighted = isPersistentHighlighted,
                        isOldTestament = isOldTestamentForThisVersion
                    )
                    result[verse.verseNumber] = processed
                }
            }
            result
        }
    }

    var highlightedVerse by remember { mutableStateOf<Int?>(null) }
    val offsets = if (!useLazy) remember { mutableStateMapOf<Int, Float>() } else null

    val bookmarkIconSize = viewModel.fontSize
    val bookmarkInlineContent = InlineTextContent(
        Placeholder(
            width = bookmarkIconSize.sp,
            height = bookmarkIconSize.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = "Bookmarked",
            tint = themeColors.verseNumber,
            modifier = Modifier.fillMaxSize()
        )
    }
    val noteInlineContent = InlineTextContent(
        Placeholder(
            width = bookmarkIconSize.sp,
            height = bookmarkIconSize.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Note,
            contentDescription = "Has Note",
            tint = themeColors.verseNumber,
            modifier = Modifier.fillMaxSize().rotate(90f)
        )
    }

    val crossRefCounts = remember { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(passage.bookNumber, passage.chapter, crossRefHelper) {
        val counts = crossRefHelper?.getCrossReferenceCountsForChapter(passage.bookNumber, passage.chapter) ?: emptyMap()
        crossRefCounts.clear()
        crossRefCounts.putAll(counts)
    }

    var selectedWords by remember(
        passage.bookNumber,
        passage.chapter,
        refreshKey,
        databaseHelper?.databaseName
    ) {
        mutableStateOf(
            buildSet {
                if (databaseHelper != null) {
                    content.forEach { item ->
                        if (item is VerseContent.VerseVal) {
                            val fullVerse = item.verse.copy(bookName = passage.bookName, chapter = passage.chapter)
                            val highlights = databaseHelper.getWordHighlightsForVerse(fullVerse)
                            addAll(highlights)
                        }
                    }
                }
            }
        )
    }
    val groupedHighlights = remember(selectedWords) { selectedWords.groupBy { it.verseNumber } }
    @Composable
    fun VerseItem(verseContent: VerseContent.VerseVal) {
        val verse = verseContent.verse
        val processedVerse = processedVerses[verse.verseNumber]
        val isTemporaryHighlighted = verse.verseNumber == highlightedVerse
        val fullVerse = verse.copy(bookName = passage.bookName, chapter = passage.chapter)
        val isPersistentHighlighted = databaseHelper?.isHighlighted(fullVerse) ?: false
        val persistentHighlightColor = if (isPersistentHighlighted) {
            databaseHelper.getHighlightColor(fullVerse) ?: themeColors.searchHighlightBg
        } else null
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .then(backgroundModifier)
                    .let {
                        if (!useLazy && offsets != null) {
                            it.onGloballyPositioned { coords ->
                                offsets[verse.verseNumber] = coords.positionInParent().y
                            }
                        } else it
                    }
            ) {
                processedVerse?.header?.let { header ->
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
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = themeColors.verseNumber,
                            fontSize = bookmarkIconSize.sp * 0.8f
                        )
                    ) {
                        append("${verse.verseNumber} ")
                    }
                    append(processedVerse?.body ?: verse.text)
                    if (isBookmarked) appendInlineContent("bookmark", "[bookmark]")
                    if (isNote) appendInlineContent("note", "[note]")
                    if (viewModel.isStudyMode) {
                        if (refCount > 0 && onCrossRefClick != null) {
                            append(" ")
                            appendInlineContent("crossref_${verse.verseNumber}", "[$refCount]")
                        }
                        if (onVerseCommentaryClick != null) {
                            appendInlineContent("commentary_${verse.verseNumber}", "[C]")
                        }
                    }
                }

                val inlineContentMap = remember(
                    verse.verseNumber,
                    refCount,
                    isNote,
                    isBookmarked,
                    viewModel.fontSize,
                    themeColors
                ) {
                    buildMap {
                        if (isBookmarked) put("bookmark", bookmarkInlineContent)
                        if (isNote) put("note", noteInlineContent)
                        if (viewModel.isStudyMode) {
                            if (refCount > 0 && onCrossRefClick != null) {
                                put("crossref_${verse.verseNumber}", InlineTextContent(
                                    Placeholder(
                                        width = (viewModel.fontSize * 1.4).sp,
                                        height = (viewModel.fontSize).sp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clickable {
                                                onCrossRefClick(
                                                    passage.bookNumber,
                                                    passage.chapter,
                                                    verse.verseNumber,
                                                    isPrimary
                                                )
                                            }
                                            .background(
                                                themeColors.primary.copy(alpha = 0.15f),
                                                RoundedCornerShape(2.dp)
                                            )
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$refCount",
                                            fontSize = (viewModel.fontSize * 0.7f).sp,
                                            color = themeColors.verseNumber,
                                            fontWeight = FontWeight.Bold,
                                            style = TextStyle(lineHeight = (viewModel.fontSize).sp)
                                        )
                                    }
                                })
                            }
                            if (onVerseCommentaryClick != null) {
                                put("commentary_${verse.verseNumber}", InlineTextContent(
                                    Placeholder(
                                        width = (viewModel.fontSize * 1.4f).sp,
                                        height = (viewModel.fontSize).sp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                onVerseCommentaryClick(
                                                    passage.bookNumber,
                                                    passage.chapter,
                                                    verse.verseNumber
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "*",
                                            fontSize = (viewModel.fontSize * 1.2f).sp,
                                            color = themeColors.verseNumber,
                                            fontWeight = FontWeight.Bold,
                                            style = TextStyle(lineHeight = (viewModel.fontSize).sp)
                                        )
                                    }
                                })
                            }
                        }
                    }
                }

                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                val finalAnnotatedString = remember(annotatedString, wordHighlightsForVerse) {
                    if (wordHighlightsForVerse.isEmpty()) {
                        annotatedString
                    } else {
                        val builder = AnnotatedString.Builder()
                        builder.append(annotatedString)
                        wordHighlightsForVerse.sortedByDescending { it.start }.forEach { word ->
                            if (word.start in 0 until builder.length && word.end in word.start..builder.length) {
                                builder.addStyle(
                                    SpanStyle(background = word.color),
                                    word.start,
                                    word.end
                                )
                            }
                        }
                        builder.toAnnotatedString()
                    }
                }

                Text(
                    text = finalAnnotatedString,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(viewModel.isDictionaryMode) {
                            detectTapGestures(
                                onTap = { offset ->
                                    textLayoutResult?.let { layout ->
                                        val position = layout.getOffsetForPosition(offset)
                                        val annotations = finalAnnotatedString.getStringAnnotations(
                                            start = position,
                                            end = position
                                        )
                                        val wordAnnotation = annotations.find { it.tag == "word" }
                                        if (wordAnnotation != null) {
                                            if (viewModel.isDictionaryMode) {
                                                onWordPress?.invoke(wordAnnotation.item, isPrimary)
                                            } else {
                                                val word = SelectedWord(
                                                    verse.verseNumber,
                                                    wordAnnotation.start,
                                                    wordAnnotation.end,
                                                    currentMarkerColor
                                                )
                                                val wasSelected = selectedWords.contains(word)
                                                selectedWords = if (wasSelected) {
                                                    selectedWords - word
                                                } else {
                                                    selectedWords + word
                                                }
                                                if (databaseHelper != null) {
                                                    if (wasSelected) {
                                                        databaseHelper.removeWordHighlight(fullVerse, word.start, word.end)
                                                    } else {
                                                        databaseHelper.addWordHighlight(
                                                            fullVerse,
                                                            word.start,
                                                            word.end,
                                                            currentMarkerColor.toArgb()
                                                        )
                                                    }
                                                }
                                                onWordHighlightAction?.invoke()
                                            }
                                        } else {
                                            annotations.forEach { annotation ->
                                                when (annotation.tag) {
                                                    "strong" -> onStrongsPress?.invoke(
                                                        annotation.item,
                                                        passage.bookNumber,
                                                        isPrimary
                                                    )
                                                    "tag" -> onTagPress?.invoke(
                                                        annotation.item,
                                                        passage.bookNumber,
                                                        passage.chapter,
                                                        verse.verseNumber,
                                                        isPrimary
                                                    )
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
        val texture = when (viewModel.bgImageIndex) {
            0 -> null
            34 -> if (viewModel.customTextureUri != null) viewModel.customTextureUri else null
            else -> "file:///android_asset/textures/${viewModel.bgImageIndex}.jpg"
        }
        if (texture != null) {
            AsyncImage(
                model = texture,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            val overlayColor = (if (viewModel.darkTheme) viewModel.darkOverlayColor else viewModel.lightOverlayColor)
                .copy(alpha = viewModel.overlayOpacity)
            Box(modifier = Modifier.fillMaxSize().background(overlayColor))
        }

        Column {
            if (viewModel.multiVersion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.primary)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (viewModel.scrollSync || isPrimary) {
                                viewModel.showNavigationModal = true
                            } else {
                                viewModel.showSecondaryNavigationModal = true
                            }
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
                                color = Color.White,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = passage.chapter.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
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
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            if (useLazy) {
                LazyColumn(
                    state = lazyState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    items(
                        items = content,
                        key = { item ->
                            when (item) {
                                is VerseContent.SubheadingVal -> "sub_${item.subheading.text.hashCode()}"
                                is VerseContent.VerseVal -> "verse_${item.verse.verseNumber}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is VerseContent.SubheadingVal -> {
                                Text(
                                    text = item.subheading.text,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.primary,
                                    fontSize = (viewModel.fontSize + 1).sp,
                                    lineHeight = ((viewModel.fontSize + 1) * 1.2f).sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    textAlign = TextAlign.Left,
                                    fontFamily = currentFontFamily
                                )
                            }
                            is VerseContent.VerseVal -> {
                                VerseItem(item)
                            }
                        }
                    }
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState!!)
                        .padding(horizontal = 4.dp)
                ) {
                    if (processedVerses.isEmpty() && content.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                        return@Column
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    content.forEach { item ->
                        when (item) {
                            is VerseContent.SubheadingVal -> {
                                Text(
                                    text = item.subheading.text,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.primary,
                                    fontSize = (viewModel.fontSize + 1).sp,
                                    lineHeight = ((viewModel.fontSize + 1) * 1.2f).sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    textAlign = TextAlign.Left,
                                    fontFamily = currentFontFamily
                                )
                            }
                            is VerseContent.VerseVal -> {
                                VerseItem(item)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
    if (isCurrentPage) {
        LaunchedEffect(targetVerse, content) {
            if (targetVerse != null && content.isNotEmpty() && targetVerse > 0) {
                if (useLazy) {
                    delay(100)
                    val targetIndex = content.indexOfFirst { it is VerseContent.VerseVal && it.verse.verseNumber == targetVerse }
                    if (targetIndex >= 0) {
                        lazyState.animateScrollToItem(targetIndex)
                        highlightedVerse = targetVerse
                        delay(2000)
                        highlightedVerse = null
                    }
                } else {
                    delay(200)
                    val offset = offsets?.get(targetVerse)
                    if (offset == null) {
                        onInitialScrollComplete()
                        return@LaunchedEffect
                    }
                    scrollState!!.animateScrollTo(offset.toInt())
                    highlightedVerse = targetVerse
                    delay(2000)
                    highlightedVerse = null
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