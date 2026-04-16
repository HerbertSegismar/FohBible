package com.fountofhopedotorg.fohbible.composables

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import com.fountofhopedotorg.fohbible.data.BibleData
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ModalPage
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.data.ProcessedVerse
import com.fountofhopedotorg.fohbible.data.Testament
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.functions.cleanDefinition
import com.fountofhopedotorg.fohbible.functions.fetchVerses
import com.fountofhopedotorg.fohbible.functions.getDefinitionOrClosest
import com.fountofhopedotorg.fohbible.functions.parseVerseLink
import com.fountofhopedotorg.fohbible.functions.prepareStrongContent
import com.fountofhopedotorg.fohbible.functions.sanitizeHtmlContent
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.Fonts
import com.fountofhopedotorg.fohbible.utils.InteractiveModalUtils.dictionaryDisplayNames
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun InteractiveModalDialog(
    isLandscape: Boolean,
    onDismiss: () -> Unit,
    currentPage: ModalPage,
    viewModel: AppViewModel,
    stack: SnapshotStateList<ModalPage>,
    scrollState: ScrollState,
    showEditWordDialog: MutableState<Boolean>,
    showModalColorWheel: MutableState<Boolean>,
    dictionaryDropdownExpanded: MutableState<Boolean>,
    commentaryDropdownExpanded: MutableState<Boolean>,
    crossRefDropdownExpanded: MutableState<Boolean>,
    dictionaries: List<String>,
    dictionaryDisplayNames: Map<String, String>,
    verseCommentaries: List<String>,
    verseCommentaryDisplayNames: Map<String, String>,
    crossReferenceDatabases: List<String>,
    crossReferenceDatabaseDisplayNames: Map<String, String>,
    switchToDictionary: (String) -> Unit,
    switchToVerseCommentary: (String) -> Unit,
    switchToCrossReference: (String) -> Unit,
    onNavigateToReader: (PassageSelection) -> Unit,
    onTagPress: (String, PassageSelection) -> Unit,
    onWordPress: (String) -> Unit,
    onStrongsPress: (String, Int) -> Unit,
    onCrossRefClick: (Int, Int?, Int, Boolean) -> Unit,
    onVerseCommentaryClick: (Int, Int?, Int, Boolean) -> Unit,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    databaseHelper: DatabaseHelper?,
    strongDbHelper: DatabaseHelper?,
    crossRefDbHelper: DatabaseHelper?,
    dictionaryDbHelper: DatabaseHelper?,
    lightModalColor: Color,
    darkModalColor: Color,
    modalBackgroundColor: Color,
    textColor: Color,
    linkColor: Color,
    isDark: Boolean,
    scope: CoroutineScope
) {
    AlertDialog(
        modifier = if (isLandscape) Modifier.fillMaxWidth(0.9f) else Modifier,
        properties = if (isLandscape) DialogProperties(usePlatformDefaultWidth = false) else DialogProperties(),
        onDismissRequest = onDismiss,
        title = {
            DialogTitle(
                currentPage = currentPage,
                viewModel = viewModel,
                showEditWordDialog = showEditWordDialog,
                showModalColorWheel = showModalColorWheel,
                dictionaryDropdownExpanded = dictionaryDropdownExpanded,
                commentaryDropdownExpanded = commentaryDropdownExpanded,
                crossRefDropdownExpanded = crossRefDropdownExpanded,
                dictionaries = dictionaries,
                dictionaryDisplayNames = dictionaryDisplayNames,
                verseCommentaries = verseCommentaries,
                verseCommentaryDisplayNames = verseCommentaryDisplayNames,
                crossReferenceDatabases = crossReferenceDatabases,
                crossReferenceDatabaseDisplayNames = crossReferenceDatabaseDisplayNames,
                switchToDictionary = switchToDictionary,
                switchToVerseCommentary = switchToVerseCommentary,
                switchToCrossReference = switchToCrossReference,
                onNavigateToReader = onNavigateToReader,
                onDismiss = onDismiss,
                lightModalColor = lightModalColor,
                darkModalColor = darkModalColor
            )
        },
        text = {
            DialogContent(
                currentPage = currentPage,
                viewModel = viewModel,
                stack = stack,
                scrollState = scrollState,
                themeColors = themeColors,
                currentFontFamily = currentFontFamily,
                databaseHelper = databaseHelper,
                strongDbHelper = strongDbHelper,
                crossRefDbHelper = crossRefDbHelper,
                dictionaryDbHelper = dictionaryDbHelper,
                onTagPress = onTagPress,
                onWordPress = onWordPress,
                onStrongsPress = onStrongsPress,
                onCrossRefClick = onCrossRefClick,
                onVerseCommentaryClick = onVerseCommentaryClick,
                textColor = textColor,
                linkColor = linkColor,
                isDark = isDark,
                scope = scope
            )
        },
        confirmButton = {
            DialogConfirmButtons(
                stack = stack,
                onDismiss = onDismiss
            )
        },
        dismissButton = {
            DialogDismissButton(
                currentPage = currentPage,
                viewModel = viewModel,
                dictionaries = dictionaries,
                verseCommentaries = verseCommentaries,
                crossReferenceDatabases = crossReferenceDatabases,
                switchToDictionary = switchToDictionary,
                switchToVerseCommentary = switchToVerseCommentary,
                switchToCrossReference = switchToCrossReference
            )
        },
        containerColor = modalBackgroundColor
    )
}

@Composable
private fun DialogTitle(
    currentPage: ModalPage,
    viewModel: AppViewModel,
    showEditWordDialog: MutableState<Boolean>,
    showModalColorWheel: MutableState<Boolean>,
    dictionaryDropdownExpanded: MutableState<Boolean>,
    commentaryDropdownExpanded: MutableState<Boolean>,
    crossRefDropdownExpanded: MutableState<Boolean>,
    dictionaries: List<String>,
    dictionaryDisplayNames: Map<String, String>,
    verseCommentaries: List<String>,
    verseCommentaryDisplayNames: Map<String, String>,
    crossReferenceDatabases: List<String>,
    crossReferenceDatabaseDisplayNames: Map<String, String>,
    switchToDictionary: (String) -> Unit,
    switchToVerseCommentary: (String) -> Unit,
    switchToCrossReference: (String) -> Unit,
    onNavigateToReader: (PassageSelection) -> Unit,
    onDismiss: () -> Unit,
    lightModalColor: Color,
    darkModalColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage.type == "definition" && currentPage.word != null) {
                val capitalizedWord = currentPage.word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
                val isTopical = viewModel.selectedDictionary == "topical"
                val prefix = when {
                    isTopical -> "References for "
                    currentPage.title.startsWith("Definition of ") -> "Definition of "
                    currentPage.title.startsWith("Closest match for ") -> "Closest match for "
                    currentPage.title.startsWith("Matches for ") -> "Matches for "
                    else -> "Definition of "
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = capitalizedWord,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { showEditWordDialog.value = true }
                    )
                }
            } else {
                Text(
                    text = currentPage.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (currentPage.type == "verses" && currentPage.passage != null) {
                IconButton(
                    onClick = {
                        onNavigateToReader(currentPage.passage.copy(verseEnd = null, chapterEnd = null))
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Read in Reader",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(lightModalColor, darkModalColor)
                        ),
                        shape = CircleShape
                    )
                    .border(0.2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { showModalColorWheel.value = true }
            )
        }

        currentPage.description?.let { description ->
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                DescriptionWithDropdown(
                    type = currentPage.type,
                    description = description,
                    viewModel = viewModel,
                    dictionaryDropdownExpanded = dictionaryDropdownExpanded,
                    commentaryDropdownExpanded = commentaryDropdownExpanded,
                    crossRefDropdownExpanded = crossRefDropdownExpanded,
                    dictionaries = dictionaries,
                    dictionaryDisplayNames = dictionaryDisplayNames,
                    verseCommentaries = verseCommentaries,
                    verseCommentaryDisplayNames = verseCommentaryDisplayNames,
                    crossReferenceDatabases = crossReferenceDatabases,
                    crossReferenceDatabaseDisplayNames = crossReferenceDatabaseDisplayNames,
                    switchToDictionary = switchToDictionary,
                    switchToVerseCommentary = switchToVerseCommentary,
                    switchToCrossReference = switchToCrossReference
                )
            }
        }
    }
}

@Composable
private fun DescriptionWithDropdown(
    type: String,
    description: String,
    viewModel: AppViewModel,
    dictionaryDropdownExpanded: MutableState<Boolean>,
    commentaryDropdownExpanded: MutableState<Boolean>,
    crossRefDropdownExpanded: MutableState<Boolean>,
    dictionaries: List<String>,
    dictionaryDisplayNames: Map<String, String>,
    verseCommentaries: List<String>,
    verseCommentaryDisplayNames: Map<String, String>,
    crossReferenceDatabases: List<String>,
    crossReferenceDatabaseDisplayNames: Map<String, String>,
    switchToDictionary: (String) -> Unit,
    switchToVerseCommentary: (String) -> Unit,
    switchToCrossReference: (String) -> Unit
) {
    when (type) {
        "definition" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(
                        onClick = { dictionaryDropdownExpanded.value = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Dictionary",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = dictionaryDropdownExpanded.value,
                        onDismissRequest = { dictionaryDropdownExpanded.value = false }
                    ) {
                        dictionaries.forEach { dictKey ->
                            DropdownMenuItem(
                                modifier = if (dictKey == viewModel.selectedDictionary) {
                                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                } else Modifier,
                                text = { Text(dictionaryDisplayNames[dictKey] ?: dictKey) },
                                onClick = {
                                    dictionaryDropdownExpanded.value = false
                                    switchToDictionary(dictKey)
                                }
                            )
                        }
                    }
                }
            }
        }
        "versecommentary" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(
                        onClick = { commentaryDropdownExpanded.value = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Commentary",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = commentaryDropdownExpanded.value,
                        onDismissRequest = { commentaryDropdownExpanded.value = false }
                    ) {
                        verseCommentaries.forEach { comKey ->
                            DropdownMenuItem(
                                modifier = if (comKey == viewModel.selectedVerseCommentary) {
                                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                } else Modifier,
                                text = { Text(verseCommentaryDisplayNames[comKey] ?: comKey) },
                                onClick = {
                                    commentaryDropdownExpanded.value = false
                                    switchToVerseCommentary(comKey)
                                }
                            )
                        }
                    }
                }
            }
        }
        "crossreference" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(
                        onClick = { crossRefDropdownExpanded.value = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowDropDown, "Select Cross-Reference DB")
                    }
                    DropdownMenu(
                        expanded = crossRefDropdownExpanded.value,
                        onDismissRequest = { crossRefDropdownExpanded.value = false }
                    ) {
                        crossReferenceDatabases.forEach { dbKey ->
                            DropdownMenuItem(
                                modifier = if (dbKey == viewModel.selectedCrossReferenceDatabase) {
                                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                } else Modifier,
                                text = { Text(crossReferenceDatabaseDisplayNames[dbKey] ?: dbKey) },
                                onClick = {
                                    crossRefDropdownExpanded.value = false
                                    switchToCrossReference(dbKey)
                                }
                            )
                        }
                    }
                }
            }
        }
        else -> {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DialogContent(
    currentPage: ModalPage,
    viewModel: AppViewModel,
    stack: SnapshotStateList<ModalPage>,
    scrollState: ScrollState,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    databaseHelper: DatabaseHelper?,
    strongDbHelper: DatabaseHelper?,
    crossRefDbHelper: DatabaseHelper?,
    dictionaryDbHelper: DatabaseHelper?,
    onTagPress: (String, PassageSelection) -> Unit,
    onWordPress: (String) -> Unit,
    onStrongsPress: (String, Int) -> Unit,
    onCrossRefClick: (Int, Int?, Int, Boolean) -> Unit,
    onVerseCommentaryClick: (Int, Int?, Int, Boolean) -> Unit,
    textColor: Color,
    linkColor: Color,
    isDark: Boolean,
    scope: CoroutineScope
) {
    if (currentPage.type == "verses") {
        VersesContent(
            currentPage = currentPage,
            viewModel = viewModel,
            themeColors = themeColors,
            currentFontFamily = currentFontFamily,
            scrollState = scrollState,
            crossRefDbHelper = crossRefDbHelper,
            onTagPress = onTagPress,
            onWordPress = onWordPress,
            onStrongsPress = onStrongsPress,
            onCrossRefClick = onCrossRefClick,
            onVerseCommentaryClick = onVerseCommentaryClick
        )
    } else {
        HtmlContent(
            currentPage = currentPage,
            viewModel = viewModel,
            stack = stack,
            scrollState = scrollState,
            databaseHelper = databaseHelper,
            strongDbHelper = strongDbHelper,
            dictionaryDbHelper = dictionaryDbHelper,
            textColor = textColor,
            linkColor = linkColor,
            isDark = isDark,
            scope = scope
        )
    }
}

@Composable
private fun VersesContent(
    currentPage: ModalPage,
    viewModel: AppViewModel,
    themeColors: ThemeColors,
    currentFontFamily: FontFamily,
    scrollState: ScrollState,
    crossRefDbHelper: DatabaseHelper?,
    onTagPress: (String, PassageSelection) -> Unit,
    onWordPress: (String) -> Unit,
    onStrongsPress: (String, Int) -> Unit,
    onCrossRefClick: (Int, Int?, Int, Boolean) -> Unit,
    onVerseCommentaryClick: (Int, Int?, Int, Boolean) -> Unit
) {
    val verses = currentPage.verses ?: emptyList()
    val passage = currentPage.passage ?: return

    val processor = remember(verses) { VerseTextProcessor() }
    val processedVerses = remember(verses, themeColors) {
        val result = mutableMapOf<Int, ProcessedVerse>()
        for (verse in verses) {
            val processed = processor.processVerse(
                verseText = verse.text,
                baseFontSize = (viewModel.fontSize * 0.85f).sp,
                themeColors = themeColors,
                textColor = themeColors.textColor,
                onTagPress = { marker -> onTagPress(marker, passage) },
                onWordPress = onWordPress,
                onStrongsPress = { strong -> onStrongsPress(strong, passage.bookNumber) },
                isHighlighted = false,
                isOldTestament = currentPage.isOldTestament
            )
            result[verse.verseNumber] = processed
        }
        result
    }

    val chapters = remember(verses) { verses.map { it.chapter }.distinct() }
    val crossRefCounts = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(chapters, crossRefDbHelper) {
        crossRefCounts.clear()
        chapters.forEach { chap ->
            val counts = withContext(Dispatchers.IO) {
                crossRefDbHelper?.getCrossReferenceCountsForChapter(passage.bookNumber, chap) ?: emptyMap()
            }
            crossRefCounts.putAll(counts)
        }
    }

    var currentBatch by remember(verses) { mutableIntStateOf(50) }
    val showChapterHeaders = remember(verses) { verses.mapNotNull { it.chapter }.distinct().size > 1 }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
        var lastChapter: Int? = null
        verses.take(currentBatch).forEach { verse ->
            if (showChapterHeaders && verse.chapter != lastChapter) {
                Text(
                    text = "Chapter ${verse.chapter}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = currentFontFamily
                )
                lastChapter = verse.chapter
            }

            val processedVerse = processedVerses[verse.verseNumber] ?: return@forEach
            val refCount = crossRefCounts[verse.verseNumber] ?: 0

            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = themeColors.verseNumber,
                        fontSize = (viewModel.fontSize * 0.85f * 0.778f).sp
                    )
                ) {
                    append(" ${verse.verseNumber} ")
                }
                append(processedVerse.body)
                if (refCount > 0) {
                    append(" ")
                    appendInlineContent("crossref_${verse.verseNumber}", "[$refCount]")
                }
                appendInlineContent("commentary_${verse.verseNumber}", "[C]")
            }

            val inlineContentMap = remember(verse.verseNumber, refCount) {
                buildMap {
                    if (refCount > 0) {
                        put("crossref_${verse.verseNumber}", InlineTextContent(
                            Placeholder(
                                width = (viewModel.fontSize * 1.2f).sp,
                                height = (viewModel.fontSize).sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        onCrossRefClick(
                                            passage.bookNumber,
                                            verse.chapter ?: passage.chapter,
                                            verse.verseNumber,
                                            currentPage.isOldTestament
                                        )
                                    }
                                    .background(themeColors.primary.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$refCount",
                                    fontSize = (viewModel.fontSize * 0.6f).sp,
                                    color = themeColors.verseNumber,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(lineHeight = (viewModel.fontSize).sp)
                                )
                            }
                        })
                    }
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
                                        verse.chapter ?: passage.chapter,
                                        verse.verseNumber,
                                        currentPage.isOldTestament
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "*",
                                fontSize = (viewModel.fontSize * 1.2f).sp,
                                color = themeColors.verseNumber,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(lineHeight = (viewModel.fontSize).sp),
                            )
                        }
                    })
                }
            }

            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            Column(modifier = Modifier.fillMaxWidth()) {
                processedVerse.header?.let { header ->
                    if (header.text.isNotEmpty()) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.tagColor,
                            fontFamily = currentFontFamily
                        )
                    }
                }
                Text(
                    text = annotatedString,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                textLayoutResult?.let { layout ->
                                    val position = layout.getOffsetForPosition(offset)
                                    val annotations = annotatedString.getStringAnnotations(
                                        start = position,
                                        end = position
                                    )
                                    annotations.forEach { annotation ->
                                        when (annotation.tag) {
                                            "word" -> onWordPress(annotation.item)
                                            "strong" -> onStrongsPress(annotation.item, passage.bookNumber)
                                            "tag" -> onTagPress(annotation.item, passage)
                                        }
                                    }
                                }
                            }
                        },
                    fontSize = (viewModel.fontSize * 0.85f).sp,
                    lineHeight = (viewModel.fontSize * 0.85f * 1.333f).sp,
                    fontFamily = currentFontFamily,
                    inlineContent = inlineContentMap,
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }

        if (currentBatch < verses.size) {
            TextButton(
                onClick = { currentBatch += 50 },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Load More")
            }
        }
    }
}

@Composable
private fun HtmlContent(
    currentPage: ModalPage,
    viewModel: AppViewModel,
    stack: SnapshotStateList<ModalPage>,
    scrollState: ScrollState,
    databaseHelper: DatabaseHelper?,
    strongDbHelper: DatabaseHelper?,
    dictionaryDbHelper: DatabaseHelper?,
    textColor: Color,
    linkColor: Color,
    isDark: Boolean,
    scope: CoroutineScope
) {
    val content = currentPage.content
    if (content == "Loading...") {
        InteractiveLoadingIndicator()
    } else {
        key(currentPage.content ?: "") {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        textSize = viewModel.fontSize.toFloat() * 0.85f
                        setLineSpacing(0f, 1.333f)
                        typeface = Fonts.getTypeface(ctx, viewModel.selectedFontFamily)
                        textDirection = View.TEXT_DIRECTION_LTR
                        gravity = Gravity.START
                    }
                },
                update = { textView ->
                    val spanned = HtmlCompat.fromHtml(content ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT)
                    val spannable = SpannableString(spanned)
                    val urlSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)

                    for (urlSpan in urlSpans) {
                        val start = spannable.getSpanStart(urlSpan)
                        val end = spannable.getSpanEnd(urlSpan)
                        val flags = spannable.getSpanFlags(urlSpan)
                        val href = urlSpan.url
                        val linkText = spannable.substring(start, end)

                        spannable.removeSpan(urlSpan)

                        var clickableSpan: ClickableSpan? = null
                        var addClickable = false

                        if (href.startsWith("B:")) {
                            val passage = parseVerseLink(href, linkText)
                            if (passage != null) {
                                val book = BibleData.getBookByCustomNumber(passage.bookNumber)
                                val isOld = book?.testament == Testament.OLD
                                clickableSpan = object : ClickableSpan() {
                                    override fun onClick(widget: View) {
                                        val verseStart = passage.verse ?: return
                                        val verseEndTemp = passage.verseEnd
                                        if (verseEndTemp != null) {
                                            val maxVerse = databaseHelper?.getVerseCount(passage.bookNumber, passage.chapter) ?: 0
                                            if (verseEndTemp !in verseStart..maxVerse) {
                                                passage.chapterEnd = verseEndTemp
                                            }
                                        }
                                        val verses = fetchVerses(passage, databaseHelper)
                                        val rangeStr = if (passage.chapterEnd != null) {
                                            " ${passage.verse}-Ch ${passage.chapterEnd}"
                                        } else {
                                            " ${passage.verse}" + (passage.verseEnd?.let { "-$it" } ?: "")
                                        }
                                        val newTitle = " ${passage.bookName} ${passage.chapter}:$rangeStr"
                                        stack.add(ModalPage(newTitle, "verses", verses = verses, passage = passage, isOldTestament = isOld))
                                    }
                                }
                                addClickable = true
                            }
                        } else if (href.startsWith("S:")) {
                            val seeContent = href.substringAfter("S:").trim()
                            val cleanedLinkText = linkText.replace(Regex("^See\\s+", RegexOption.IGNORE_CASE), "").trim()

                            when {
                                seeContent.startsWith("B:") -> {
                                    val verseHref = "B:" + seeContent.substringAfter("B:")
                                    val passage = parseVerseLink(verseHref, linkText)
                                    if (passage != null) {
                                        val book = BibleData.getBookByCustomNumber(passage.bookNumber)
                                        val isOld = book?.testament == Testament.OLD
                                        clickableSpan = object : ClickableSpan() {
                                            override fun onClick(widget: View) {
                                                val verseStart = passage.verse ?: return
                                                val verseEndTemp = passage.verseEnd
                                                if (verseEndTemp != null) {
                                                    val maxVerse = databaseHelper?.getVerseCount(passage.bookNumber, passage.chapter) ?: 0
                                                    if (verseEndTemp !in verseStart..maxVerse) {
                                                        passage.chapterEnd = verseEndTemp
                                                        passage.verseEnd = null
                                                    }
                                                }
                                                val verses = fetchVerses(passage, databaseHelper)
                                                val rangeStr = if (passage.chapterEnd != null) {
                                                    " ${passage.verse}-Ch ${passage.chapterEnd}"
                                                } else {
                                                    " ${passage.verse}" + (passage.verseEnd?.let { "-$it" } ?: "")
                                                }
                                                val newTitle = " ${passage.bookName} ${passage.chapter}:$rangeStr"
                                                stack.add(ModalPage(newTitle, "verses", verses = verses, passage = passage, isOldTestament = isOld))
                                            }
                                        }
                                        addClickable = true
                                    }
                                }
                                seeContent.matches(Regex("^[GH]\\d+")) -> {
                                    val isOld = seeContent.startsWith("H")
                                    clickableSpan = object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            val loadingPage = ModalPage("Strong's Definition for $seeContent", "strong", "Loading...", strongNumber = seeContent, isOldTestament = isOld)
                                            stack.add(loadingPage)
                                            scope.launch {
                                                val definition = withContext(Dispatchers.IO) {
                                                    strongDbHelper?.getStrongDefinition(seeContent) ?: "Strong's definition not found."
                                                }
                                                val prepared = prepareStrongContent(definition)
                                                val index = stack.indexOf(loadingPage)
                                                if (index != -1) {
                                                    stack[index] = loadingPage.copy(content = prepared)
                                                }
                                            }
                                        }
                                    }
                                    addClickable = true
                                }
                                seeContent.matches(Regex("^\\d+")) -> {
                                    val currentIsOld = stack.last().isOldTestament
                                    val hNum = "H$seeContent"
                                    val gNum = "G$seeContent"
                                    clickableSpan = object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            val loadingPage = ModalPage("Loading Strong's Definition", "strong", "Loading...", strongNumber = "$hNum,$gNum", isOldTestament = currentIsOld)
                                            stack.add(loadingPage)
                                            scope.launch {
                                                val hDef = withContext(Dispatchers.IO) { strongDbHelper?.getStrongDefinition(hNum) ?: "" }
                                                val gDef = withContext(Dispatchers.IO) { strongDbHelper?.getStrongDefinition(gNum) ?: "" }
                                                var combinedDef = ""
                                                var combinedTitle = "Strong's Definition"
                                                var combinedStrongNum = ""
                                                val preparedH = if (hDef.isNotBlank()) prepareStrongContent(hDef) else ""
                                                val preparedG = if (gDef.isNotBlank()) prepareStrongContent(gDef) else ""
                                                when {
                                                    preparedH.isNotBlank() && preparedG.isNotBlank() -> {
                                                        combinedTitle += " for $hNum and $gNum"
                                                        combinedDef = "$hNum:\n$preparedH\n\n$gNum:\n$preparedG"
                                                        combinedStrongNum = "$hNum,$gNum"
                                                    }
                                                    preparedH.isNotBlank() -> {
                                                        combinedTitle += " for $hNum"
                                                        combinedDef = preparedH
                                                        combinedStrongNum = hNum
                                                    }
                                                    preparedG.isNotBlank() -> {
                                                        combinedTitle += " for $gNum"
                                                        combinedDef = preparedG
                                                        combinedStrongNum = gNum
                                                    }
                                                    else -> {
                                                        combinedTitle = "Strong's Definition not found"
                                                        combinedDef = "No definition found."
                                                    }
                                                }
                                                val index = stack.indexOf(loadingPage)
                                                if (index != -1) {
                                                    stack[index] = loadingPage.copy(title = combinedTitle, content = combinedDef, strongNumber = combinedStrongNum)
                                                }
                                            }
                                        }
                                    }
                                    addClickable = true
                                }
                                else -> {
                                    val wordToFetch = cleanedLinkText.ifEmpty { seeContent }
                                    clickableSpan = object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            val dbDisplayName = dictionaryDisplayNames[viewModel.selectedDictionary] ?: viewModel.selectedDictionary
                                            val capitalizedWordToFetch = wordToFetch.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                            val loadingTitle = "Loading Definition of $capitalizedWordToFetch"
                                            val loadingPage = ModalPage(
                                                loadingTitle,
                                                "definition",
                                                "Loading...",
                                                word = wordToFetch,
                                                description = dbDisplayName,
                                                isOldTestament = stack.last().isOldTestament
                                            )
                                            stack.add(loadingPage)
                                            scope.launch {
                                                val pairs: List<Pair<String, String>> = getDefinitionOrClosest(dictionaryDbHelper, wordToFetch) ?: emptyList()
                                                val newContentInner: String
                                                val newTitleInner: String
                                                if (pairs.isNotEmpty()) {
                                                    val isExact = pairs.size == 1 && pairs[0].first.equals(wordToFetch, ignoreCase = true)
                                                    val isTopical = viewModel.selectedDictionary == "topical"
                                                    newTitleInner = if (isTopical) {
                                                        "References for $capitalizedWordToFetch"
                                                    } else if (isExact) {
                                                        "Definition of ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                                    } else if (pairs.size == 1) {
                                                        "Closest match for ${pairs[0].first.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                                                    } else {
                                                        "Matches for \"$capitalizedWordToFetch\""
                                                    }
                                                    newContentInner = if (isExact) {
                                                        sanitizeHtmlContent(pairs[0].second)
                                                    } else if (pairs.size == 1) {
                                                        cleanDefinition(pairs[0].first, pairs[0].second)
                                                    } else {
                                                        pairs.joinToString("<br><hr><br>") { p -> sanitizeHtmlContent(p.second) }
                                                    }
                                                } else {
                                                    newTitleInner = "Definition of $capitalizedWordToFetch not found"
                                                    newContentInner = "No definition found."
                                                }
                                                val index = stack.indexOf(loadingPage)
                                                if (index != -1) {
                                                    stack[index] = loadingPage.copy(title = newTitleInner, content = newContentInner)
                                                }
                                            }
                                        }
                                    }
                                    addClickable = true
                                }
                            }
                        }

                        if (addClickable) {
                            spannable.setSpan(clickableSpan, start, end, flags)
                        }
                    }

                    if (isDark) {
                        val colorSpans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
                        for (span in colorSpans) {
                            spannable.removeSpan(span)
                        }
                        textView.setTextColor(Color.White.toArgb())
                    } else {
                        textView.setTextColor(textColor.toArgb())
                    }
                    textView.setLinkTextColor(linkColor.toArgb())
                    textView.text = spannable
                },
                modifier = Modifier.verticalScroll(scrollState)
            )
        }
    }
}

@Composable
private fun DialogConfirmButtons(
    stack: SnapshotStateList<ModalPage>,
    onDismiss: () -> Unit
) {
    Row {
        if (stack.size > 1) {
            TextButton(onClick = { stack.removeAt(stack.lastIndex) }) {
                Text("Back")
            }
        }
        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }
}

@Composable
private fun DialogDismissButton(
    currentPage: ModalPage,
    viewModel: AppViewModel,
    dictionaries: List<String>,
    verseCommentaries: List<String>,
    crossReferenceDatabases: List<String>,
    switchToDictionary: (String) -> Unit,
    switchToVerseCommentary: (String) -> Unit,
    switchToCrossReference: (String) -> Unit
) {
    when (currentPage.type) {
        "definition" -> {
            val currentIndex = dictionaries.indexOf(viewModel.selectedDictionary)
            val nextIndex = (currentIndex + 1) % dictionaries.size
            val nextDictionary = dictionaries[nextIndex]
            TextButton(onClick = { switchToDictionary(nextDictionary) }) {
                Text("Switch to ${nextDictionary.uppercase()}")
            }
        }
        "versecommentary" -> {
            val currentIndex = verseCommentaries.indexOf(viewModel.selectedVerseCommentary)
            val nextIndex = (currentIndex + 1) % verseCommentaries.size
            val nextKey = verseCommentaries[nextIndex]
            TextButton(onClick = { switchToVerseCommentary(nextKey) }) {
                Text("Switch to ${nextKey.uppercase()}")
            }
        }
        "crossreference" -> {
            val idx = crossReferenceDatabases.indexOf(viewModel.selectedCrossReferenceDatabase)
            val nextKey = crossReferenceDatabases[(idx + 1) % crossReferenceDatabases.size]
            TextButton(onClick = { switchToCrossReference(nextKey) }) {
                Text("Switch to ${nextKey.uppercase()}")
            }
        }
        else -> null
    }
}