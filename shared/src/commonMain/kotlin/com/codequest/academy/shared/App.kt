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
import com.codequest.academy.shared.ui.screens.CreateAccountScreen
import com.codequest.academy.shared.ui.screens.LegacyCredentialSetupScreen
import com.codequest.academy.shared.ui.screens.ProfileScreen
import com.codequest.academy.shared.ui.screens.SettingsScreen
import com.codequest.academy.shared.ui.screens.SignInScreen
import com.codequest.academy.shared.ui.screens.WorkspaceLoadingScreen
import com.codequest.academy.shared.ui.theme.CodeQuestTheme
import com.codequest.academy.shared.ui.viewmodels.AppViewModel
import com.codequest.academy.shared.ui.viewmodels.rememberViewModel

@Composable
fun App(progressRepository: ProgressRepository) {
    val navigation = remember { Navigation() }
    val appViewModel = rememberViewModel { AppViewModel() }
    val isRailExpanded by appViewModel.isRailExpanded.collectAsState()
    CodeQuestTheme {
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
                Screen.Home -> CleanLibraryScreen("Home", "A quiet, private workspace is ready for the official Nous AI Academy curriculum package.", navigation)
                Screen.LearningLibrary -> CleanLibraryScreen("Learning Library", "The official curriculum package is not installed. No books or learning files are represented as available.", navigation)
                Screen.Books -> CleanLibraryScreen("Books", "Books will appear here only after the official curriculum package is verified and installed.", navigation)
                Screen.IntensiveFiles -> CleanLibraryScreen("Intensive Files", "Intensive files will appear here only after the official curriculum package is verified and installed.", navigation)
                Screen.ReadingProgress -> CleanLibraryScreen("Reading Progress", "Your private reading metadata is retained locally. There is no active curriculum to measure.", navigation)
                Screen.Bookmarks -> CleanLibraryScreen("Bookmarks", "Bookmarks are preserved as private local data. Removed curriculum documents are not available to open.", navigation)
                Screen.Search -> CleanLibraryScreen("Search", "Search will become available when verified library content is installed.", navigation)
                Screen.Profile -> ProfileScreen(navigation, progressRepository)
                Screen.Settings -> SettingsScreen(navigation, progressRepository)
                Screen.About -> CleanLibraryScreen("About", "Nous AI Academy is an offline-first reading workspace. Read deeply. Build locally.", navigation)
            }
        }
    }
}
