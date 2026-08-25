package com.codequest.academy.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.models.TrackIdentity
import com.codequest.academy.shared.ui.theme.Theme

@Composable
fun TrackCard(
    track: TrackIdentity,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    pathNames: List<String> = emptyList()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val elevation = if (isHovered) 8.dp else 2.dp
    val borderColor = if (isSelected) Theme.colors.brandPrimary else if (isHovered) Theme.colors.accentCyan else Theme.colors.borderDefault
    val bgColor = if (isSelected) Theme.colors.surfaceSecondary else Theme.colors.surfacePrimary

    // Each learning domain owns one stable, solid identity color.
    val badgeColor = track.primaryColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(14.dp), spotColor = track.primaryColor.copy(alpha = 0.16f))
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(BorderStroke(if (isSelected || isHovered) 1.dp else 1.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.heightIn(min = 276.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor)
                    .border(1.dp, badgeColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trackCode(track),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Title & Description
            Text(
                text = track.title,
                style = com.codequest.academy.shared.ui.theme.AppTypography.h3,
                color = Color.White
            )

            if (pathNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                pathNames.forEach { path ->
                Text(path, style = com.codequest.academy.shared.ui.theme.AppTypography.caption, color = Theme.colors.textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.description,
                style = com.codequest.academy.shared.ui.theme.AppTypography.body2,
                color = Theme.colors.textSecondary,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CURRICULUM", style = com.codequest.academy.shared.ui.theme.AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textMuted)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(4.dp).background(Theme.colors.borderStrong, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Structured paths", style = com.codequest.academy.shared.ui.theme.AppTypography.caption, color = Theme.colors.textMuted)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar with Cyan Fill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = badgeColor,
                    backgroundColor = Theme.colors.surfaceTertiary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = com.codequest.academy.shared.ui.theme.AppTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = when { progress <= 0f -> "Ready to start"; progress >= 1f -> "Completed"; else -> "In progress" },
                    style = com.codequest.academy.shared.ui.theme.AppTypography.caption,
                    color = if (progress >= 1f) Theme.colors.success else Theme.colors.accentGold
                )
                Text(
                    text = when { progress <= 0f -> "Start Track →"; progress >= 1f -> "View Track →"; else -> "Continue →" },
                    style = com.codequest.academy.shared.ui.theme.AppTypography.button,
                    color = Theme.colors.accentCyan
                )
            }
        }
    }
}

private fun trackCode(track: TrackIdentity): String = when (track) {
    TrackIdentity.WEB_DEV -> "WD"
    TrackIdentity.APP_DEV -> "AD"
    TrackIdentity.CYBERSECURITY -> "CY"
    TrackIdentity.PROBLEM_SOLVING -> "PS"
    TrackIdentity.AI_ML -> "ML"
}
