package com.madebykunal.doon.lib

import java.util.regex.Pattern

sealed interface InlineNode {
    data class Text(val text: String) : InlineNode
    data class Styled(val style: Style, val children: List<InlineNode>) : InlineNode
    data class Code(val text: String) : InlineNode
    data class Link(val label: String, val url: String) : InlineNode

    enum class Style { Bold, Italic, Strike }
}

object Markdown {
    private val urlPattern = Pattern.compile("https?://[^\\s<>()]+[^\\s<>().,;:!?'\"]")
    private val headingPattern = Regex("^#\\s+(.+)$")
    private val safeUrl = Regex("^https?://", RegexOption.IGNORE_CASE)

    fun isSafeUrl(url: String): Boolean = safeUrl.containsMatchIn(url)

    fun parseInline(source: String): List<InlineNode> {
        val nodes = mutableListOf<InlineNode>()
        val buffer = StringBuilder()
        var i = 0

        fun flush() {
            if (buffer.isNotEmpty()) nodes.add(InlineNode.Text(buffer.toString()))
            buffer.clear()
        }

        fun wrapped(marker: String, style: InlineNode.Style): Boolean {
            if (!source.startsWith(marker, i)) return false
            val end = source.indexOf(marker, i + marker.length)
            if (end <= i + marker.length) return false
            flush()
            nodes.add(InlineNode.Styled(style, parseInline(source.substring(i + marker.length, end))))
            i = end + marker.length
            return true
        }

        while (i < source.length) {
            val c = source[i]
            if (c == '*' && (wrapped("**", InlineNode.Style.Bold) || wrapped("*", InlineNode.Style.Italic))) continue
            if (c == '~' && wrapped("~~", InlineNode.Style.Strike)) continue

            if (c == '`') {
                val end = source.indexOf('`', i + 1)
                if (end > i + 1) {
                    flush()
                    nodes.add(InlineNode.Code(source.substring(i + 1, end)))
                    i = end + 1
                    continue
                }
            }

            if (c == '[') {
                val close = source.indexOf("](", i + 1)
                val end = if (close == -1) -1 else source.indexOf(')', close + 2)
                if (close > i + 1 && end != -1) {
                    val url = source.substring(close + 2, end).trim()
                    if (isSafeUrl(url)) {
                        flush()
                        nodes.add(InlineNode.Link(source.substring(i + 1, close), url))
                        i = end + 1
                        continue
                    }
                }
            }

            if (c == 'h' && (i == 0 || source[i - 1].isWhitespace())) {
                val matcher = urlPattern.matcher(source).region(i, source.length)
                if (matcher.lookingAt()) {
                    val url = matcher.group()
                    flush()
                    nodes.add(InlineNode.Link(url, url))
                    i += url.length
                    continue
                }
            }

            buffer.append(c)
            i++
        }
        flush()
        return nodes
    }

    fun plainText(source: String): String {
        fun walk(nodes: List<InlineNode>): String = nodes.joinToString("") {
            when (it) {
                is InlineNode.Text -> it.text
                is InlineNode.Code -> it.text
                is InlineNode.Link -> it.label
                is InlineNode.Styled -> walk(it.children)
            }
        }
        return walk(parseInline(source))
    }

    fun headingText(source: String): String? = headingPattern.find(source)?.groupValues?.get(1)?.trim()
}
