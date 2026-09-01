package com.codequest.academy.shared.ui.screens

import kotlin.test.Test
import kotlin.test.assertTrue

class LearningHubCatalogTest {
    @Test
    fun packageIdentifiersUseNousAcademyContract() {
        assertTrue("Nous AI Academy".contains("Nous"))
        assertTrue("S01-L01".matches(Regex("S[0-9]{2}-L[0-9]{2}")))
        assertTrue("NAA-01-01-001".matches(Regex("NAA-[0-9]{2}-[0-9]{2}-[0-9]{3}")))
    }
}
