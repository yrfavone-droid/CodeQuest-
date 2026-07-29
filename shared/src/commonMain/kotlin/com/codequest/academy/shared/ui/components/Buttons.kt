package com.codequest.academy.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.theme.Theme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    color: Color = Theme.colors.brandPrimary
) {
    val gradientBrush = if (enabled) {
        if (color == Theme.colors.brandPrimary) {
            Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF5A3FD5)))
        } else {
            Brush.linearGradient(listOf(color, color.copy(alpha = 0.85f)))
        }
    } else {
        Brush.linearGradient(listOf(Theme.colors.surfaceTertiary, Theme.colors.surfaceTertiary))
    }
    val textColor = if (enabled) Color(0xFFE0E0E0) else Theme.colors.textMuted
    
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(gradientBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White),
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = textColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp).padding(end = 8.dp)
                    )
                }
                Text(
                    text = text,
                    style = com.codequest.academy.shared.ui.theme.AppTypography.button,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val borderColor = if (enabled) Color(0xFF00D9FF) else Theme.colors.borderDefault
    val textColor = if (enabled) Color(0xFF00D9FF) else Theme.colors.textMuted
    
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color(0xFF00D9FF)),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp).padding(end = 8.dp)
                )
            }
            Text(
                text = text,
                style = com.codequest.academy.shared.ui.theme.AppTypography.button,
                color = textColor
            )
        }
    }
}

@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val textColor = if (enabled) Color(0xFFB0B0B0) else Theme.colors.textMuted
    val borderColor = Color(0xFF2A2A3E)
    
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Theme.colors.surfaceTertiary),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = com.codequest.academy.shared.ui.theme.AppTypography.button,
            color = textColor
        )
    }
}
