package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.TrackDetailsUiState
import com.codequest.academy.shared.ui.viewmodels.TrackDetailsViewModel

@Composable
fun TrackDetailsScreen(navigation: Navigation, viewModel: TrackDetailsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        TrackDetailsUiState.Loading -> LoadingPage("Loading track details…")
        is TrackDetailsUiState.Error -> ContentNotFoundState("Track not found", state.message, { navigation.pop() }, { navigation.resetTo(Screen.Dashboard) })
        is TrackDetailsUiState.Loaded -> BoxWithConstraints(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
            val compact = maxWidth < 900.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (compact) 20.dp else 40.dp)) {
                SecondaryButton("Tracks", onClick = { navigation.pop() })
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(state.track.softColor).border(1.dp, state.track.primaryColor.copy(alpha = .35f), RoundedCornerShape(18.dp)).padding(32.dp)) {
                    Text(state.track.icon, style = DisplayStyle)
                    Spacer(Modifier.height(12.dp))
                    Text(state.track.title, style = DisplayStyle, color = Theme.colors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(state.track.description, style = AppTypography.body1, color = Theme.colors.textSecondary)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text("2 paths", style = AppTypography.button); Text("10 levels", style = AppTypography.button); Text("${(state.progress * 100).toInt()}% complete", style = AppTypography.button)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(state.progress, Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), state.track.primaryColor, Theme.colors.surfacePrimary)
                }
                Spacer(Modifier.height(30.dp))
                Text("Learning Paths", style = AppTypography.h2)
                Spacer(Modifier.height(16.dp))
                if (compact) Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    state.paths.forEach { path -> PathCard(path.title, state.pathProgress[path.id] ?: 0f, state.track.primaryColor) { navigation.navigateTo(Screen.PathDetails(path.id)) } }
                } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    state.paths.forEach { path -> Box(Modifier.weight(1f)) { PathCard(path.title, state.pathProgress[path.id] ?: 0f, state.track.primaryColor) { navigation.navigateTo(Screen.PathDetails(path.id)) } } }
                }
                Spacer(Modifier.height(30.dp))
                InfoCard("Track skills overview", "Build structural models, trace state, test boundaries, and explain why each solution works across both paths.")
                Spacer(Modifier.height(18.dp))
                InfoCard("Track Final Project · Locked", "Complete both path capstones to unlock the integrated ${state.track.title} final project.")
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun PathCard(title: String, progress: Float, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(24.dp)) {
        Text(title, style = AppTypography.h3); Spacer(Modifier.height(8.dp)); Text("5 levels · approximately 5–8 weeks", style = AppTypography.body2, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(20.dp)); Text("${(progress * 100).toInt()}% complete", style = AppTypography.caption, color = accent)
        Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), accent, Theme.colors.surfaceTertiary)
        Spacer(Modifier.height(18.dp)); Text(if (progress > 0f) "Continue Path →" else "Start Path →", style = AppTypography.button, color = accent)
    }
}
