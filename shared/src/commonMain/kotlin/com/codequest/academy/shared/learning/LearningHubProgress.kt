package com.codequest.academy.shared.learning

expect object LearningHubProgress {
    fun initialize(databasePath: String)
    fun recordAttempt(lessonId: String, score: Double, completed: Boolean = false)
}
