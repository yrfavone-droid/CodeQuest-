package com.codequest.academy.shared

import androidx.compose.runtime.*
import com.codequest.academy.shared.data.CurriculumFileReader
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.AppShell
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.screens.*
import com.codequest.academy.shared.ui.theme.CodeQuestTheme
import com.codequest.academy.shared.ui.viewmodels.*

@Composable
fun App(fileReader: CurriculumFileReader, progressRepository: ProgressRepository) {
    val navigation = remember { Navigation() }
    val appViewModel = rememberViewModel { AppViewModel() }
    val isRailExpanded by appViewModel.isRailExpanded.collectAsState()

    CodeQuestTheme {
        AppShell(navigation, isRailExpanded, appViewModel::toggleRail) {
            val screen = navigation.currentScreen
            val authScreen = screen == Screen.CurriculumLoading || screen == Screen.CreateAccount || screen == Screen.SignIn || screen == Screen.LegacyCredentialSetup
            if (!authScreen && !progressRepository.hasActiveSession()) {
                LaunchedEffect(screen) {
                    navigation.resetTo(if (progressRepository.hasAnyProfiles()) Screen.SignIn else Screen.CreateAccount)
                }
            } else when (screen) {
                Screen.CurriculumLoading -> CurriculumLoadingScreen(navigation, fileReader, progressRepository)
                Screen.CreateAccount -> CreateAccountScreen(navigation, progressRepository)
                Screen.SignIn -> SignInScreen(navigation, progressRepository)
                Screen.LegacyCredentialSetup -> LegacyCredentialSetupScreen(navigation, progressRepository)
                Screen.Dashboard -> DashboardScreen(navigation, rememberViewModel { DashboardViewModel(progressRepository) })
                Screen.TrackBrowser -> TrackBrowserScreen(navigation, rememberViewModel { TrackBrowserViewModel(progressRepository) })
                is Screen.TrackDetails -> TrackDetailsScreen(navigation, rememberViewModel(screen.trackId) { TrackDetailsViewModel(screen.trackId, progressRepository) })
                is Screen.PathDetails -> PathDetailsScreen(navigation, rememberViewModel(screen.pathId) { PathDetailsViewModel(screen.pathId, progressRepository) })
                is Screen.LevelOverview -> LevelOverviewScreen(navigation, rememberViewModel(screen.levelId) { LevelOverviewViewModel(screen.levelId, progressRepository) })
                is Screen.LearningMap -> LevelOverviewScreen(navigation, rememberViewModel(screen.levelId) { LevelOverviewViewModel(screen.levelId, progressRepository) })
                is Screen.Diagnostic -> DiagnosticScreen(navigation, rememberViewModel(screen.levelId, screen.diagnosticId) { DiagnosticViewModel(screen.levelId, screen.diagnosticId, progressRepository) }, screen.levelId)
                is Screen.CheatSheet -> DocumentNodeScreen(navigation, rememberViewModel(screen.levelId, screen.cheatSheetId) { DocumentNodeViewModel(screen.levelId, screen.cheatSheetId, "cheat_sheet", progressRepository) }, screen.levelId, "Cheat Sheet")
                is Screen.Lesson -> LessonScreen(navigation, rememberViewModel(screen.levelId, screen.lessonId) { LessonViewModel(screen.levelId, screen.lessonId, progressRepository) }, screen.levelId)
                is Screen.Practice -> PracticeScreen(navigation, rememberViewModel(screen.levelId, screen.practiceId) { PracticeViewModel(screen.levelId, screen.practiceId, progressRepository) }, screen.levelId)
                is Screen.Challenge -> ChallengeScreen(navigation, rememberViewModel(screen.levelId, screen.challengeId) { ChallengeViewModel(screen.levelId, screen.challengeId, progressRepository) }, screen.levelId)
                is Screen.MixedReview -> MixedReviewScreen(navigation, rememberViewModel(screen.levelId, screen.reviewId) { MixedReviewViewModel(screen.levelId, screen.reviewId, progressRepository) }, screen.levelId)
                is Screen.AdaptiveReview -> AssessmentScreen(navigation, rememberViewModel(screen.levelId, screen.reviewId) { AdaptiveReviewViewModel(screen.levelId, screen.reviewId, progressRepository) }, screen.levelId, "Adaptive Review", com.codequest.academy.shared.ui.theme.Theme.colors.information)
                is Screen.FinalQuiz -> FinalQuizScreen(navigation, rememberViewModel(screen.levelId, screen.quizId) { FinalQuizViewModel(screen.levelId, screen.quizId, progressRepository) }, screen.levelId)
                is Screen.Project -> ProjectScreen(navigation, rememberViewModel(screen.levelId, screen.projectId) { ProjectViewModel(screen.levelId, screen.projectId, progressRepository) }, screen.levelId)
                is Screen.ProjectReflection -> DocumentNodeScreen(navigation, rememberViewModel(screen.levelId, screen.reflectionId) { DocumentNodeViewModel(screen.levelId, screen.reflectionId, "project_reflection", progressRepository) }, screen.levelId, "Project Reflection")
                is Screen.MasteryChallenge -> DocumentNodeScreen(navigation, rememberViewModel(screen.levelId, screen.masteryId) { DocumentNodeViewModel(screen.levelId, screen.masteryId, "optional_mastery_challenge", progressRepository) }, screen.levelId, "Optional Mastery Challenge")
                Screen.Projects -> ProjectsScreen(navigation, progressRepository)
                Screen.CodeEditor -> CodeEditorScreen(navigation, progressRepository)
                Screen.Progress -> ProgressScreen(navigation, progressRepository)
                Screen.Review -> ReviewScreen(navigation, progressRepository)
                Screen.Profile -> ProfileScreen(navigation, progressRepository)
                Screen.ChangePassword -> ChangePasswordScreen(navigation, progressRepository)
                Screen.Settings -> SettingsScreen(navigation, progressRepository)
                is Screen.PathCapstone -> LockedCapstoneScreen("Path Capstone", "Complete Level 5’s project and Project Reflection to unlock this capstone.", navigation)
                is Screen.TrackFinalProject -> LockedCapstoneScreen("Track Final Project", "Complete both path capstones to unlock the integrated track final project.", navigation)
                is Screen.QuestionActivity -> ContentNotFoundState("Question not found", "Question '${screen.questionId}' is not available from this destination.", navigation::pop) { navigation.resetTo(Screen.Dashboard) }
                is Screen.ProjectWorkspace -> ContentNotFoundState("Project not found", "Project '${screen.projectId}' is not available from this destination.", navigation::pop) { navigation.resetTo(Screen.Dashboard) }
                is Screen.QuizResult -> ContentNotFoundState("Quiz result not found", "Attempt '${screen.attemptId}' is not available. Quiz results are shown when an attempt is submitted.", navigation::pop) { navigation.resetTo(Screen.Dashboard) }
            }
        }
    }
}
