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
        assertFalse(blocks.any { it.text.contains("**") || it.text.startsWith("#") })
    }
}
