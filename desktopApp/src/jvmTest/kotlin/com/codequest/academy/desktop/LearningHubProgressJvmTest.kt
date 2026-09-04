package com.codequest.academy.desktop

import com.codequest.academy.shared.learning.LearningHubProgress
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearningHubProgressJvmTest {
    @Test
    fun learnerStateAndMasteryPersistLocally() {
        val db = File(Files.createTempDirectory("nous-progress-test").toFile(), "profile.db")
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath.replace("\\", "/")}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE TABLE ActiveSession(session_id INTEGER PRIMARY KEY, user_id TEXT NOT NULL, updated_at INTEGER NOT NULL)")
                statement.executeUpdate("INSERT INTO ActiveSession VALUES(1, 'learner-1', 1)")
                statement.executeUpdate("CREATE TABLE LearningHubProgress(user_id TEXT NOT NULL, section_id TEXT NOT NULL, lesson_id TEXT, best_practice_score REAL, attempts INTEGER NOT NULL DEFAULT 0, completed INTEGER NOT NULL DEFAULT 0, last_opened_at INTEGER, time_spent_seconds INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(user_id, section_id, lesson_id))")
            }
        }
        LearningHubProgress.initialize(db.absolutePath)
        LearningHubProgress.recordAttempt("S02-L01", 0.75)
        LearningHubProgress.markArticleProgress("S02-L01", 8)
        LearningHubProgress.toggleReviewItem("S02-L01", 3)
        LearningHubProgress.saveNote("S02-L01", "Check boundary types")
        LearningHubProgress.toggleBookmark("S02-L01")
        LearningHubProgress.recordQuizAttempt("S02-L01", 16, 20, listOf("NAA-02-01-081"))
        LearningHubProgress.refresh()
        val progress = LearningHubProgress.state.value.getValue("S02-L01")
        assertEquals(8, progress.articleUnitsRead)
        assertTrue(3 in progress.reviewItems)
        assertEquals("Check boundary types", progress.note)
        assertTrue(progress.bookmarked)
        assertEquals(16, progress.bestQuizCorrect)
        assertTrue(progress.completed)
        assertTrue(progress.attempts >= 2)
    }
}
