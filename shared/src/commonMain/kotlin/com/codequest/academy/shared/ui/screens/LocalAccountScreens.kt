package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.AccountResult
import com.codequest.academy.shared.data.LocalAccount
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOCAL_STORAGE_NOTE = "Your Nous AI Academy account and reading progress are stored on this device.\nThey are not automatically synchronized with other computers."

@Composable
fun CreateAccountScreen(navigation: Navigation, repository: ProgressRepository) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }; var confirmation by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AuthCard("Start learning through mathematical thinking.", "Create Account", error, loading) {
        AccountFields(name, { name = it }, email, { email = it }, password, { password = it }, confirmation, { confirmation = it }, showPassword, { showPassword = !showPassword })
        Spacer(Modifier.height(12.dp)); Text("Password: 8+ characters, one letter, and one number. Maximum 128 characters.", style = AppTypography.caption, color = Theme.colors.textMuted)
        Spacer(Modifier.height(18.dp)); PrimaryButton("Create Account", onClick = {
            if (password != confirmation) { error = "The passwords do not match."; return@PrimaryButton }
            loading = true; error = null
            scope.launch {
                val result = withContext(Dispatchers.Default) { repository.createLocalAccount(name, email, password) }
                loading = false
                when (result) { is AccountResult.Success -> navigation.resetTo(Screen.Home); is AccountResult.Error -> error = result.message }
            }
        }, enabled = !loading)
        Spacer(Modifier.height(14.dp)); TextButton(onClick = { navigation.resetTo(Screen.SignIn) }, enabled = !loading) { Text("Already have an account? Sign in") }
        Text(LOCAL_STORAGE_NOTE, style = AppTypography.caption, color = Theme.colors.textMuted)
    }
}

@Composable
fun SignInScreen(navigation: Navigation, repository: ProgressRepository) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }; var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AuthCard("Welcome back. Continue your local learning journey.", "Sign In", error, loading) {
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, enabled = !loading)
        PasswordField("Password", password, { password = it }, showPassword, { showPassword = !showPassword }, loading)
        Spacer(Modifier.height(16.dp)); PrimaryButton("Sign In", onClick = {
            loading = true; error = null
            scope.launch {
                val result = withContext(Dispatchers.Default) { repository.signIn(email, password) }
                loading = false
                when (result) { is AccountResult.Success -> navigation.resetTo(Screen.Home); is AccountResult.Error -> error = "Incorrect email or password." }
            }
        }, enabled = !loading && email.isNotBlank() && password.isNotEmpty())
        Spacer(Modifier.height(10.dp)); TextButton(onClick = { navigation.resetTo(Screen.CreateAccount) }, enabled = !loading) { Text("Create a new account") }
        Text(LOCAL_STORAGE_NOTE, style = AppTypography.caption, color = Theme.colors.textMuted)
    }
}

@Composable
fun LegacyCredentialSetupScreen(navigation: Navigation, repository: ProgressRepository) {
    val legacy = repository.legacyAccount()
    if (legacy == null) { navigation.resetTo(Screen.SignIn); return }
    var name by remember { mutableStateOf(legacy.displayName) }; var email by remember { mutableStateOf(legacy.email) }
    var password by remember { mutableStateOf("") }; var confirmation by remember { mutableStateOf("") }; var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }; var loading by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    AuthCard("Secure your existing local learning profile.", "Set Up Local Credentials", error, loading) {
        Text("Your existing progress will remain attached to this profile.", style = AppTypography.body2, color = Theme.colors.textSecondary)
        AccountFields(name, { name = it }, email, { email = it }, password, { password = it }, confirmation, { confirmation = it }, show, { show = !show })
        Spacer(Modifier.height(14.dp)); PrimaryButton("Save Local Credentials", onClick = {
            if (password != confirmation) { error = "The passwords do not match."; return@PrimaryButton }
            loading = true; scope.launch {
                val result = withContext(Dispatchers.Default) { repository.completeLegacySetup(legacy.userId, name, email, password) }
                loading = false; when (result) { is AccountResult.Success -> navigation.resetTo(Screen.Home); is AccountResult.Error -> error = result.message }
            }
        }, enabled = !loading)
        Text(LOCAL_STORAGE_NOTE, style = AppTypography.caption, color = Theme.colors.textMuted)
    }
}

@Composable
fun ChangePasswordScreen(navigation: Navigation, repository: ProgressRepository) {
    var current by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }; var success by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    AuthCard("Keep your local account secure.", "Change Password", error, loading) {
        PasswordField("Current password", current, { current = it }, show, { show = !show }, loading)
        PasswordField("New password", next, { next = it }, show, { show = !show }, loading)
        PasswordField("Confirm new password", confirm, { confirm = it }, show, { show = !show }, loading)
        Spacer(Modifier.height(14.dp)); if (success) Text("Password changed successfully.", color = Theme.colors.success)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Save Password", onClick = {
                loading = true; scope.launch { val result = withContext(Dispatchers.Default) { repository.changePassword(current, next, confirm) }; loading = false; when (result) { is AccountResult.Success -> success = true; is AccountResult.Error -> error = result.message } }
            }, enabled = !loading)
            SecondaryButton("Back to Profile", onClick = { navigation.pop() })
        }
    }
}

@Composable
private fun AuthCard(headline: String, title: String, error: String?, loading: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.widthIn(max = 540.dp).fillMaxWidth(), elevation = 0.dp, backgroundColor = Theme.colors.surfaceSecondary) {
            Column(Modifier.padding(36.dp), horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CQ", style = AppTypography.button.copy(fontWeight = FontWeight.ExtraBold), color = Color.White, modifier = Modifier.padding(10.dp))
                    Spacer(Modifier.width(12.dp)); Column {
                        Text("CODEQUEST ACADEMY", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.accentCyan)
                        Text("Local learning workspace", style = AppTypography.caption, color = Theme.colors.textMuted)
                    }
                }
                Spacer(Modifier.height(28.dp)); Text(title, style = AppTypography.h1, color = Theme.colors.textPrimary)
                Spacer(Modifier.height(8.dp)); Text(headline, style = AppTypography.body2, color = Theme.colors.textSecondary); Spacer(Modifier.height(24.dp))
                if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Theme.colors.brandPrimary); Spacer(Modifier.height(12.dp)) }
                error?.let { Text(it, style = AppTypography.body2, color = Theme.colors.error); Spacer(Modifier.height(8.dp)) }
                content()
            }
        }
    }
}

@Composable
private fun AccountFields(name: String, onName: (String) -> Unit, email: String, onEmail: (String) -> Unit, password: String, onPassword: (String) -> Unit, confirmation: String, onConfirmation: (String) -> Unit, showPassword: Boolean, togglePassword: () -> Unit) {
    OutlinedTextField(name, onName, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
    OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("Email (local identifier)") }, singleLine = true)
    PasswordField("Password", password, onPassword, showPassword, togglePassword, false)
    PasswordField("Confirm password", confirmation, onConfirmation, showPassword, togglePassword, false)
}

@Composable
private fun PasswordField(label: String, value: String, onValue: (String) -> Unit, visible: Boolean, toggle: () -> Unit, disabled: Boolean) {
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, enabled = !disabled, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = toggle, enabled = !disabled) { Text(if (visible) "Hide" else "Show") } })
}
