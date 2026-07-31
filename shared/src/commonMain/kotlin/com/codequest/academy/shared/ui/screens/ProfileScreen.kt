package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.AccountResult
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme

@Composable
fun ProfileScreen(navigation: Navigation, repository: ProgressRepository) {
    val account = repository.activeAccount()
    if (account == null) {
        navigation.resetTo(Screen.SignIn)
        return
    }
    var name by remember(account.userId) { mutableStateOf(account.displayName) }
    var email by remember(account.userId) { mutableStateOf(account.email) }
    var currentPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var exportVisible by remember { mutableStateOf(false) }
    var signOutVisible by remember { mutableStateOf(false) }
    val summary = remember(account.userId) { repository.getProgressSummary() }
    val currentTrack = remember(account.userId) { repository.getSetting("current_track", "Not selected") }
    val currentPath = remember(account.userId) { repository.getSetting("current_path", "Not selected") }
    val initials = name.trim().split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "L" }
    if (signOutVisible) AlertDialog(
        onDismissRequest = { signOutVisible = false },
        title = { Text("Sign out of CodeQuest Academy?") },
        text = { Text("Your learning progress will remain saved on this device.") },
        confirmButton = { PrimaryButton("Sign Out", onClick = { repository.signOut(); navigation.resetTo(Screen.SignIn) }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { signOutVisible = false }) }
    )
    if (exportVisible) AlertDialog(
        onDismissRequest = { exportVisible = false }, title = { Text("Progress export") },
        text = { SelectionContainer { Text("{\"profile\":\"${name.replace("\"", "")}\",\"completedNodes\":${summary.completedNodes},\"levelsCompleted\":${summary.levelsCompleted},\"projectsSubmitted\":${summary.projectsSubmitted}}") } },
        confirmButton = { SecondaryButton("Close", onClick = { exportVisible = false }) }
    )
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp)) {
        Text("Your Profile", style = DisplayStyle); Spacer(Modifier.height(24.dp))
        InfoCard(initials, "Local account · progress stays on this device")
        Spacer(Modifier.height(12.dp)); Text("Account email: ${account.email}", style = AppTypography.body2, color = Theme.colors.textSecondary)
        Text("Created: ${account.createdAt}", style = AppTypography.caption, color = Theme.colors.textMuted)
        Text("Last sign-in: ${account.lastLoginAt}", style = AppTypography.caption, color = Theme.colors.textMuted)
        Text("Current track: $currentTrack", style = AppTypography.body2, color = Theme.colors.textSecondary)
        Text("Current path: $currentPath", style = AppTypography.body2, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(name, { name = it }, Modifier.widthIn(max = 520.dp).fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.widthIn(max = 520.dp).fillMaxWidth(), label = { Text("Email (local identifier)") }, singleLine = true)
        OutlinedTextField(currentPassword, { currentPassword = it }, Modifier.widthIn(max = 520.dp).fillMaxWidth(), label = { Text("Current password (required when changing email)") }, singleLine = true)
        message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Theme.colors.error, style = AppTypography.body2) }
        Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Save Profile", onClick = { message = resultMessage(repository.updateLocalProfile(name, email, currentPassword)) }, enabled = name.isNotBlank() && email.isNotBlank())
            SecondaryButton("Change Password", onClick = { navigation.navigateTo(Screen.ChangePassword) })
            SecondaryButton("Export Progress", onClick = { exportVisible = true })
            SecondaryButton("Sign Out", onClick = { signOutVisible = true })
        }
        Spacer(Modifier.height(28.dp)); Text("Learning summary", style = AppTypography.h2); Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { InfoCard("Completed steps", summary.completedNodes.toString(), Modifier.weight(1f)); InfoCard("Completed levels", summary.levelsCompleted.toString(), Modifier.weight(1f)); InfoCard("Projects", summary.projectsSubmitted.toString(), Modifier.weight(1f)) }
        Spacer(Modifier.height(20.dp)); Text("Your CodeQuest Academy account and learning progress are stored on this device. They are not automatically synchronized with other computers.", style = AppTypography.caption, color = Theme.colors.textMuted)
    }
}

private fun resultMessage(result: AccountResult): String = when (result) {
    is AccountResult.Success -> "Profile saved."
    is AccountResult.Error -> result.message
}
