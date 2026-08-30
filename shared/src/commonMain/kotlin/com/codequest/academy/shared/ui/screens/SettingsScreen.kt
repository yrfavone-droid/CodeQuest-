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

        InfoCard("In-app updates", "The application checks only a verified release endpoint. An update action appears only when a real installer is published.")

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
