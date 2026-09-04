package com.codequest.academy.shared.learning

import kotlinx.coroutines.flow.StateFlow

data class LearningHubLessonProgress(
    val lessonId: String,
    val bestPracticeScore: Double = 0.0,
    val attempts: Int = 0,
    val completed: Boolean = false,
    val bestQuizCorrect: Int = 0,
    val quizAttempts: Int = 0,
    val articleUnitsRead: Int = 0,
    val reviewItems: Set<Int> = emptySet(),
    val note: String = "",
    val bookmarked: Boolean = false
)

expect object LearningHubProgress {
    val state: StateFlow<Map<String, LearningHubLessonProgress>>
    fun initialize(databasePath: String)
    fun recordAttempt(lessonId: String, score: Double, completed: Boolean = false)
    fun recordQuizAttempt(lessonId: String, correct: Int, total: Int, incorrectIds: List<String>)
    fun markArticleProgress(lessonId: String, unitsRead: Int)
    fun toggleReviewItem(lessonId: String, item: Int)
    fun saveNote(lessonId: String, note: String)
    fun toggleBookmark(lessonId: String)
    fun refresh()
}
