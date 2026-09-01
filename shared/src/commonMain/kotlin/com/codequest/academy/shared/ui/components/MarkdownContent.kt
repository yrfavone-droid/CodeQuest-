package com.codequest.academy.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme

private val MarkdownAccent = Color(0xFFEE6A36)

/**
 * Small, dependency-free Markdown presenter for bundled lesson content.
 * The lesson files remain Markdown on disk, but learners see formatted text
 * rather than source markers such as #, ##, or **bold**.
 */
@Composable
fun MarkdownContent(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parseMarkdown(markdown).forEach { block ->
            when (block.kind) {
                MarkdownKind.HEADING -> Text(
                    block.text,
                    style = AppTypography.h2.copy(fontWeight = FontWeight.Bold),
                    color = Theme.colors.textPrimary
                )
                MarkdownKind.BULLET -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = AppTypography.body1, color = MarkdownAccent)
                    Text(block.text, style = AppTypography.body2, color = Theme.colors.textSecondary)
                }
                MarkdownKind.CODE -> Text(
                    block.text,
                    style = AppTypography.body2,
                    color = Theme.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                MarkdownKind.PARAGRAPH -> Text(
                    block.text,
                    style = AppTypography.body2,
                    color = Theme.colors.textSecondary
                )
            }
        }
    }
}

internal enum class MarkdownKind { HEADING, BULLET, CODE, PARAGRAPH }

internal data class MarkdownBlock(val kind: MarkdownKind, val text: String)

internal fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    var inCode = false
    return markdown.lineSequence().mapNotNull { rawLine ->
        val line = rawLine.trim()
        if (line.startsWith("```")) {
            inCode = !inCode
            return@mapNotNull null
        }
        if (line.isBlank()) return@mapNotNull null

        if (inCode) return@mapNotNull MarkdownBlock(MarkdownKind.CODE, line)

        val heading = Regex("^#{1,6}\\s+").replace(line, "")
        if (heading != line) return@mapNotNull MarkdownBlock(MarkdownKind.HEADING, cleanInlineMarkdown(heading))

        val bullet = Regex("^(?:[-*+]\\s+|\\d+[.)]\\s+)").replace(line, "")
        if (bullet != line) return@mapNotNull MarkdownBlock(MarkdownKind.BULLET, cleanInlineMarkdown(bullet))

        MarkdownBlock(MarkdownKind.PARAGRAPH, cleanInlineMarkdown(line))
    }.toList()
}

private fun cleanInlineMarkdown(value: String): String = value
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("__(.+?)__"), "$1")
    .replace(Regex("`(.+?)`"), "$1")
    .replace(Regex("\\[(.+?)]\\([^)]*\\)"), "$1")
    .replace(Regex("~~(.+?)~~"), "$1")
    .trim()
