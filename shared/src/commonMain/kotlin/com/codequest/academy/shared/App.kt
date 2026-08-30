package com.codequest.academy.shared

import androidx.compose.runtime.*
import com.codequest.academy.shared.data.AcademyDocumentHandler
import com.codequest.academy.shared.data.OfflinePdfReader
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.AppShell
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.screens.*
import com.codequest.academy.shared.ui.theme.CodeQuestTheme
import com.codequest.academy.shared.ui.viewmodels.AppViewModel
import com.codequest.academy.shared.ui.viewmodels.rememberViewModel

@Composable
fun App(progressRepository: ProgressRepository, documentHandler: AcademyDocumentHandler, pdfReader: OfflinePdfReader) {
    val navigation = remember { Navigation() }
    val appViewModel = rememberViewModel { AppViewModel() }
    val isRailExpanded by appViewModel.isRailExpanded.collectAsState()
    CodeQuestTheme {
        AppShell(navigation, isRailExpanded, appViewModel::toggleRail) {
            val screen = navigation.currentScreen
            val authScreen = screen == Screen.CurriculumLoading || screen == Screen.CreateAccount || screen == Screen.SignIn || screen == Screen.LegacyCredentialSetup
            if (!authScreen && !progressRepository.hasActiveSession()) {
                LaunchedEffect(screen) { navigation.resetTo(if (progressRepository.hasAnyProfiles()) Screen.SignIn else Screen.CreateAccount) }
            } else when (screen) {
                Screen.CurriculumLoading -> CurriculumLoadingScreen(navigation, progressRepository)
                Screen.CreateAccount -> CreateAccountScreen(navigation, progressRepository)
                Screen.SignIn -> SignInScreen(navigation, progressRepository)
                Screen.LegacyCredentialSetup -> LegacyCredentialSetupScreen(navigation, progressRepository)
                Screen.ChangePassword -> ChangePasswordScreen(navigation, progressRepository)
                Screen.NousBooks -> NousLibraryScreen(com.codequest.academy.shared.data.LibraryKind.BOOK, navigation, progressRepository, documentHandler)
                Screen.NousIntensiveFiles -> NousLibraryScreen(com.codequest.academy.shared.data.LibraryKind.INTENSIVE_FILE, navigation, progressRepository, documentHandler)
                is Screen.NousReader -> {
                    val item = (progressRepository.getNousBooks() + progressRepository.getNousIntensiveFiles()).firstOrNull { it.id == screen.documentId }
                    if (item == null) navigation.resetTo(Screen.NousBooks) else NousReaderScreen(item, navigation, progressRepository, documentHandler, pdfReader)
                }
                Screen.Profile -> ProfileScreen(navigation, progressRepository)
                Screen.Settings -> SettingsScreen(navigation, progressRepository)
                else -> LaunchedEffect(screen) { navigation.resetTo(Screen.NousBooks) }
            }
        }
    }
}
