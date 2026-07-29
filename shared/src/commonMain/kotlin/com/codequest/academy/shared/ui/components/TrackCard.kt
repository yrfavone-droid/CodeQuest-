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

    // Map track ID to vibrant squircle badge color
    val badgeColor = when (track.id) {
        "track_math_foundations" -> Color(0xFF7C5CFF) // Purple
        "track_algorithmic_math" -> Color(0xFF00D9FF) // Cyan
        "track_discrete_structures" -> Color(0xFFFFD700) // Gold
        "track_linear_algebra" -> Color(0xFF00FF41) // Lime
        else -> Theme.colors.brandPrimary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(16.dp), spotColor = Theme.colors.brandPrimary.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(BorderStroke(if (isSelected || isHovered) 2.dp else 1.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.heightIn(min = 300.dp)) {
            // High-end Vibrant Squircle Icon Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = track.icon,
                    fontSize = 30.sp
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
                    Text("• $path", style = com.codequest.academy.shared.ui.theme.AppTypography.caption, color = Theme.colors.accentCyan)
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
                Text("2 paths", style = com.codequest.academy.shared.ui.theme.AppTypography.caption, color = Theme.colors.textMuted)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(4.dp).background(Theme.colors.borderStrong, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("10 levels · 12 Quests", style = com.codequest.academy.shared.ui.theme.AppTypography.caption, color = Theme.colors.textMuted)
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
                    color = Theme.colors.accentCyan,
                    backgroundColor = Theme.colors.surfaceTertiary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = com.codequest.academy.shared.ui.theme.AppTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = Theme.colors.accentCyan
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
