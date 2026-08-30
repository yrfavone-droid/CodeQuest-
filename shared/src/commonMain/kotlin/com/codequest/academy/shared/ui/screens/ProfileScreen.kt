package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val account = repository.activeAccount() ?: run { navigation.resetTo(Screen.SignIn); return }
    var name by remember(account.userId) { mutableStateOf(account.displayName) }
    var email by remember(account.userId) { mutableStateOf(account.email) }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(44.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Profile & local data", style = DisplayStyle)
        Text("Your account and preferences stay on this device.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        InfoCard("Data protection", "Existing bookmarks, notes, highlights, and reading metadata are retained privately. Removed curriculum documents cannot be opened.")
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email (local identifier)") }, singleLine = true)
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Current password (only needed to change email)") }, singleLine = true)
        message?.let { Text(it, style = AppTypography.body2, color = Theme.colors.textSecondary) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Save profile", onClick = { message = when (val result = repository.updateLocalProfile(name, email, password)) { is AccountResult.Success -> "Profile saved."; is AccountResult.Error -> result.message } })
            SecondaryButton("Change password", onClick = { navigation.navigateTo(Screen.ChangePassword) })
            SecondaryButton("Sign out", onClick = { repository.signOut(); navigation.resetTo(Screen.SignIn) })
        }
    }
}
