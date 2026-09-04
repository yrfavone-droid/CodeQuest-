package com.codequest.academy.desktop

import com.codequest.academy.shared.learning.LearningHubContent
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
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
            DriverManager.getConnection("jdbc:sqlite:${db.absolutePath.replace("\\", "/")}").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate("CREATE TABLE UserProfile(user_id TEXT PRIMARY KEY, name TEXT NOT NULL)")
                    statement.executeUpdate("INSERT INTO UserProfile VALUES('existing-user', 'Existing Learner')")
                    statement.executeUpdate("CREATE TABLE AppSetting(user_id TEXT NOT NULL, setting_key TEXT NOT NULL, setting_value TEXT NOT NULL, PRIMARY KEY(user_id, setting_key))")
                    statement.executeUpdate("INSERT INTO AppSetting VALUES('existing-user', 'reduced_motion', 'true')")
                    statement.executeUpdate("CREATE TABLE AiBookmark(id TEXT PRIMARY KEY, user_id TEXT NOT NULL, content_type TEXT NOT NULL, content_id TEXT NOT NULL, location TEXT NOT NULL, created_at INTEGER NOT NULL)")
                    statement.executeUpdate("INSERT INTO AiBookmark VALUES('bookmark-1', 'existing-user', 'lesson', 'S01-L01', 'unit-2', 1)")
                }
            }
            LearningHubContent.initialize(db.absolutePath)
            val state = LearningHubContent.state.value
            assertNotNull(state.curriculum, state.error ?: "Curriculum was not loaded")
            assertEquals(20, state.curriculum!!.sections.size)
            assertEquals(100, state.curriculum!!.lessonCount)
            assertEquals(10000, state.curriculum!!.problemCount)
            assertEquals("2.0.0-sections-02-20", state.curriculumVersion)
            val allLessons = state.curriculum!!.sections.flatMap { it.lessons }
            assertEquals(100, allLessons.size)
            assertTrue(allLessons.all { LearningHubContent.lessonReview(it).isNotBlank() })
            val deepLessons = allLessons.filterNot { it.sectionId == "S01" }
            assertEquals(95, deepLessons.size)
            assertTrue(deepLessons.all { LearningHubContent.lessonArticleBlocks(it).count { block -> block.type == "heading" } == 8 })
            assertTrue(deepLessons.all { LearningHubContent.allPractice(it).size == 80 })
            assertTrue(deepLessons.all { LearningHubContent.lessonQuiz(it).size == 20 })
            assertEquals(9_500, deepLessons.sumOf { LearningHubContent.allPractice(it).size + LearningHubContent.lessonQuiz(it).size })
            assertEquals(9_500, deepLessons.flatMap { LearningHubContent.allPractice(it) + LearningHubContent.lessonQuiz(it) }.map { it.id }.toSet().size)
            assertTrue(deepLessons.all { LearningHubContent.lessonPdfPath(it)?.endsWith(".pdf") == true })
            assertTrue(state.curriculum!!.sections.drop(1).all { LearningHubContent.sectionPdfPath(it)?.endsWith(".pdf") == true })
            assertTrue(LearningHubContent.searchLessons("variables and types").any { it.lessonId == "S02-L01" })
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
            val deepPdf = File(home, "S02-L01.pdf")
            val deepLesson = state.curriculum!!.sections[1].lessons.first()
            assertTrue(LearningHubContent.lessonPdfPath(deepLesson)?.let { File(it).copyTo(deepPdf); true } == true)
            assertEquals(File(LearningHubContent.lessonPdfPath(deepLesson)!!).length(), deepPdf.length())
            val pointer = File(home, ".nous-ai-academy/learning-hub/active-version")
            val before = pointer.readText()
            val corrupt = File(home, "corrupt.zip")
            corrupt.writeBytes(byteArrayOf(0x13, 0x37, 0x00, 0x01))
            assertTrue(!LearningHubContent.installPackage(corrupt.absolutePath, db.absolutePath))
            assertEquals(before, pointer.readText())
            val deepPointer = File(home, ".nous-ai-academy/learning-hub/sections-S02-S20-active")
            val deepBefore = deepPointer.readText()
            val corruptDeep = File(home, "Nous_AI_Academy_Sections_02_to_20_corrupt.zip").apply { writeBytes(byteArrayOf(0x50, 0x4b, 0x00, 0x01)) }
            assertTrue(!LearningHubContent.installPackage(corruptDeep.absolutePath, db.absolutePath))
            assertEquals(deepBefore, deepPointer.readText())
            DriverManager.getConnection("jdbc:sqlite:${db.absolutePath.replace("\\", "/")}").use { connection ->
                assertEquals(1, connection.createStatement().executeQuery("SELECT COUNT(*) FROM UserProfile").let { it.next(); it.getInt(1) })
                assertEquals("true", connection.createStatement().executeQuery("SELECT setting_value FROM AppSetting WHERE user_id='existing-user'").let { it.next(); it.getString(1) })
                assertEquals("S01-L01", connection.createStatement().executeQuery("SELECT content_id FROM AiBookmark WHERE id='bookmark-1'").let { it.next(); it.getString(1) })
            }
        } finally {
            if (previousHome != null) System.setProperty("user.home", previousHome)
        }
    }
}
