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
            val allLessons = state.curriculum!!.sections.flatMap { it.lessons }
            assertEquals(100, allLessons.size)
            assertTrue(allLessons.all { LearningHubContent.lessonReview(it).isNotBlank() })
            assertTrue(allLessons.filterNot { it.sectionId == "S01" }.all { LearningHubContent.lessonQuiz(it).size == 20 })
            val lesson = state.curriculum!!.sections.first().lessons.first()
            assertTrue(LearningHubContent.lessonMarkdown(lesson).isNotBlank())
            assertEquals(10, LearningHubContent.firstProblems(lesson).size)
            assertTrue(LearningHubContent.sectionPdfPath(state.curriculum!!.sections.first())?.endsWith(".pdf") == true)
            assertEquals(8, LearningHubContent.section1ArticleBlocks("S01-L01").count { it.type == "heading" })
            assertEquals(8, LearningHubContent.section1ArticleBlocks("S01-L01").count { it.type == "knowledge_check" })
            assertEquals(10, LearningHubContent.section1Problems("S01-L01").size)
            assertEquals(20, LearningHubContent.section1Problems("S01-L01", quiz = true, limit = 20).size)
            assertEquals(12, LearningHubContent.section1Problems("S01-L01", quiz = true, limit = 20).count { it.answerType == "multiple_choice" })
            assertTrue(LearningHubContent.section1Review("S01-L01").isNotBlank())
            assertEquals(500, (1..5).sumOf { n -> LearningHubContent.section1Problems("S01-L${n.toString().padStart(2, '0')}", limit = 100).size + LearningHubContent.section1Problems("S01-L${n.toString().padStart(2, '0')}", quiz = true, limit = 100).size })
            assertTrue(LearningHubContent.section1Lesson("S01-L01")!!.pdfPath.endsWith(".pdf"))
            LearningHubContent.saveSection1Note("S01-L01", "offline note")
            assertEquals("offline note", LearningHubContent.section1Note("S01-L01"))
            val savedPdf = File(home, "S01-L01.pdf")
            assertTrue(LearningHubContent.saveSection1Pdf("S01-L01", savedPdf.absolutePath))
            assertTrue(savedPdf.length() > 100_000)
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
