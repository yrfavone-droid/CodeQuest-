package com.codequest.academy.shared.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class Screen {
    object WorkspaceLoading : Screen()
    object CreateAccount : Screen()
    object SignIn : Screen()
    object LegacyCredentialSetup : Screen()
    object ChangePassword : Screen()
    object Home : Screen()
    object LearningLibrary : Screen()
    object Books : Screen()
    object IntensiveFiles : Screen()
    object ReadingProgress : Screen()
    object Bookmarks : Screen()
    object Search : Screen()
    object Profile : Screen()
    object Settings : Screen()
    object About : Screen()
}

class Navigation {
    var backStack by mutableStateOf(listOf<Screen>(Screen.WorkspaceLoading))
        private set

    val currentScreen: Screen get() = backStack.last()

    fun navigateTo(screen: Screen) {
        if (backStack.lastOrNull() != screen) backStack = backStack + screen
    }

    fun pop() {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }

    fun resetTo(screen: Screen) {
        backStack = listOf(screen)
    }
}
