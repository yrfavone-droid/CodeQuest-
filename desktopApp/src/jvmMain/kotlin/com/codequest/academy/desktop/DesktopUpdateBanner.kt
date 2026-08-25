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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.update.AutoUpdateManager
import com.codequest.academy.shared.update.UpdateState
import java.net.URI

@Composable
fun DesktopUpdateBanner(modifier: Modifier = Modifier) {
    val updateState by AutoUpdateManager.updateState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val update = (updateState as? UpdateState.UpdateAvailable)?.info ?: return
    val canOpenDownload = isSupportedUpdateUrl(update.downloadUrl)

    Box(modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier.widthIn(max = 430.dp).fillMaxWidth(.42f)
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
                    Text("UPDATE AVAILABLE", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.accentCyan)
                    Text("CodeQuest Academy ${update.latestVersion}", style = AppTypography.body2.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary)
                }
                TextButton(onClick = AutoUpdateManager::dismissUpdate) { Text("Later", style = AppTypography.caption, color = Theme.colors.textSecondary) }
            }
            if (update.releaseNotes.isNotBlank()) {
                Spacer(Modifier.height(12.dp)); Text(update.releaseNotes, style = AppTypography.body2, color = Theme.colors.textSecondary, maxLines = 3)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Update now", enabled = canOpenDownload, onClick = {
                    uriHandler.openUri(update.downloadUrl)
                    AutoUpdateManager.recordUpdateAction(update)
                })
                Text("Opens the verified installer", style = AppTypography.caption, color = Theme.colors.textMuted)
            }
            if (!canOpenDownload) {
                Spacer(Modifier.height(8.dp)); Text("The update link is not valid. Try checking again from the tray menu.", style = AppTypography.caption, color = Theme.colors.error)
            }
        }
    }
}

internal fun isSupportedUpdateUrl(value: String): Boolean = try {
    val uri = URI(value)
    uri.scheme == "https" || (uri.scheme == "http" && (uri.host == "localhost" || uri.host == "127.0.0.1"))
} catch (_: Exception) {
    false
}
