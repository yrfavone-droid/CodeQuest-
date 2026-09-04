package com.codequest.academy.shared.learning

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

actual object LearningHubProgress {
    private val mutableState = MutableStateFlow<Map<String, LearningHubLessonProgress>>(emptyMap())
    actual val state: StateFlow<Map<String, LearningHubLessonProgress>> = mutableState
    private var databasePath: String? = null

    @Synchronized
    actual fun initialize(databasePath: String) {
        this.databasePath = databasePath
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS LearningHubQuizAttempt (
                      id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT NOT NULL, lesson_id TEXT NOT NULL,
                      correct_count INTEGER NOT NULL, total_count INTEGER NOT NULL,
                      incorrect_ids TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL
                    )
                """.trimIndent())
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS LearningHubLessonState (
                      user_id TEXT NOT NULL, lesson_id TEXT NOT NULL,
                      article_units_read INTEGER NOT NULL DEFAULT 0, review_items TEXT NOT NULL DEFAULT '',
                      note TEXT NOT NULL DEFAULT '', bookmarked INTEGER NOT NULL DEFAULT 0,
                      updated_at INTEGER NOT NULL, PRIMARY KEY (user_id, lesson_id)
                    )
                """.trimIndent())
            }
        }
        refresh()
    }

    @Synchronized
    actual fun recordAttempt(lessonId: String, score: Double, completed: Boolean) {
        val userId = activeUserId() ?: return
        connection().use { connection -> upsertProgress(connection, userId, lessonId, score.coerceIn(0.0, 1.0), completed) }
        refresh()
    }

    @Synchronized
    actual fun recordQuizAttempt(lessonId: String, correct: Int, total: Int, incorrectIds: List<String>) {
        require(total > 0 && correct in 0..total)
        val userId = activeUserId() ?: return
        connection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement("INSERT INTO LearningHubQuizAttempt(user_id, lesson_id, correct_count, total_count, incorrect_ids, created_at) VALUES (?, ?, ?, ?, ?, ?)").use { statement ->
                    statement.setString(1, userId); statement.setString(2, lessonId); statement.setInt(3, correct); statement.setInt(4, total)
                    statement.setString(5, incorrectIds.joinToString(",")); statement.setLong(6, System.currentTimeMillis()); statement.executeUpdate()
                }
                upsertProgress(connection, userId, lessonId, correct.toDouble() / total, correct * 5 >= total * 4)
                connection.commit()
            } catch (failure: Throwable) { connection.rollback(); throw failure }
        }
        refresh()
    }

    @Synchronized
    actual fun markArticleProgress(lessonId: String, unitsRead: Int) {
        updateLessonState(lessonId) { it.copy(articleUnitsRead = maxOf(it.articleUnitsRead, unitsRead.coerceIn(0, 8))) }
    }

    @Synchronized
    actual fun toggleReviewItem(lessonId: String, item: Int) {
        require(item in 1..8)
        updateLessonState(lessonId) { current ->
            val items = current.reviewItems.toMutableSet().apply { if (!add(item)) remove(item) }
            current.copy(reviewItems = items)
        }
    }

    @Synchronized
    actual fun saveNote(lessonId: String, note: String) = updateLessonState(lessonId) { it.copy(note = note) }

    @Synchronized
    actual fun toggleBookmark(lessonId: String) = updateLessonState(lessonId) { it.copy(bookmarked = !it.bookmarked) }

    @Synchronized
    actual fun refresh() {
        val userId = activeUserId() ?: run { mutableState.value = emptyMap(); return }
        val loaded = linkedMapOf<String, LearningHubLessonProgress>()
        connection().use { connection ->
            connection.prepareStatement("""
                SELECT p.lesson_id, COALESCE(p.best_practice_score, 0), p.attempts, p.completed,
                       COALESCE(MAX(q.correct_count), 0), COUNT(q.id), COALESCE(s.article_units_read, 0),
                       COALESCE(s.review_items, ''), COALESCE(s.note, ''), COALESCE(s.bookmarked, 0)
                FROM LearningHubProgress p
                LEFT JOIN LearningHubQuizAttempt q ON q.user_id = p.user_id AND q.lesson_id = p.lesson_id
                LEFT JOIN LearningHubLessonState s ON s.user_id = p.user_id AND s.lesson_id = p.lesson_id
                WHERE p.user_id = ? AND p.lesson_id IS NOT NULL GROUP BY p.lesson_id
            """.trimIndent()).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        val lessonId = rows.getString(1)
                        loaded[lessonId] = LearningHubLessonProgress(
                            lessonId, rows.getDouble(2), rows.getInt(3), rows.getInt(4) != 0,
                            rows.getInt(5), rows.getInt(6), rows.getInt(7), parseItems(rows.getString(8)), rows.getString(9), rows.getInt(10) != 0
                        )
                    }
                }
            }
            connection.prepareStatement("SELECT lesson_id, article_units_read, review_items, note, bookmarked FROM LearningHubLessonState WHERE user_id = ?").use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        val lessonId = rows.getString(1)
                        if (lessonId !in loaded) loaded[lessonId] = LearningHubLessonProgress(
                            lessonId = lessonId, articleUnitsRead = rows.getInt(2), reviewItems = parseItems(rows.getString(3)), note = rows.getString(4), bookmarked = rows.getInt(5) != 0
                        )
                    }
                }
            }
        }
        mutableState.value = loaded
    }

    private fun updateLessonState(lessonId: String, transform: (LearningHubLessonProgress) -> LearningHubLessonProgress) {
        val userId = activeUserId() ?: return
        val updated = transform(mutableState.value[lessonId] ?: LearningHubLessonProgress(lessonId))
        connection().use { connection ->
            connection.prepareStatement("""
                INSERT INTO LearningHubLessonState(user_id, lesson_id, article_units_read, review_items, note, bookmarked, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id, lesson_id) DO UPDATE SET article_units_read = excluded.article_units_read,
                  review_items = excluded.review_items, note = excluded.note, bookmarked = excluded.bookmarked, updated_at = excluded.updated_at
            """.trimIndent()).use { statement ->
                statement.setString(1, userId); statement.setString(2, lessonId); statement.setInt(3, updated.articleUnitsRead)
                statement.setString(4, updated.reviewItems.sorted().joinToString(",")); statement.setString(5, updated.note)
                statement.setInt(6, if (updated.bookmarked) 1 else 0); statement.setLong(7, System.currentTimeMillis()); statement.executeUpdate()
            }
        }
        refresh()
    }

    private fun upsertProgress(connection: Connection, userId: String, lessonId: String, score: Double, completed: Boolean) {
        connection.prepareStatement("""
            INSERT INTO LearningHubProgress(user_id, section_id, lesson_id, best_practice_score, attempts, completed, last_opened_at, time_spent_seconds)
            VALUES (?, ?, ?, ?, 1, ?, ?, 0)
            ON CONFLICT(user_id, section_id, lesson_id) DO UPDATE SET
              best_practice_score = MAX(COALESCE(LearningHubProgress.best_practice_score, 0), excluded.best_practice_score),
              attempts = LearningHubProgress.attempts + 1, completed = MAX(LearningHubProgress.completed, excluded.completed),
              last_opened_at = excluded.last_opened_at
        """.trimIndent()).use { statement ->
            statement.setString(1, userId); statement.setString(2, lessonId.substringBefore('-')); statement.setString(3, lessonId)
            statement.setDouble(4, score); statement.setInt(5, if (completed) 1 else 0); statement.setLong(6, System.currentTimeMillis()); statement.executeUpdate()
        }
    }

    private fun parseItems(value: String): Set<Int> = value.split(',').mapNotNull(String::toIntOrNull).toSet()

    private fun activeUserId(): String? = runCatching {
        connection().use { connection -> connection.prepareStatement("SELECT user_id FROM ActiveSession WHERE session_id = 1").use { statement -> statement.executeQuery().use { if (it.next()) it.getString(1) else null } } }
    }.getOrNull()

    private fun connection(): Connection {
        val path = requireNotNull(databasePath) { "Learning Hub progress has not been initialized" }
        return DriverManager.getConnection("jdbc:sqlite:${File(path).absolutePath.replace("\\", "/")}")
    }
}
