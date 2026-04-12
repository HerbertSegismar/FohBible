package com.fountofhopedotorg.fohbible.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.fountofhopedotorg.fohbible.data.ParsedNode
import com.fountofhopedotorg.fohbible.data.ProcessedVerse
import com.fountofhopedotorg.fohbible.data.ProcessingOptions
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.TraversalContext
import com.fountofhopedotorg.fohbible.data.TreeNode

class VerseTextProcessor {
    companion object {
        private val ATTRIBUTE_REGEX = Regex("""(\w+)="([^"]*)"""")
        private const val CACHE_SIZE = 200
        private val ESCAPE_REGEX_PATTERN = Regex($$"[.*+?^${}()|\\[\\]\\\\]")
    }

    private val verseCache = object : LinkedHashMap<String, ProcessedVerse>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, ProcessedVerse>): Boolean {
            return size > CACHE_SIZE
        }
    }

    fun processVerse(
        verseText: String?,
        baseFontSize: TextUnit,
        themeColors: ThemeColors,
        highlight: String? = null,
        onTagPress: ((String) -> Unit)? = null,
        textColor: Color? = null,
        onWordPress: ((String) -> Unit)? = null,
        onStrongsPress: ((String) -> Unit)? = null,
        isHighlighted: Boolean = false,
        isOldTestament: Boolean,
        options: ProcessingOptions = ProcessingOptions(),
        wordHighlights: Set<String>? = null
    ): ProcessedVerse {
        if (verseText.isNullOrBlank()) {
            return ProcessedVerse(
                header = null,
                body = AnnotatedString("")
            )
        }

        val cacheKey = buildCacheKey(
            verseText = verseText,
            baseFontSize = baseFontSize,
            textColor = textColor,
            isOldTestament = isOldTestament,
            highlight = highlight,
            isHighlighted = isHighlighted,
            options = options,
            themeColors = themeColors,
            wordHighlights = wordHighlights,
            enableTagClick = options.enableTagClick && onTagPress != null,
            enableStrongsClick = options.enableStrongsClick && onStrongsPress != null,
            enableWordClick = options.enableWordClick && onWordPress != null
        )
        verseCache[cacheKey]?.let { cached ->
            return cached
        }

        val nodes = parseXmlTags(verseText)
        val tree = buildTree(nodes)

        val initialContext = TraversalContext(
            textColor = textColor ?: themeColors.textColor,
            isTextContainer = false,
            isHeader = false,
            currentTag = null,
            baseFontSize = baseFontSize,
            isOldTestament = isOldTestament
        )

        val (header, body) = traverseTree(
            tree, initialContext, highlight, themeColors,
            onTagPress, onWordPress, onStrongsPress,
            isHighlighted, options, wordHighlights
        )

        val result = ProcessedVerse(
            header = if (options.showHeaders) header else null,
            body = body
        )
        verseCache[cacheKey] = result
        return result
    }

    private fun buildCacheKey(
        verseText: String,
        baseFontSize: TextUnit,
        textColor: Color?,
        isOldTestament: Boolean,
        highlight: String?,
        isHighlighted: Boolean,
        options: ProcessingOptions,
        themeColors: ThemeColors,
        wordHighlights: Set<String>?,
        enableTagClick: Boolean,
        enableStrongsClick: Boolean,
        enableWordClick: Boolean
    ): String {
        return StringBuilder()
            .append(verseText.hashCode())
            .append("|")
            .append(baseFontSize.value)
            .append("|")
            .append(textColor?.value ?: 0)
            .append("|")
            .append(isOldTestament)
            .append("|")
            .append(highlight)
            .append("|")
            .append(isHighlighted)
            .append("|")
            .append(options.hashCode())
            .append("|")
            .append(themeColors.hashCode())
            .append("|")
            .append(wordHighlights?.hashCode() ?: 0)
            .append("|enableTagClick=$enableTagClick")
            .append("|enableStrongsClick=$enableStrongsClick")
            .append("|enableWordClick=$enableWordClick")
            .toString()
    }

    private fun parseXmlTags(text: String): List<ParsedNode> {
        if (text.isEmpty()) return emptyList()
        val nodes = mutableListOf<ParsedNode>()
        val currentText = StringBuilder()
        var i = 0
        val stack = mutableListOf<String>()

        while (i < text.length) {
            if (text[i] == '<') {
                if (currentText.isNotEmpty()) {
                    nodes.add(ParsedNode.Text(currentText.toString()))
                    currentText.clear()
                }
                val tagEnd = text.indexOf('>', i)
                if (tagEnd == -1) {
                    currentText.append(text.substring(i))
                    break
                }
                val fullTag = text.substring(i, tagEnd + 1)
                when {
                    fullTag.startsWith("</") -> {
                        val tagName = fullTag.substring(2, fullTag.length - 1).trim().split(" ")[0]
                        if (stack.isNotEmpty() && stack.last() == tagName) {
                            stack.removeAt(stack.size - 1)
                        }
                        nodes.add(ParsedNode.ClosingTag(tagName))
                    }
                    fullTag.endsWith("/>") -> {
                        val tagName = fullTag.substring(1, fullTag.length - 2).trim().split(" ")[0]
                        nodes.add(ParsedNode.SelfClosingTag(tagName, fullTag))
                    }
                    else -> {
                        val tagName = fullTag.substring(1, fullTag.length - 1).trim().split(" ")[0]
                        stack.add(tagName)
                        nodes.add(ParsedNode.OpeningTag(tagName, fullTag))
                    }
                }
                i = tagEnd + 1
            } else {
                currentText.append(text[i])
                i++
            }
        }
        if (currentText.isNotEmpty()) {
            nodes.add(ParsedNode.Text(currentText.toString()))
        }
        if (stack.isNotEmpty()) {
            for (tag in stack.reversed()) {
                nodes.add(ParsedNode.ClosingTag(tag))
            }
        }
        return nodes
    }

    private fun buildTree(nodes: List<ParsedNode>): List<TreeNode> {
        val root = mutableListOf<TreeNode>()
        val stack = mutableListOf<MutableList<TreeNode>>()
        val elementStack = mutableListOf<TreeNode.Element>()
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
                    elementStack.add(element)
                    current = element.children as MutableList<TreeNode>
                }
                is ParsedNode.ClosingTag -> {
                    if (stack.isNotEmpty()) {
                        if (elementStack.isNotEmpty()) {
                            elementStack.removeAt(elementStack.size - 1)
                        }
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
        onStrongsPress: ((String) -> Unit)?,
        isHighlighted: Boolean,
        options: ProcessingOptions,
        wordHighlights: Set<String>?
    ): Pair<AnnotatedString?, AnnotatedString> {
        val headerBuilder = AnnotatedString.Builder()
        val bodyBuilder = AnnotatedString.Builder()
        traverseChildren(
            tree, headerBuilder, bodyBuilder, initialContext,
            highlight, themeColors, onTagPress, onWordPress, onStrongsPress,
            isHighlighted, options, wordHighlights
        )
        val header = if (headerBuilder.length > 0) headerBuilder.toAnnotatedString() else null
        val body = bodyBuilder.toAnnotatedString()
        return Pair(header, body)
    }

    private fun traverseChildren(
        children: List<TreeNode>,
        headerBuilder: AnnotatedString.Builder,
        bodyBuilder: AnnotatedString.Builder,
        context: TraversalContext,
        highlight: String?,
        themeColors: ThemeColors,
        onTagPress: ((String) -> Unit)?,
        onWordPress: ((String) -> Unit)?,
        onStrongsPress: ((String) -> Unit)?,
        isHighlighted: Boolean,
        options: ProcessingOptions,
        wordHighlights: Set<String>?
    ) {
        for (i in children.indices) {
            if (i > 0) {
                val builder = if (context.isHeader) headerBuilder else bodyBuilder
                builder.append(" ")
            }
            traverseNode(
                children[i], headerBuilder, bodyBuilder, context,
                highlight, themeColors, onTagPress, onWordPress, onStrongsPress,
                isHighlighted,  options, wordHighlights
            )
        }
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
        onStrongsPress: ((String) -> Unit)?,
        isHighlighted: Boolean,
        options: ProcessingOptions,
        wordHighlights: Set<String>?
    ) {
        when (node) {
            is TreeNode.Text -> {
                val builder = if (context.isHeader) headerBuilder else bodyBuilder
                processTextNode(
                    node, builder, context, highlight, themeColors,
                    onWordPress, onStrongsPress, onTagPress, options, wordHighlights
                )
            }
            is TreeNode.SelfClosingTag -> {
                if (!options.showFootnotesInline) return
                val builder = if (context.isHeader) headerBuilder else bodyBuilder
                processSelfClosingTagNode(node, builder, context, themeColors)
            }
            is TreeNode.Element -> {
                val newContext = when (node.tag) {
                    "n" -> {
                        return
                    }
                    "J" -> context.copy(
                        isTextContainer = true,
                        textColor = if (!isHighlighted) themeColors.wordsOfJesus else context.textColor,
                        currentTag = node.tag
                    )
                    "t" -> context.copy(isTextContainer = true, currentTag = node.tag)
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
                        fontSizeMultiplier = 1.0f,
                        baselineShift = BaselineShift.None,
                        currentTag = node.tag
                    )
                    else -> context.copy(currentTag = node.tag)
                }
                traverseChildren(
                    node.children, headerBuilder, bodyBuilder, newContext,
                    highlight, themeColors, onTagPress, onWordPress, onStrongsPress,
                    isHighlighted, options, wordHighlights
                )
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
        onStrongsPress: ((String) -> Unit)?,
        onTagPress: ((String) -> Unit)?,
        options: ProcessingOptions,
        wordHighlights: Set<String>?
    ) {
        val effectiveMultiplier = if (context.currentTag == "f" && isEncircled(node.content)) {
            1.25f
        } else {
            context.fontSizeMultiplier
        }
        val textContext = context.copy(fontSizeMultiplier = effectiveMultiplier)

        val rawText = if (options.preserveWhitespace) node.content else node.content.trim()
        if (rawText.isEmpty()) return

        when (context.currentTag) {
            "S" -> if (options.enableStrongsClick && onStrongsPress != null) {
                val trimmed = rawText.trim()
                if (trimmed.isNotEmpty() && isValidStrongsNumber(trimmed)) {
                    val prefixed = formatStrongsNumber(trimmed, context.isOldTestament)
                    builder.pushStringAnnotation("strong", prefixed)
                    builder.withStyle(
                        SpanStyle(
                            color = textContext.textColor,
                            fontSize = textContext.baseFontSize * textContext.fontSizeMultiplier,
                            baselineShift = textContext.baselineShift ?: BaselineShift.None
                        )
                    ) {
                        builder.append(rawText)
                    }
                    builder.pop()
                } else {
                    processNormalText(rawText, builder, textContext, highlight, themeColors, onWordPress, options, wordHighlights)
                }
            } else {
                processNormalText(rawText, builder, textContext, highlight, themeColors, onWordPress, options, wordHighlights)
            }

            "f" -> {
                val trimmed = rawText.trim()
                val visibleStyle = SpanStyle(
                    color = textContext.textColor,
                    fontSize = textContext.baseFontSize * textContext.fontSizeMultiplier,
                    baselineShift = textContext.baselineShift ?: BaselineShift.None
                )

                if (options.enableTagClick && onTagPress != null) {
                    if (isEncircled(node.content) && trimmed.isNotEmpty()) {
                        builder.pushStringAnnotation("tag", trimmed)

                        builder.withStyle(visibleStyle) {
                            builder.append(rawText)
                        }
                        builder.withStyle(
                            SpanStyle(
                                color = Color.Transparent,
                                fontSize = textContext.baseFontSize * textContext.fontSizeMultiplier,
                                baselineShift = BaselineShift.None
                            )
                        ) {
                            builder.append("\u200B")
                        }

                        builder.pop()
                    } else if (trimmed.isNotEmpty()) {
                        builder.pushStringAnnotation("tag", trimmed)
                        builder.withStyle(visibleStyle) {
                            builder.append(rawText)
                        }
                        builder.pop()
                    } else {
                        builder.withStyle(visibleStyle) {
                            builder.append(rawText)
                        }
                    }
                } else {
                    processNormalText(rawText, builder, textContext, highlight, themeColors, onWordPress, options, wordHighlights)
                }
            }

            else -> {
                processNormalText(rawText, builder, textContext, highlight, themeColors, onWordPress, options, wordHighlights)
            }
        }
    }

    private fun isEncircled(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length != 1) return false
        val code = trimmed[0].code
        return code in 0x2460..0x24FF
    }

    private fun isValidStrongsNumber(text: String): Boolean {
        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> false
            trimmed.matches(Regex("""\d+""")) -> true
            trimmed.matches(Regex("""[GH]\d+""", RegexOption.IGNORE_CASE)) -> true
            else -> false
        }
    }

    private fun formatStrongsNumber(text: String, isOldTestament: Boolean): String {
        val trimmed = text.trim()
        return when {
            trimmed.matches(Regex("""[GH]\d+""", RegexOption.IGNORE_CASE)) -> trimmed.uppercase()
            trimmed.firstOrNull()?.isLetter() == true -> trimmed
            else -> (if (isOldTestament) "H" else "G") + trimmed
        }
    }

    private fun processNormalText(
        text: String,
        builder: AnnotatedString.Builder,
        context: TraversalContext,
        highlight: String?,
        themeColors: ThemeColors,
        onWordPress: ((String) -> Unit)?,
        options: ProcessingOptions,
        wordHighlights: Set<String>?
    ) {
        if (options.enableWordClick && onWordPress != null) {
            val words = splitIntoWords(text, options.preserveWhitespace)
            var addSpaceAfterPunct = false
            for (word in words) {
                if (addSpaceAfterPunct && word.firstOrNull()?.isLetter() == true) {
                    builder.append(" ")
                }
                if (isWord(word)) {
                    val bgColor = if (highlight != null && word.contains(highlight, ignoreCase = true)) {
                        themeColors.searchHighlightBg
                    } else if (wordHighlights != null && word.lowercase() in wordHighlights) {
                        themeColors.wordHighlightBg
                    } else {
                        Color.Transparent
                    }

                    builder.pushStringAnnotation("word", word.lowercase())
                    builder.withStyle(
                        SpanStyle(
                            color = context.textColor,
                            background = bgColor,
                            fontSize = context.baseFontSize * context.fontSizeMultiplier,
                            baselineShift = context.baselineShift ?: BaselineShift.None
                        )
                    ) {
                        builder.append(word)
                    }
                    builder.pop()
                } else {
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
                val trimmedWord = word.trim()
                addSpaceAfterPunct = (trimmedWord == "." || trimmedWord == "," || trimmedWord == ":" || trimmedWord == ";" || trimmedWord == "?" || trimmedWord == "!")
            }
        } else {
            processTextWithoutWordClick(text, builder, context, highlight, themeColors, wordHighlights)
        }
    }

    private fun processTextWithoutWordClick(
        text: String,
        builder: AnnotatedString.Builder,
        context: TraversalContext,
        highlight: String?,
        themeColors: ThemeColors,
        wordHighlights: Set<String>?
    ) {
        if (highlight != null && text.contains(highlight, ignoreCase = true)) {
            val escapedHighlight = escapeRegex(highlight)
            val regex = try {
                Regex(escapedHighlight, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                return
            }
            var lastIndex = 0
            for (match in regex.findAll(text)) {
                if (match.range.first > lastIndex) {
                    val segment = text.substring(lastIndex, match.range.first)
                    appendWithWordHighlights(segment, builder, context, wordHighlights, themeColors)
                }
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
            if (lastIndex < text.length) {
                val remaining = text.substring(lastIndex)
                appendWithWordHighlights(remaining, builder, context, wordHighlights, themeColors)
            }
        } else {
            appendWithWordHighlights(text, builder, context, wordHighlights, themeColors)
        }
    }

    private fun appendWithWordHighlights(
        text: String,
        builder: AnnotatedString.Builder,
        context: TraversalContext,
        wordHighlights: Set<String>?,
        themeColors: ThemeColors
    ) {
        if (wordHighlights == null) {
            builder.withStyle(
                SpanStyle(
                    color = context.textColor,
                    fontSize = context.baseFontSize * context.fontSizeMultiplier,
                    baselineShift = context.baselineShift ?: BaselineShift.None
                )
            ) {
                builder.append(text)
            }
            return
        }

        val words = splitIntoWords(text, preserveWhitespace = false)
        for (word in words) {
            val bgColor = if (isWord(word) && word.lowercase() in wordHighlights) {
                themeColors.wordHighlightBg
            } else {
                Color.Transparent
            }
            builder.withStyle(
                SpanStyle(
                    color = context.textColor,
                    background = bgColor,
                    fontSize = context.baseFontSize * context.fontSizeMultiplier,
                    baselineShift = context.baselineShift ?: BaselineShift.None
                )
            ) {
                builder.append(word)
            }
            val trimmedWord = word.trim()
            if (trimmedWord != "." && trimmedWord != "," && trimmedWord != ":" && trimmedWord != ";" && trimmedWord != "?" && trimmedWord != "!" && trimmedWord != word) {
                builder.append(" ")
            }
        }
    }

    private fun processSelfClosingTagNode(
        node: TreeNode.SelfClosingTag,
        builder: AnnotatedString.Builder,
        context: TraversalContext,
        themeColors: ThemeColors
    ) {
        val content = extractContentFromTag(node.fullTag)
        val trimmedContent = content.trim()
        if (trimmedContent.isNotEmpty()) {
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
    }

    private fun extractContentFromTag(tag: String): String {
        val match = ATTRIBUTE_REGEX.find(tag)
        return match?.groupValues?.get(2) ?: ""
    }

    private fun splitIntoWords(text: String, preserveWhitespace: Boolean): List<String> {
        return if (preserveWhitespace) {
            text.split(Regex("(?<=\\S)(?=\\s)|(?<=\\s)(?=\\S)"))
        } else {
            text.split(Regex("(?<=\\w)(?=\\W)|(?<=\\W)(?=\\w)|\\s+"))
                .filter { it.isNotEmpty() }
        }
    }

    private fun isWord(text: String): Boolean {
        return text.matches(Regex("""[a-zA-ZÀ-ÿ']+"""))
    }

    private fun escapeRegex(string: String): String {
        return string.replace(ESCAPE_REGEX_PATTERN, "\\$0")
    }
}

object SimpleVerseProcessor {
    fun stripXmlTags(text: String): String {
        if (text.isEmpty()) return ""
        var processedText = text
        processedText = processedText.replace(
            Regex("""<f[^>]*>.*?</f>""", RegexOption.DOT_MATCHES_ALL),
            ""
        )
        processedText = processedText.replace(
            Regex("""<S[^>]*>.*?</S>""", RegexOption.DOT_MATCHES_ALL),
            ""
        )
        processedText = processedText.replace(Regex("""<[^>]+>"""), "")
        processedText = processedText.replace(Regex("""\s+"""), " ")
        return processedText.trim()
    }

    fun extractVerseReference(verses: List<com.fountofhopedotorg.fohbible.data.Verse>): String {
        if (verses.isEmpty()) return ""
        val first = verses.first()
        return if (verses.size == 1) {
            "${first.bookName ?: ""} ${first.chapter}:${first.verseNumber}"
        } else {
            "${first.bookName ?: ""} ${first.chapter}:${first.verseNumber}-${verses.last().verseNumber}"
        }
    }
}