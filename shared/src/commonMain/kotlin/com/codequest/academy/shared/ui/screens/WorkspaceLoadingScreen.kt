package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WorkspaceLoadingScreen(navigation: Navigation, repository: ProgressRepository) {
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) { repository.prepareCleanLibrary() }
        val destination = withContext(Dispatchers.Default) {
            when {
                repository.hasLegacyProfiles() -> Screen.LegacyCredentialSetup
                repository.hasActiveSession() -> Screen.Home
                repository.hasAnyProfiles() -> Screen.SignIn
                else -> Screen.CreateAccount
            }
        }
        navigation.resetTo(destination)
    }
    Box(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Theme.colors.brandPrimary)
            Text("Preparing your private workspace", style = AppTypography.h2, color = Theme.colors.textPrimary)
            Text("Protecting local accounts and reader data.", style = AppTypography.body2, color = Theme.colors.textSecondary)
        }
    }
}
