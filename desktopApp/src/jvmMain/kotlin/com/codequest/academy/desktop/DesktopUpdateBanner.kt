package com.codequest.academy.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.update.UpdateController
import com.codequest.academy.shared.update.UpdateUiState
import java.net.URI

@Composable
fun DesktopUpdateBanner(modifier: Modifier = Modifier) {
    val state by UpdateController.state.collectAsState()
    if (state is UpdateUiState.Idle) return

    Box(modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 430.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Theme.colors.surfaceSecondary)
                .border(1.dp, Theme.colors.brandPrimary.copy(alpha = .7f), RoundedCornerShape(14.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Theme.colors.brandSoft), contentAlignment = Alignment.Center) {
                    Text("UP", style = AppTypography.caption.copy(fontWeight = FontWeight.ExtraBold), color = Theme.colors.brandPrimaryHover)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("NOUS AI ACADEMY UPDATES", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.accentCyan)
                    Text(updateHeadline(state), style = AppTypography.body2.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary, maxLines = 2)
                }
            }
            if (state is UpdateUiState.Available && (state as UpdateUiState.Available).info.releaseNotes.isNotBlank()) {
                Spacer(Modifier.height(12.dp)); Text((state as UpdateUiState.Available).info.releaseNotes, style = AppTypography.body2, color = Theme.colors.textSecondary, maxLines = 3)
            }
            when (state) {
                is UpdateUiState.Available -> {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("Update now", modifier = Modifier.weight(1f), onClick = UpdateController::installUpdate)
                        TextButton(onClick = UpdateController::cancel) { Text("Later", style = AppTypography.caption, color = Theme.colors.textSecondary) }
                    }
                }
                UpdateUiState.Checking, UpdateUiState.Downloading, UpdateUiState.Verifying, UpdateUiState.Installing -> {
                    Spacer(Modifier.height(12.dp)); SecondaryButton("Cancel", onClick = UpdateController::cancel)
                }
                UpdateUiState.Offline, UpdateUiState.UpToDate, is UpdateUiState.Failed -> {
                    Spacer(Modifier.height(12.dp)); TextButton(onClick = UpdateController::checkForUpdates) { Text("Check again", style = AppTypography.caption, color = Theme.colors.brandPrimary) }
                }
                UpdateUiState.Idle, UpdateUiState.ReadyToRestart -> Unit
            }
        }
    }
}

private fun updateHeadline(state: UpdateUiState): String = when (state) {
    UpdateUiState.Checking -> "Checking the trusted release manifest…"
    UpdateUiState.Offline -> "Offline — connect to check for updates."
    UpdateUiState.UpToDate -> "You are up to date."
    UpdateUiState.Downloading -> "Downloading a verified package…"
    UpdateUiState.Verifying -> "Verifying package integrity…"
    UpdateUiState.ReadyToRestart -> "Update verified; preparing restart."
    UpdateUiState.Installing -> "Installing and restarting…"
    is UpdateUiState.Available -> "Version ${state.info.latestVersion} is available."
    is UpdateUiState.Failed -> state.message
    UpdateUiState.Idle -> ""
}

internal fun isSupportedUpdateUrl(value: String): Boolean = try {
    val uri = URI(value)
    uri.scheme == "https" || (uri.scheme == "http" && (uri.host == "localhost" || uri.host == "127.0.0.1"))
} catch (_: Exception) {
    false
}
