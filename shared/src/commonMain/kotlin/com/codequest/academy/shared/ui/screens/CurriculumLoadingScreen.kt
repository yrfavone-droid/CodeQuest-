package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.CurriculumFileReader
import com.codequest.academy.shared.data.CurriculumLoader
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed class StartupState {
    data class Loading(val status: String) : StartupState()
    data class Failed(val safeMessage: String) : StartupState()
}

@Composable
fun CurriculumLoadingScreen(
    navigation: Navigation,
    fileReader: CurriculumFileReader,
    progressRepository: ProgressRepository
) {
    var retryKey by remember { mutableStateOf(0) }
    var state: StartupState by remember { mutableStateOf(StartupState.Loading("Loading curriculum…")) }

    LaunchedEffect(retryKey) {
        state = StartupState.Loading("Reading curriculum catalog…")
        try {
            val seedResult = withContext(Dispatchers.Default) {
                val manifest = fileReader.readAsset("curriculum_build_manifest.json")
                val version = "\"schema_version\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    .find(manifest)?.groupValues?.get(1) ?: error("Curriculum version is missing")
                val legacySeed = if (progressRepository.isCurriculumCurrent(version)) {
                    com.codequest.academy.shared.data.CurriculumSeedResult(version, 5, 10, 50, 5000, false)
                } else {
                    val loader = CurriculumLoader()
                    val pathFiles = fileReader.listPaths()
                    val parsed = pathFiles.mapIndexed { index, path ->
                        withContext(Dispatchers.Main) {
                            state = StartupState.Loading("Validating legacy path ${index + 1} of ${pathFiles.size}…")
                        }
                        loader.parsePath(fileReader.readAsset(path))
                    }
                    progressRepository.seedCurriculum(version, parsed)
                }
                withContext(Dispatchers.Main) { state = StartupState.Loading("Installing the offline AI Academy pack…") }
                progressRepository.installLocalAcademyContent(
                    fileReader.readAsset("academy/source/CURRICULUM/problem_manifest_10000.csv")
                )
                legacySeed
            }
            require(seedResult.trackCount == 5 && seedResult.pathCount == 10 && seedResult.levelCount == 50)
            state = StartupState.Loading("Restoring learner progress…")
            val destination = withContext(Dispatchers.Default) {
                when {
                    progressRepository.hasLegacyProfiles() -> Screen.LegacyCredentialSetup
                    progressRepository.hasActiveSession() -> Screen.AcademyHome
                    progressRepository.hasAnyProfiles() -> Screen.SignIn
                    else -> Screen.CreateAccount
                }
            }
            navigation.resetTo(destination)
        } catch (error: Throwable) {
            println("CodeQuest curriculum startup failed:\n${error.stackTraceToString()}")
            state = StartupState.Failed(error.message ?: "The curriculum data could not be validated.")
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
                Text("CQ", style = DisplayStyle, color = Theme.colors.brandPrimary)
                Spacer(Modifier.height(8.dp))
                Text("CodeQuest Academy", style = AppTypography.h2, color = Theme.colors.textPrimary)
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
                        Text("We couldn’t load the curriculum.", style = AppTypography.h3, color = Theme.colors.error)
                        Spacer(Modifier.height(8.dp))
                        Text(current.safeMessage, style = AppTypography.body2, color = Theme.colors.textSecondary)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryButton("Back to Dashboard", onClick = { navigation.resetTo(Screen.Dashboard) })
                            PrimaryButton("Retry", onClick = { retryKey += 1 })
                        }
                    }
                }
            }
        }
    }
}
