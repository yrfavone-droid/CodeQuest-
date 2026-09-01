package com.codequest.academy.desktop

import com.codequest.academy.shared.learning.LearningHubContent
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LearningHubContentJvmTest {
    @Test
    fun bundledPackageIsVerifiedAndExposesRealContent() {
        val home = Files.createTempDirectory("nous-learning-hub-test").toFile()
        val previousHome = System.getProperty("user.home")
        try {
            System.setProperty("user.home", home.absolutePath)
            val db = File(home, "profile.db")
            LearningHubContent.initialize(db.absolutePath)
            val state = LearningHubContent.state.value
            assertNotNull(state.curriculum)
            assertEquals(20, state.curriculum!!.sections.size)
            assertEquals(100, state.curriculum!!.lessonCount)
            assertEquals(10000, state.curriculum!!.problemCount)
            val lesson = state.curriculum!!.sections.first().lessons.first()
            assertTrue(LearningHubContent.lessonMarkdown(lesson).isNotBlank())
            assertEquals(10, LearningHubContent.firstProblems(lesson).size)
            assertTrue(LearningHubContent.sectionPdfPath(state.curriculum!!.sections.first())?.endsWith(".pdf") == true)
            val pointer = File(home, ".nous-ai-academy/learning-hub/active-version")
            val before = pointer.readText()
            val corrupt = File(home, "corrupt.zip")
            corrupt.writeBytes(byteArrayOf(0x13, 0x37, 0x00, 0x01))
            assertTrue(!LearningHubContent.installPackage(corrupt.absolutePath, db.absolutePath))
            assertEquals(before, pointer.readText())
        } finally {
            if (previousHome != null) System.setProperty("user.home", previousHome)
        }
    }
}
