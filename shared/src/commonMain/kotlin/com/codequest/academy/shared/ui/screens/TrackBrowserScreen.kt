package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.models.TrackIdentity
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.components.TrackCard
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.TrackBrowserUiState
import com.codequest.academy.shared.ui.viewmodels.TrackBrowserViewModel

@Composable
fun TrackBrowserScreen(navigation: Navigation, viewModel: TrackBrowserViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        val padding = if (maxWidth < 900.dp) 20.dp else 40.dp
        val columns = when { maxWidth >= 1320.dp -> 3; maxWidth >= 1000.dp -> 2; else -> 1 }
        Column(Modifier.fillMaxSize().padding(start = padding, end = padding, top = 32.dp)) {
            Text("CURRICULUM PORTFOLIO", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.accentCyan)
            Spacer(Modifier.height(6.dp))
            Text("Choose a technical domain", style = DisplayStyle, color = Theme.colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Each portfolio develops a different system of thinking. Progress is recorded locally as you work.", style = AppTypography.body1, color = Theme.colors.textSecondary)
            Spacer(Modifier.height(28.dp))
            when (val state = uiState) {
                TrackBrowserUiState.Loading -> LazyVerticalGrid(
                    columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)
                ) { items(5) { SkeletonCard(Modifier.height(310.dp)) } }
                TrackBrowserUiState.Empty -> EmptyPage(
                    title = "No curriculum is installed.", detail = "Install or restore the CodeQuest curriculum to browse tracks.",
                    actionLabel = "Retry", onAction = viewModel::loadPaths
                )
                is TrackBrowserUiState.Error -> ErrorPage(
                    title = "We couldn’t load the curriculum.", detail = state.message,
                    onRetry = viewModel::loadPaths, onBack = { navigation.resetTo(Screen.Dashboard) }
                )
                is TrackBrowserUiState.Loaded -> LazyVerticalGrid(
                    columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    items(TrackIdentity.values().toList(), key = { it.id }) { track ->
                        TrackCard(
                            track = track,
                            progress = state.progress[track.id] ?: 0f,
                            pathNames = state.paths.filter { it.track_id == track.id }.map { it.title },
                            onClick = { navigation.navigateTo(Screen.TrackDetails(track.id)) }
                        )
                    }
                }
            }
        }
    }
}
