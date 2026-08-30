package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codequestacademy.shared.generated.resources.Res
import codequestacademy.shared.generated.resources.codequest_ai_book_icon
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private sealed class StartupState {
    data class Loading(val status: String) : StartupState()
    data class Failed(val safeMessage: String) : StartupState()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CurriculumLoadingScreen(
    navigation: Navigation,
    progressRepository: ProgressRepository
) {
    var retryKey by remember { mutableStateOf(0) }
    var state: StartupState by remember { mutableStateOf(StartupState.Loading("Loading curriculum…")) }

    LaunchedEffect(retryKey) {
        state = StartupState.Loading("Preparing the offline AI Academy…")
        try {
            withContext(Dispatchers.Default) {
                progressRepository.installNousLibrary()
            }
            state = StartupState.Loading("Restoring learner progress…")
            val destination = withContext(Dispatchers.Default) {
                when {
                    progressRepository.hasLegacyProfiles() -> Screen.LegacyCredentialSetup
                    progressRepository.hasActiveSession() -> Screen.NousBooks
                    progressRepository.hasAnyProfiles() -> Screen.SignIn
                    else -> Screen.CreateAccount
                }
            }
            navigation.resetTo(destination)
        } catch (error: Throwable) {
            println("Nous library startup failed:\n${error.stackTraceToString()}")
            state = StartupState.Failed(error.message ?: "The local library could not be validated.")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = Theme.colors.surfacePrimary,
            elevation = 2.dp
        ) {
            Column(Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(Res.drawable.codequest_ai_book_icon),
                    contentDescription = "Nous AI Academy logo",
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text("Nous AI Academy", style = AppTypography.h2, color = Theme.colors.textPrimary)
                Spacer(Modifier.height(28.dp))
                when (val current = state) {
                    is StartupState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Theme.colors.brandPrimary,
                            backgroundColor = Theme.colors.surfaceTertiary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(current.status, style = AppTypography.body2, color = Theme.colors.textSecondary)
                    }
                    is StartupState.Failed -> {
                        Text("We couldn’t load the offline library.", style = AppTypography.h3, color = Theme.colors.error)
                        Spacer(Modifier.height(8.dp))
                        Text(current.safeMessage, style = AppTypography.body2, color = Theme.colors.textSecondary)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryButton("Back to library", onClick = { navigation.resetTo(Screen.NousBooks) })
                            PrimaryButton("Retry", onClick = { retryKey += 1 })
                        }
                    }
                }
            }
        }
    }
}
