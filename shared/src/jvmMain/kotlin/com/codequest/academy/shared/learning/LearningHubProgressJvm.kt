package com.codequest.academy.shared.learning

import java.io.File
import java.sql.DriverManager

actual object LearningHubProgress {
    private var databasePath: String? = null
    actual fun initialize(databasePath: String) { this.databasePath = databasePath }
    actual fun recordAttempt(lessonId: String, score: Double, completed: Boolean) {
        val path = databasePath ?: return
        runCatching {
            DriverManager.getConnection("jdbc:sqlite:${File(path).absolutePath.replace("\\", "/")}").use { connection ->
                connection.prepareStatement("SELECT user_id FROM ActiveSession WHERE session_id = 1").use { userQuery ->
                    userQuery.executeQuery().use { users ->
                        if (!users.next()) return
                        val userId = users.getString(1)
                        val sectionId = lessonId.substringBefore('-')
                        connection.prepareStatement("""
                            INSERT INTO LearningHubProgress(user_id, section_id, lesson_id, best_practice_score, attempts, completed, last_opened_at, time_spent_seconds)
                            VALUES (?, ?, ?, ?, 1, ?, ?, 0)
                            ON CONFLICT(user_id, section_id, lesson_id) DO UPDATE SET
                              best_practice_score = MAX(COALESCE(LearningHubProgress.best_practice_score, 0), excluded.best_practice_score),
                              attempts = LearningHubProgress.attempts + 1,
                              completed = MAX(LearningHubProgress.completed, excluded.completed),
                              last_opened_at = excluded.last_opened_at
                        """.trimIndent()).use { statement ->
                            statement.setString(1, userId); statement.setString(2, sectionId); statement.setString(3, lessonId)
                            statement.setDouble(4, score); statement.setInt(5, if (completed || score >= 0.8) 1 else 0); statement.setLong(6, System.currentTimeMillis()); statement.executeUpdate()
                        }
                    }
                }
            }
        }
    }
}
