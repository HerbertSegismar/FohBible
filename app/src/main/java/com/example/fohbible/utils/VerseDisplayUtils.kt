package com.example.fohbible.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

// Data classes for parsing
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
    val baselineShift: BaselineShift? = null
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
    val highlightIcon: Color
)

class VerseTextProcessor {
    fun processVerse(
        verseText: String?,
        baseFontSize: TextUnit,
        themeColors: ThemeColors,
        highlight: String? = null,
        onTagPress: ((String) -> Unit)? = null,
        textColor: Color? = null,
        onWordPress: ((String) -> Unit)? = null,
        isHighlighted: Boolean = false,
        isKjvPlus: Boolean = false
    ): ProcessedVerse {
        val nodes = parseXmlTags(verseText ?: "")
        val tree = buildTree(nodes)
        val initialContext = TraversalContext(
            textColor = textColor ?: themeColors.textColor,
            isTextContainer = false,
            isHeader = false,
            currentTag = null,
            baseFontSize = baseFontSize
        )
        val (header, body) = traverseTree(tree, initialContext, highlight, themeColors, onTagPress, onWordPress, isHighlighted, isKjvPlus)
        return ProcessedVerse(header, body)
    }

    private fun parseXmlTags(text: String): List<ParsedNode> {
        if (text.isEmpty()) return emptyList()
        val nodes = mutableListOf<ParsedNode>()
        var currentText = ""
        var i = 0
        while (i < text.length) {
            if (text[i] == '<') {
                // Save any accumulated text
                if (currentText.isNotEmpty()) {
                    nodes.add(ParsedNode.Text(currentText))
                    currentText = ""
                }
                val tagEnd = text.indexOf('>', i)
                if (tagEnd == -1) {
                    currentText += text.substring(i)
                    break
                }
                val fullTag = text.substring(i, tagEnd + 1)
                if (fullTag.startsWith("</")) {
                    val tagName = fullTag.substring(2, fullTag.length - 1).trim().split(" ")[0]
                    nodes.add(ParsedNode.ClosingTag(tagName))
                } else if (fullTag.endsWith("/>")) {
                    val tagName = fullTag.substring(1, fullTag.length - 2).trim().split(" ")[0]
                    nodes.add(ParsedNode.SelfClosingTag(tagName, fullTag))
                } else {
                    val tagName = fullTag.substring(1, fullTag.length - 1).trim().split(" ")[0]
                    nodes.add(ParsedNode.OpeningTag(tagName, fullTag))
                }
                i = tagEnd + 1
            } else {
                currentText += text[i]
                i++
            }
        }
        if (currentText.isNotEmpty()) {
            nodes.add(ParsedNode.Text(currentText))
        }
        return nodes
    }

    private fun buildTree(nodes: List<ParsedNode>): List<TreeNode> {
        val root = mutableListOf<TreeNode>()
        val stack = mutableListOf<MutableList<TreeNode>>()
        var current = root
        for (node in nodes) {
            when (node) {
                is ParsedNode.Text -> {
                    current.add(TreeNode.Text(node.content))
                }
                is ParsedNode.OpeningTag -> {
                    val element = TreeNode.Element(node.tag, node.fullTag, mutableListOf())
                    current.add(element)
                    stack.add(current)
                    current = element.children as MutableList<TreeNode>
                }
                is ParsedNode.ClosingTag -> {
                    if (stack.isNotEmpty()) {
                        current = stack.removeAt(stack.size - 1)
                    }
                }
                is ParsedNode.SelfClosingTag -> {
                    current.add(TreeNode.SelfClosingTag(node.tag, node.fullTag))
                }
            }
        }
        return root
    }

    private fun traverseTree(
        tree: List<TreeNode>,
        initialContext: TraversalContext,
        highlight: String?,
        themeColors: ThemeColors,
        onTagPress: ((String) -> Unit)?,
        onWordPress: ((String) -> Unit)?,
        isHighlighted: Boolean,
        isKjvPlus: Boolean
    ): Pair<AnnotatedString?, AnnotatedString> {
        val headerBuilder = AnnotatedString.Builder()
        val bodyBuilder = AnnotatedString.Builder()
        for (node in tree) {
            traverseNode(
                node,
                headerBuilder,
                bodyBuilder,
                initialContext,
                highlight,
                themeColors,
                onTagPress,
                onWordPress,
                isHighlighted,
                isKjvPlus
            )
        }
        val header = if (headerBuilder.length > 0) headerBuilder.toAnnotatedString() else null
        val body = bodyBuilder.toAnnotatedString()
        return Pair(header, body)
    }

    private fun traverseNode(
        node: TreeNode,
        headerBuilder: AnnotatedString.Builder,
        bodyBuilder: AnnotatedString.Builder,
        context: TraversalContext,
        highlight: String?,
        themeColors: ThemeColors,
        onTagPress: ((String) -> Unit)?,
        onWordPress: ((String) -> Unit)?,
        isHighlighted: Boolean,
        isKjvPlus: Boolean
    ) {
        when (node) {
            is TreeNode.Text -> {
                val builder = if (context.isHeader) headerBuilder else bodyBuilder
                processTextNode(
                    node,
                    builder,
                    context,
                    highlight,
                    themeColors,
                    onWordPress,
                    isHighlighted
                )
            }
            is TreeNode.SelfClosingTag -> {
                val builder = if (context.isHeader) headerBuilder else bodyBuilder
                processSelfClosingTagNode(
                    node,
                    builder,
                    context,
                    themeColors,
                    onTagPress
                )
            }
            is TreeNode.Element -> {
                val newContext = when (node.tag) {
                    "n" -> context.copy(
                        isHeader = !isKjvPlus,
                        textColor = themeColors.primary,
                        currentTag = node.tag
                    )
                    "J" -> context.copy(
                        isTextContainer = true,
                        textColor = if (!isHighlighted) themeColors.wordsOfJesus else context.textColor,
                        currentTag = node.tag
                    )
                    "t" -> context.copy(
                        isTextContainer = true,
                        currentTag = node.tag
                    )
                    "S" -> context.copy(
                        isTextContainer = true,
                        textColor = themeColors.tagColor,
                        fontSizeMultiplier = 0.7f,
                        baselineShift = BaselineShift(0.2f),
                        currentTag = node.tag
                    )
                    "f" -> context.copy(
                        isTextContainer = true,
                        textColor = themeColors.tagColor,
                        fontSizeMultiplier = 0.8f,
                        baselineShift = BaselineShift(0.2f),
                        currentTag = node.tag
                    )
                    else -> context.copy(currentTag = node.tag)
                }
                // Traverse children
                for (child in node.children) {
                    traverseNode(
                        child,
                        headerBuilder,
                        bodyBuilder,
                        newContext,
                        highlight,
                        themeColors,
                        onTagPress,
                        onWordPress,
                        isHighlighted,
                        isKjvPlus
                    )
                }
            }
        }
    }

    private fun processTextNode(
        node: TreeNode.Text,
        builder: AnnotatedString.Builder,
        context: TraversalContext,
        highlight: String?,
        themeColors: ThemeColors,
        onWordPress: ((String) -> Unit)?,
        isHighlighted: Boolean
    ) {
        val effectiveMultiplier = if (context.currentTag == "f" && isEncircled(node.content)) {
            1.1f
        } else {
            context.fontSizeMultiplier
        }
        val textContext = context.copy(fontSizeMultiplier = effectiveMultiplier)
        val text = node.content
        processNormalText(text, builder, textContext, highlight, themeColors, onWordPress, isHighlighted)
    }

    private fun isEncircled(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length != 1) return false
        val code = trimmed[0].code
        return code in 0x2460..0x24FF
    }

    private fun processNormalText(
        text: String,
        builder: AnnotatedString.Builder,
        context: TraversalContext,
        highlight: String?,
        themeColors: ThemeColors,
        onWordPress: ((String) -> Unit)?,
        isHighlighted: Boolean
    ) {
        if (onWordPress != null) {
            // Split into words for clickable words
            val words = splitIntoWords(text)
            for (word in words) {
                if (isWord(word) && word.length > 1) {
                    // Make word clickable by adding annotation
                    builder.pushStringAnnotation("word", word)
                    builder.withStyle(
                        SpanStyle(
                            color = context.textColor,
                            background = if (highlight != null && word.contains(highlight, ignoreCase = true)) themeColors.searchHighlightBg else Color.Transparent,
                            fontSize = context.baseFontSize * context.fontSizeMultiplier,
                            baselineShift = context.baselineShift ?: BaselineShift.None
                        )
                    ) {
                        builder.append(word)
                    }
                    builder.pop()
                } else {
                    // Non-words (punctuation, numbers, whitespace)
                    builder.withStyle(
                        SpanStyle(
                            color = context.textColor,
                            fontSize = context.baseFontSize * context.fontSizeMultiplier,
                            baselineShift = context.baselineShift ?: BaselineShift.None
                        )
                    ) {
                        builder.append(word)
                    }
                }
            }
        } else {
            // Simple text with highlight support
            if (highlight != null && text.contains(highlight, ignoreCase = true)) {
                val regex = Regex(escapeRegex(highlight), RegexOption.IGNORE_CASE)
                var lastIndex = 0
                for (match in regex.findAll(text)) {
                    // Add text before match
                    if (match.range.first > lastIndex) {
                        builder.withStyle(
                            SpanStyle(
                                color = context.textColor,
                                fontSize = context.baseFontSize * context.fontSizeMultiplier,
                                baselineShift = context.baselineShift ?: BaselineShift.None
                            )
                        ) {
                            builder.append(text.substring(lastIndex, match.range.first))
                        }
                    }
                    // Add highlighted match
                    builder.withStyle(
                        SpanStyle(
                            color = context.textColor,
                            background = themeColors.searchHighlightBg,
                            fontSize = context.baseFontSize * context.fontSizeMultiplier,
                            baselineShift = context.baselineShift ?: BaselineShift.None
                        )
                    ) {
                        builder.append(match.value)
                    }
                    lastIndex = match.range.last + 1
                }
                // Add remaining text
                if (lastIndex < text.length) {
                    builder.withStyle(
                        SpanStyle(
                            color = context.textColor,
                            fontSize = context.baseFontSize * context.fontSizeMultiplier,
                            baselineShift = context.baselineShift ?: BaselineShift.None
                        )
                    ) {
                        builder.append(text.substring(lastIndex))
                    }
                }
            } else {
                builder.withStyle(
                    SpanStyle(
                        color = context.textColor,
                        fontSize = context.baseFontSize * context.fontSizeMultiplier,
                        baselineShift = context.baselineShift ?: BaselineShift.None
                    )
                ) {
                    builder.append(text)
                }
            }
        }
    }

    private fun processSelfClosingTagNode(
        node: TreeNode.SelfClosingTag,
        builder: AnnotatedString.Builder,
        context: TraversalContext,
        themeColors: ThemeColors,
        onTagPress: ((String) -> Unit)?
    ) {
        val content = extractContentFromTag(node.fullTag)
        val trimmedContent = content.trim()
        builder.withStyle(
            SpanStyle(
                fontSize = context.baseFontSize * 0.8f,
                color = themeColors.tagColor,
                background = themeColors.tagBg
            )
        ) {
            builder.append(trimmedContent)
        }
    }

    private fun extractContentFromTag(tag: String): String {
        // Extract content from self-closing tags like <wt="word"/>
        val regex = """<[^>]+="([^"]*)"/>""".toRegex()
        val match = regex.find(tag)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun splitIntoWords(text: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val char = text[i]
            when {
                char.isLetter() -> {
                    var word = char.toString()
                    i++
                    while (i < text.length && text[i].isLetter()) {
                        word += text[i]
                        i++
                    }
                    result.add(word)
                }
                char.isDigit() -> {
                    var num = char.toString()
                    i++
                    while (i < text.length && text[i].isDigit()) {
                        num += text[i]
                        i++
                    }
                    result.add(num)
                }
                !char.isWhitespace() -> {
                    var punct = char.toString()
                    i++
                    while (i < text.length && !text[i].isWhitespace() && !text[i].isLetter() && !text[i].isDigit()) {
                        punct += text[i]
                        i++
                    }
                    result.add(punct)
                }
                else -> {
                    var whitespace = char.toString()
                    i++
                    while (i < text.length && text[i].isWhitespace()) {
                        whitespace += text[i]
                        i++
                    }
                    result.add(whitespace)
                }
            }
        }
        return result
    }

    private fun isWord(text: String): Boolean {
        return text.matches(Regex("[a-zA-ZÀ-ÿ]{2,}"))
    }

    private fun escapeRegex(string: String): String {
        return string.replace(Regex("[.*+?^${'$'}{}()|\\[\\]\\\\]"), "\\$&")
    }
}

// Simple processor for HomeScreen use
object SimpleVerseProcessor {
    fun stripXmlTags(text: String): String {
        return text
            .replace(Regex("""<[^>]+>"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun extractVerseReference(verses: List<com.example.fohbible.data.Verse>): String {
        if (verses.isEmpty()) return ""
        val first = verses.first()
        return if (verses.size == 1) {
            "${first.bookName ?: ""} ${first.chapter}:${first.verseNumber}"
        } else {
            "${first.bookName ?: ""} ${first.chapter}:${first.verseNumber}-${verses.last().verseNumber}"
        }
    }
}