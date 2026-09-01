package com.codequest.academy.shared.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearningHubCatalogTest {
    @Test
    fun catalogContainsExactlyTwentyStableShells() {
        val sections = LearningHubCatalog.sections
        assertEquals(20, sections.size)
        assertEquals((1..20).map { "HUB-${it.toString().padStart(2, '0')}" }, sections.map { it.id })
        assertEquals((1..20).toList(), sections.map { it.number })
        assertTrue(sections.all { it.title.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun shellsDoNotAdvertiseUnimportedCountsOrProgress() {
        LearningHubCatalog.sections.forEach { section ->
            assertTrue(!section.description.contains("lesson", ignoreCase = true))
            assertTrue(!section.description.contains("question", ignoreCase = true))
        }
    }
}
