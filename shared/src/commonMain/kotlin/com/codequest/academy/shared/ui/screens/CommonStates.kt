package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp))
            .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(48.dp).background(Theme.colors.surfaceTertiary, RoundedCornerShape(12.dp)))
        Box(Modifier.fillMaxWidth(.62f).height(20.dp).background(Theme.colors.surfaceTertiary, RoundedCornerShape(6.dp)))
        Box(Modifier.fillMaxWidth().height(12.dp).background(Theme.colors.surfaceSecondary, RoundedCornerShape(6.dp)))
        Box(Modifier.fillMaxWidth(.82f).height(12.dp).background(Theme.colors.surfaceSecondary, RoundedCornerShape(6.dp)))
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(7.dp).background(Theme.colors.surfaceTertiary, RoundedCornerShape(4.dp)))
    }
}

@Composable
fun LoadingPage(status: String) {
    Box(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(40.dp), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 920.dp).fillMaxWidth()) {
            Text(status, style = AppTypography.body1, color = Theme.colors.textSecondary)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SkeletonCard(Modifier.weight(1f).height(260.dp))
                SkeletonCard(Modifier.weight(1f).height(260.dp))
            }
        }
    }
}

@Composable
fun ErrorPage(title: String, detail: String, onRetry: () -> Unit, onBack: () -> Unit) {
    StateCard("!", title, detail) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Back to Dashboard", onClick = onBack)
            PrimaryButton("Retry", onClick = onRetry)
        }
    }
}

@Composable
fun EmptyPage(title: String, detail: String, actionLabel: String, onAction: () -> Unit) {
    StateCard("○", title, detail) { PrimaryButton(actionLabel, onClick = onAction) }
}

@Composable
fun ContentNotFoundState(title: String, detail: String, onBack: () -> Unit, onDashboard: () -> Unit) {
    StateCard("?", title, detail) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Go Back", onClick = onBack)
            PrimaryButton("Return to Dashboard", onClick = onDashboard)
        }
    }
}

@Composable
private fun StateCard(icon: String, title: String, detail: String, actions: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(18.dp))
                .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(18.dp)).padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = AppTypography.h1, color = Theme.colors.brandPrimary)
            Spacer(Modifier.height(12.dp)); Text(title, style = AppTypography.h2, color = Theme.colors.textPrimary)
            Spacer(Modifier.height(8.dp)); Text(detail, style = AppTypography.body1, color = Theme.colors.textSecondary)
            Spacer(Modifier.height(24.dp)); actions()
        }
    }
}

@Composable
fun InfoCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp))
            .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(22.dp)
    ) {
        Text(title, style = AppTypography.h3, color = Theme.colors.textPrimary)
        Spacer(Modifier.height(6.dp)); Text(detail, style = AppTypography.body2, color = Theme.colors.textSecondary)
    }
}

@Composable
fun StateCompletionPage(title: String, detail: String, actionLabel: String, onAction: () -> Unit) {
    StateCard("✓", title, detail) { PrimaryButton(actionLabel, onClick = onAction) }
}
