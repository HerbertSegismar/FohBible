package com.fountofhopedotorg.fohbible.data

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import com.fountofhopedotorg.fohbible.MainActivity
import com.fountofhopedotorg.fohbible.ui.theme.DefaultPrimaryColor
import java.util.UUID

data class PassageSelection(
    val bookNumber: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int? = null,
    var verseEnd: Int? = null,
    var chapterEnd: Int? = null
)

data class ModalPage(
    val title: String,
    val type: String,
    val content: String? = null,
    val verses: List<Verse>? = null,
    val passage: PassageSelection? = null,
    val word: String? = null,
    val strongNumber: String? = null,
    val description: String? = null,
    val isOldTestament: Boolean,
    val bookNumber: Int? = null,
    val chapter: Int? = null,
    val verse: Int? = null
)

data class BibleVersionInfo(
    val description: String?,
    val detailedInfo: String?
)

data class Verse(
    val verseNumber: Int,
    val text: String,
    val bookName: String? = null,
    val chapter: Int? = null,
    val bookNumber: Int? = null
)

data class SearchColors(
    val primary: Color,
    val background: Color,
    val text: Color,
    val muted: Color,
    val card: Color,
    val border: Color
)

data class SearchVerse(
    val verse: Int,
    val text: String?,
    val bookNumber: Int = 0,
    val chapter: Int = 0,
    val bookName: String? = null,
    val bookColor: String? = null
)

data class InspirationalVerseRef(
    val bookNumber: Int,
    val chapter: Int,
    val verse: Int,
    val endVerse: Int? = null
)

data class CrossReference(
    val bookTo: Int,
    val chapterTo: Int,
    val verseToStart: Int,
    val verseToEnd: Int
)

data class Note(
    val bookName: String,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int,
    val note: String,
    val timestamp: Long
)

data class VerseCommentary(
    val text: String,
    val chapterFrom: Int,
    val verseFrom: Int,
    val chapterTo: Int?,
    val verseTo: Int?
)

data class ScopeRange(val start: Int, val end: Int)

data class BibleBook(
    val customNumber: Int,
    val name: String,
    val chapters: Int,
    val testament: Testament,
    val abbreviation: String,
    val standardNumber: Int = 0
) {
    fun getVersesForChapter(chapter: Int, context: Context? = null): Int {
        return if (context != null) {
            val dbHelper = DatabaseHelper(context as MainActivity, databaseName = "kj2.sqlite3")
            dbHelper.getVerseCount(customNumber, chapter)
        } else {
            30
        }
    }
}

data class ScopeConfig(
    val label: String,
    val description: String,
    val category: String
)

data class Subheading(val verse: Int, val text: String)

sealed class VerseContent {
    data class SubheadingVal(val subheading: Subheading) : VerseContent()
    data class VerseVal(val verse: Verse) : VerseContent()
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class AppThemeState(
    val darkTheme: Boolean = false,
    val primaryColor: Color = DefaultPrimaryColor,
    val isCustomColor: Boolean = false
)

data class AppColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color
)

data class ColorTheme(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

data class Drop(
    val id: String,
    val headAnim: Animatable<Float, *>,
    val trailChars: List<String>,
    val x: Float
)

data class Overlay(
    val id: String,
    val text: String,
    val left: Float,
    val top: Float,
    val fontSize: Float,
    val fadeAnim: Animatable<Float, *>,
    val positionAnim: Animatable<Float, *>
)

sealed class ParsedNode {
    data class Text(val content: String) : ParsedNode()
    data class OpeningTag(val tag: String, val fullTag: String) : ParsedNode()
    data class ClosingTag(val tag: String) : ParsedNode()
    data class SelfClosingTag(val tag: String, val fullTag: String) : ParsedNode()
}

sealed class TreeNode {
    data class Text(val content: String) : TreeNode()
    data class SelfClosingTag(val tag: String, val fullTag: String) : TreeNode()
    data class Element(val tag: String, val fullTag: String, val children: List<TreeNode>) : TreeNode()
}

data class TraversalContext(
    val textColor: Color,
    val isTextContainer: Boolean,
    val isHeader: Boolean,
    val currentTag: String?,
    val baseFontSize: TextUnit,
    val fontSizeMultiplier: Float = 1f,
    val baselineShift: BaselineShift? = null,
    val isOldTestament: Boolean
)

data class ProcessedVerse(
    val header: AnnotatedString?,
    val body: AnnotatedString
)

data class ThemeColors(
    val textColor: Color,
    val verseNumber: Color,
    val primary: Color,
    val tagColor: Color,
    val tagBg: Color,
    val wordsOfJesus: Color,
    val searchHighlightBg: Color,
    val highlightIcon: Color,
    val wordHighlightBg: Color = Color.Transparent
)

data class ProcessingOptions(
    val enableWordClick: Boolean = true,
    val enableStrongsClick: Boolean = true,
    val enableTagClick: Boolean = true,
    val showFootnotesInline: Boolean = true,
    val preserveWhitespace: Boolean = false,
    val showHeaders: Boolean = true,
    val showStrongs: Boolean = true
)

data class RealisticSplash(
    val color: Color,
    val position: Offset,
    val scale: Float,
    val mainBlobs: List<Blob>,
    val droplets: List<Droplet>
)

data class Blob(
    val offset: Offset,
    val radius: Float,
    val alpha: Float
)

data class Droplet(
    val offset: Offset,
    val radius: Float,
    val alpha: Float,
    val hasTail: Boolean = false,
    val tailLength: Float = 0f
)

data class CanvasNote(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val offset: Offset = Offset.Zero,
    val width: Float = 900f,
    val height: Float = 900f,
    val rotation: Float = 0f,
    val backgroundColor: Color = Color.White,
    val position: Offset = Offset.Zero,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val customName: String? = null,
    val groupId: String? = null,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f
)

data class BezierNode(
    val anchor: Offset,
    val handleIn: Offset,
    val handleOut: Offset
)

data class SearchOptions(val bookRange: Pair<Int, Int>? = null)

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
    val backgroundImage: Any
)

data class BezierNodeData(
    val anchor: Offset,
    val handleIn: Offset,
    val handleOut: Offset
)

data class BoundingBox(
    val minX: Float, val minY: Float,
    val maxX: Float, val maxY: Float
)

data class PopularDevotional(
    val title: String,
    val preview: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int
)

data class BookUi(
    val bookNumber: Int,
    val longName: String,
    val shortName: String,
    val testament: Testament,
    val totalChapters: Int
)

data class SelectedWord(
    val verseNumber: Int,
    val start: Int,
    val end: Int,
    val color: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectedWord) return false
        return verseNumber == other.verseNumber && start == other.start && end == other.end
    }

    override fun hashCode(): Int {
        var result = verseNumber
        result = 31 * result + start
        result = 31 * result + end
        return result
    }
}

sealed class DisplayItem {
    data class GroupHeader(
        val groupId: String,
        val groupName: String,
        val memberCount: Int,
        val isExpanded: Boolean
    ) : DisplayItem()

    data class NoteItem(
        val note: CanvasNote,
        val originalIndex: Int,
        val isGrouped: Boolean,
        val groupId: String? = null
    ) : DisplayItem()
}