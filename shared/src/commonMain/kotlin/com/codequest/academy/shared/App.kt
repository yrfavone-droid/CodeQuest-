package com.codequest.academy.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.AppShell
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.screens.CleanLibraryScreen
import com.codequest.academy.shared.ui.screens.LibraryHomeScreen
import com.codequest.academy.shared.ui.screens.LibraryListScreen
import com.codequest.academy.shared.ui.screens.SearchLibraryScreen
import com.codequest.academy.shared.ui.screens.ReadingProgressScreen
import com.codequest.academy.shared.ui.screens.BookmarksScreen
import com.codequest.academy.shared.ui.screens.LearningHubHomeScreen
import com.codequest.academy.shared.ui.screens.LearningHubSectionScreen
import com.codequest.academy.shared.ui.screens.CreateAccountScreen
import com.codequest.academy.shared.ui.screens.LegacyCredentialSetupScreen
import com.codequest.academy.shared.ui.screens.ProfileScreen
import com.codequest.academy.shared.ui.screens.SettingsScreen
import com.codequest.academy.shared.ui.screens.SignInScreen
import com.codequest.academy.shared.ui.screens.WorkspaceLoadingScreen
import com.codequest.academy.shared.ui.theme.NousTheme
import com.codequest.academy.shared.ui.viewmodels.AppViewModel
import com.codequest.academy.shared.ui.viewmodels.rememberViewModel
import com.codequest.academy.shared.documents.OfflineDocumentActions
import com.codequest.academy.shared.data.LibraryKind

@Composable
fun App(progressRepository: ProgressRepository, documentActions: OfflineDocumentActions) {
    val navigation = remember { Navigation() }
    val appViewModel = rememberViewModel { AppViewModel() }
    val isRailExpanded by appViewModel.isRailExpanded.collectAsState()
    NousTheme {
        AppShell(navigation, isRailExpanded, appViewModel::toggleRail) {
            val screen = navigation.currentScreen
            val authScreen = screen in setOf(Screen.WorkspaceLoading, Screen.CreateAccount, Screen.SignIn, Screen.LegacyCredentialSetup)
            if (!authScreen && !progressRepository.hasActiveSession()) {
                LaunchedEffect(screen) { navigation.resetTo(if (progressRepository.hasAnyProfiles()) Screen.SignIn else Screen.CreateAccount) }
            } else when (screen) {
                Screen.WorkspaceLoading -> WorkspaceLoadingScreen(navigation, progressRepository)
                Screen.CreateAccount -> CreateAccountScreen(navigation, progressRepository)
                Screen.SignIn -> SignInScreen(navigation, progressRepository)
                Screen.LegacyCredentialSetup -> LegacyCredentialSetupScreen(navigation, progressRepository)
                Screen.ChangePassword -> CleanLibraryScreen("Password", "Password changes remain available in the local profile flow.", navigation)
                Screen.Home -> LibraryHomeScreen(progressRepository, navigation)
                Screen.LearningHub -> LearningHubHomeScreen(navigation)
                Screen.LearningHubSection -> LearningHubSectionScreen(navigation)
                Screen.LearningLibrary -> LibraryListScreen("Learning Library", null, progressRepository, documentActions)
                Screen.Books -> LibraryListScreen("Books", LibraryKind.BOOK, progressRepository, documentActions)
                Screen.IntensiveFiles -> LibraryListScreen("Intensive Files", LibraryKind.INTENSIVE_FILE, progressRepository, documentActions)
                Screen.ReadingProgress -> ReadingProgressScreen(progressRepository, documentActions)
                Screen.Bookmarks -> BookmarksScreen(progressRepository, documentActions)
                Screen.Search -> SearchLibraryScreen(progressRepository, documentActions)
                Screen.Profile -> ProfileScreen(navigation, progressRepository)
                Screen.Settings -> SettingsScreen(navigation, progressRepository)
                Screen.About -> CleanLibraryScreen("About", "Nous AI Academy is an offline-first reading workspace. Read deeply. Build locally.", navigation)
            }
        }
    }
}
