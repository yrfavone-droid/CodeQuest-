package com.codequest.academy.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.CodeStyle
import com.codequest.academy.shared.ui.theme.Theme

private val MarkdownAccent: Color
    @Composable get() = Theme.colors.brandPrimary

/**
 * Clean, robust Markdown presenter for bundled lesson content.
 * Parses headings, bullet points, paragraphs, and multi-line formatted code blocks
 * with monospace typography and selection support.
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
                MarkdownKind.CODE -> Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Theme.colors.surfaceSecondary,
                    border = BorderStroke(1.dp, Theme.colors.borderDefault)
                ) {
                    Box(Modifier.padding(14.dp).horizontalScroll(rememberScrollState())) {
                        SelectionContainer {
                            Text(
                                text = block.text,
                                style = CodeStyle,
                                color = Theme.colors.textPrimary
                            )
                        }
                    }
                }
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
    val blocks = mutableListOf<MarkdownBlock>()
    var inCode = false
    val codeLines = mutableListOf<String>()

    for (rawLine in markdown.lineSequence()) {
        val trimmed = rawLine.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                blocks.add(MarkdownBlock(MarkdownKind.CODE, codeLines.joinToString("\n").trimIndent()))
                codeLines.clear()
                inCode = false
            } else {
                inCode = true
                codeLines.clear()
            }
            continue
        }

        if (inCode) {
            codeLines.add(rawLine.trimEnd('\r', '\n'))
            continue
        }

        if (trimmed.isBlank()) continue

        val heading = Regex("^#{1,6}\\s+").replace(trimmed, "")
        if (heading != trimmed) {
            blocks.add(MarkdownBlock(MarkdownKind.HEADING, cleanInlineMarkdown(heading)))
            continue
        }

        val bullet = Regex("^(?:[-*+]\\s+|\\d+[.)]\\s+)").replace(trimmed, "")
        if (bullet != trimmed) {
            blocks.add(MarkdownBlock(MarkdownKind.BULLET, cleanInlineMarkdown(bullet)))
            continue
        }

        blocks.add(MarkdownBlock(MarkdownKind.PARAGRAPH, cleanInlineMarkdown(trimmed)))
    }

    if (inCode && codeLines.isNotEmpty()) {
        blocks.add(MarkdownBlock(MarkdownKind.CODE, codeLines.joinToString("\n").trimIndent()))
    }

    return blocks
}

private fun cleanInlineMarkdown(value: String): String = value
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("__(.+?)__"), "$1")
    .replace(Regex("`(.+?)`"), "$1")
    .replace(Regex("\\[(.+?)]\\([^)]*\\)"), "$1")
    .replace(Regex("~~(.+?)~~"), "$1")
    .trim()
