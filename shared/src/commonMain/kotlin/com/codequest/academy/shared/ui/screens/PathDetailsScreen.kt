package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.PathDetailsUiState
import com.codequest.academy.shared.ui.viewmodels.PathDetailsViewModel

@Composable
fun PathDetailsScreen(navigation: Navigation, viewModel: PathDetailsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        PathDetailsUiState.Loading -> LoadingPage("Loading path…")
        PathDetailsUiState.Empty -> EmptyPage("This path has no levels.", "Retry the curriculum load or choose another path.", "Back to Tracks") { navigation.navigateTo(Screen.TrackBrowser) }
        is PathDetailsUiState.Error -> ContentNotFoundState("Path not found", state.message, { navigation.pop() }, { navigation.resetTo(Screen.Dashboard) })
        is PathDetailsUiState.Loaded -> {
            var lockedMessage by remember { mutableStateOf<String?>(null) }
            if (lockedMessage != null) androidx.compose.material.AlertDialog(
                onDismissRequest = { lockedMessage = null }, title = { Text("Level locked") }, text = { Text(lockedMessage!!) },
                confirmButton = { SecondaryButton("Got it", onClick = { lockedMessage = null }) }
            )
            LazyColumn(
                Modifier.fillMaxSize().background(Theme.colors.appBackground),
                contentPadding = PaddingValues(40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SecondaryButton("Back", onClick = { navigation.pop() }); Spacer(Modifier.height(22.dp))
                    Text(state.track.title, style = AppTypography.caption, color = state.track.primaryColor)
                    Text(state.pathTitle, style = DisplayStyle); Spacer(Modifier.height(8.dp))
                    Text("Five levels · approximately 5–8 weeks", style = AppTypography.body1, color = Theme.colors.textSecondary)
                    Spacer(Modifier.height(14.dp)); LinearProgressIndicator(state.progress, Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), state.track.primaryColor, Theme.colors.surfaceTertiary)
                    Spacer(Modifier.height(20.dp))
                }
                items(state.levels, key = { it.level.id }) { item ->
                    val level = item.level
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary)
                            .border(1.dp, if (item.status == "Locked") Theme.colors.borderDefault else state.track.primaryColor.copy(alpha = .55f), RoundedCornerShape(14.dp))
                            .clickable {
                                if (item.status == "Locked") lockedMessage = "Complete the previous level’s required Project Reflection to unlock ${level.code}."
                                else navigation.navigateTo(Screen.LevelOverview(level.id))
                            }.padding(22.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(level.code, style = AppTypography.button, color = state.track.primaryColor); Text(item.status, style = AppTypography.caption, color = if (item.status == "Locked") Theme.colors.locked else Theme.colors.textSecondary)
                            }
                            Spacer(Modifier.height(5.dp)); Text(level.title, style = AppTypography.h3)
                            Spacer(Modifier.height(7.dp)); Text("${level.difficulty.replaceFirstChar { it.uppercase() }} · ${level.estimated_days} days · ${level.estimated_total_minutes} min · ${level.lessons.size} lessons", style = AppTypography.caption, color = Theme.colors.textSecondary)
                            Spacer(Modifier.height(10.dp)); LinearProgressIndicator(item.progress, Modifier.fillMaxWidth(.75f).height(5.dp).clip(RoundedCornerShape(3.dp)), state.track.primaryColor, Theme.colors.surfaceTertiary)
                        }
                        Spacer(Modifier.width(20.dp))
                        SecondaryButton(when (item.status) { "Completed" -> "Review"; "In Progress" -> "Continue"; "Locked" -> "Locked"; else -> "Start" }, onClick = {
                            if (item.status == "Locked") lockedMessage = "Complete the previous level’s required Project Reflection to unlock ${level.code}." else navigation.navigateTo(Screen.LevelOverview(level.id))
                        })
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(state.track.softColor).border(1.dp, state.track.primaryColor.copy(alpha=.4f), RoundedCornerShape(18.dp)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Path Capstone · Locked", style = AppTypography.h2); Spacer(Modifier.height(8.dp)); Text("Complete the Level 5 project and reflection to unlock this capstone.", style = AppTypography.body2, color = Theme.colors.textSecondary)
                    }
                    Spacer(Modifier.height(44.dp))
                }
            }
        }
    }
}
