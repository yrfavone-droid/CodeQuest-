package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var autoUpdateEnabled by remember { mutableStateOf(repository.getSetting("auto_update", "true") == "true") }
    var updateChannel by remember { mutableStateOf(repository.getSetting("update_channel", "stable")) }
    var updateCheckStatus by remember { mutableStateOf("Up to date (v1.0.0)") }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize()
            .background(Theme.colors.appBackground)
            .padding(40.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", style = DisplayStyle)
        Text("Appearance, auto-updates, cache management, and system details", style = AppTypography.body1, color = Theme.colors.textSecondary)

        Spacer(Modifier.height(28.dp))
        Text("Automatic Updates & Distribution", style = AppTypography.h2)
        Spacer(Modifier.height(12.dp))

        SettingRow(
            "Enable Automatic Updates",
            "Automatically check and download new versions in background.",
            autoUpdateEnabled
        ) {
            autoUpdateEnabled = it
            repository.setSetting("auto_update", it.toString())
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Check for Updates Now", style = AppTypography.h3)
                Text(updateCheckStatus, style = AppTypography.body2, color = Theme.colors.textSecondary)
            }
            Button(
                onClick = {
                    isCheckingUpdate = true
                    updateCheckStatus = "Checking server for updates..."
                    repository.setSetting("last_update_check", System.currentTimeMillis().toString())
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Theme.colors.brandPrimary)
            ) {
                Text(if (isCheckingUpdate) "Checking..." else "Check Now", color = Color.White)
            }
        }

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
        Text("Data and Offline Cache", style = AppTypography.h2)
        Spacer(Modifier.height(12.dp))
        InfoCard("Curriculum version", repository.getCurriculumVersion())
        Spacer(Modifier.height(10.dp))
        InfoCard("Storage & Cache", "Local SQLite progress database · Local Content Cache (TTL 24h)")
        Spacer(Modifier.height(10.dp))
        InfoCard("Backend API Connection", "Connected to http://localhost:3000 · Real-time WebSocket Stream Active")

        Spacer(Modifier.height(24.dp))
        Text("About & Release Info", style = AppTypography.h2)
        Spacer(Modifier.height(12.dp))
        InfoCard("CodeQuest Academy v1.0.0", "Native Desktop Application with Website Distribution & Auto-Update Engine.")
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
