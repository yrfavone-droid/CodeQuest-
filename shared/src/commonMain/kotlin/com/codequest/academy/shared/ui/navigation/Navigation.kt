package com.codequest.academy.shared.ui.navigation

import androidx.compose.runtime.*

sealed class Screen {
    object Dashboard : Screen()
    object TrackBrowser : Screen()
    data class TrackDetails(val trackId: String) : Screen()
    data class PathDetails(val pathId: String) : Screen()
    data class LevelOverview(val levelId: String) : Screen()
    data class QuestionActivity(val questionId: String) : Screen()
    data class ProjectWorkspace(val projectId: String) : Screen()
    object CreateAccount : Screen()
    object SignIn : Screen()
    object LegacyCredentialSetup : Screen()
    object ChangePassword : Screen()
    data class LearningMap(val levelId: String) : Screen()
    data class Diagnostic(val levelId: String, val diagnosticId: String) : Screen()
    data class CheatSheet(val levelId: String, val cheatSheetId: String) : Screen()
    data class Lesson(val levelId: String, val lessonId: String) : Screen()
    data class Practice(val levelId: String, val practiceId: String) : Screen()
    data class Challenge(val levelId: String, val challengeId: String) : Screen()
    data class MixedReview(val levelId: String, val reviewId: String) : Screen()
    data class AdaptiveReview(val levelId: String, val reviewId: String) : Screen()
    data class FinalQuiz(val levelId: String, val quizId: String) : Screen()
    data class QuizResult(val attemptId: String) : Screen()
    data class Project(val levelId: String, val projectId: String) : Screen()
    data class ProjectReflection(val levelId: String, val reflectionId: String) : Screen()
    data class MasteryChallenge(val levelId: String, val masteryId: String) : Screen()
    data class PathCapstone(val pathId: String, val capstoneId: String = pathId) : Screen()
    data class TrackFinalProject(val trackId: String, val trackFinalId: String = trackId) : Screen()
    object Review : Screen()
    object Projects : Screen()
    object CodeEditor : Screen()
    object Progress : Screen()
    object Profile : Screen()
    object Settings : Screen()
    object CurriculumLoading : Screen()
}

class Navigation {
    var backStack by mutableStateOf(listOf<Screen>(Screen.CurriculumLoading))
        private set

    val currentScreen: Screen
        get() = backStack.last()

    fun navigateTo(screen: Screen) {
        if (backStack.lastOrNull() != screen) {
            backStack = backStack + screen
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    fun resetTo(screen: Screen) {
        backStack = listOf(screen)
    }
}
