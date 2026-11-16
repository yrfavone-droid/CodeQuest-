package com.codequest.academy.shared.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MarkdownContentTest {
    @Test
    fun rendersMarkdownAsReadableBlocks() {
        val blocks = parseMarkdown(
            """# Objective
            **Distinguish** the terms.
            - Machine learning
            ```
            model.fit(data)
            ```""".trimIndent()
        )

        assertEquals(MarkdownKind.HEADING, blocks[0].kind)
        assertEquals("Objective", blocks[0].text)
        assertEquals("Distinguish the terms.", blocks[1].text)
        assertEquals(MarkdownKind.BULLET, blocks[2].kind)
        assertEquals(MarkdownKind.CODE, blocks[3].kind)
        assertEquals("model.fit(data)", blocks[3].text)
        assertFalse(blocks.any { it.text.contains("**") || it.text.startsWith("#") })
    }

    @Test
    fun preservesMultilineCodeBlockWithIndentation() {
        val snippet = """
            # Code Sample
            ```python
            def calculate_mean(values):
                total = sum(values)
                return total / len(values)
            ```
        """.trimIndent()

        val blocks = parseMarkdown(snippet)
        assertEquals(2, blocks.size)
        assertEquals(MarkdownKind.HEADING, blocks[0].kind)
        assertEquals(MarkdownKind.CODE, blocks[1].kind)
        val expectedCode = "def calculate_mean(values):\n    total = sum(values)\n    return total / len(values)"
        assertEquals(expectedCode, blocks[1].text)
    }
}
