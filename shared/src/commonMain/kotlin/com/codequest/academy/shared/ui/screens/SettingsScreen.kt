package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.update.UpdateController
import com.codequest.academy.shared.update.UpdateUiState
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton

@Composable
fun SettingsScreen(navigation: Navigation, repository: ProgressRepository) {
    var reducedMotion by remember { mutableStateOf(repository.getSetting("reduced_motion", "false") == "true") }
    var largerText by remember { mutableStateOf(repository.getSetting("larger_text", "false") == "true") }

    Column(
        Modifier.fillMaxSize()
            .background(Theme.colors.appBackground)
            .padding(40.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", style = DisplayStyle)
        Text("Appearance, local data, and system details", style = AppTypography.body1, color = Theme.colors.textSecondary)

        Spacer(Modifier.height(28.dp))
        Text("Updates & distribution", style = AppTypography.h2)
        Spacer(Modifier.height(12.dp))

        UpdateSettingsPanel()

        Spacer(Modifier.height(24.dp))
        Text("Appearance", style = AppTypography.h2)
        Spacer(Modifier.height(12.dp))
        SettingRow("Reduced motion", "Removes optional movement transitions.", reducedMotion) {
            reducedMotion = it
            repository.setSetting("reduced_motion", it.toString())
        }
        SettingRow("Larger reading text", "Uses the larger reading presentation for long-form learning content.", largerText) {
            largerText = it
            repository.setSetting("larger_text", it.toString())
        }

        Spacer(Modifier.height(24.dp))
        Text("Data and local storage", style = AppTypography.h2)
        Spacer(Modifier.height(12.dp))
        InfoCard("Library status", "No official curriculum package is installed.")
        Spacer(Modifier.height(10.dp))
        InfoCard("Storage", "Local SQLite database for accounts, preferences, bookmarks, notes, and reader metadata.")
        Spacer(Modifier.height(10.dp))
        InfoCard("Backend connection", "Configured by the desktop deployment; update checks work when a release endpoint is provided.")

        Spacer(Modifier.height(24.dp))
        Text("About & Release Info", style = AppTypography.h2)
        Spacer(Modifier.height(12.dp))
        InfoCard("Nous AI Academy", "Native desktop application with offline-first reading progress and verified release manifests.")
    }
}

/** Shared update controls. Installation is always explicit and is blocked until a signed release manifest is available. */
@Composable
fun UpdateSettingsPanel() {
    val state by UpdateController.state.collectAsState()
    val status = when (state) {
        UpdateUiState.Idle -> "Ready to check the verified Nous AI Academy release channel."
        UpdateUiState.Checking -> "Checking the trusted release manifest…"
        UpdateUiState.Offline -> "Offline. Connect to the internet to check for updates."
        UpdateUiState.UpToDate -> "You are running the latest verified version."
        UpdateUiState.Downloading -> "Downloading the update securely…"
        UpdateUiState.Verifying -> "Verifying checksum and package metadata…"
        UpdateUiState.ReadyToRestart -> "Update verified and ready to restart."
        UpdateUiState.Installing -> "Installing the update and restarting the app…"
        is UpdateUiState.Available -> "Version ${(state as UpdateUiState.Available).info.latestVersion} is available."
        is UpdateUiState.Failed -> (state as UpdateUiState.Failed).message
    }
    Column(
        Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Secure release channel", style = AppTypography.h3)
        Text(status, style = AppTypography.body2, color = Theme.colors.textSecondary)
        val available = state as? UpdateUiState.Available
        if (available != null) {
            val info = available.info
            Text("Nous AI Academy ${info.latestVersion} · ${info.updateType}", style = AppTypography.body1)
            if (info.releaseNotes.isNotBlank()) Text(info.releaseNotes, style = AppTypography.caption, color = Theme.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Update now", onClick = UpdateController::installUpdate)
                SecondaryButton("Cancel", onClick = UpdateController::cancel)
            }
        } else {
            PrimaryButton("Check for updates", onClick = UpdateController::checkForUpdates, enabled = state != UpdateUiState.Checking)
        }
        Text("Updates are downloaded only from the trusted HTTPS host, verified by size and SHA-256, and never execute unsigned packages.", style = AppTypography.caption, color = Theme.colors.textMuted)
    }
}

@Composable
private fun SettingRow(title: String, detail: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTypography.h3)
            Text(detail, style = AppTypography.body2, color = Theme.colors.textSecondary)
        }
        Switch(value, onCheckedChange = onChange)
    }
}
