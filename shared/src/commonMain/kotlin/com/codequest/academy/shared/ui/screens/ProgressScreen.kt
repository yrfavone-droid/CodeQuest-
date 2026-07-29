package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.LearningProgressSummary
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.TrackIdentity
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProgressScreen(navigation: Navigation, repository: ProgressRepository) {
    var summary by remember { mutableStateOf<LearningProgressSummary?>(null) }
    var tracks by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var activity by remember { mutableStateOf(emptyList<com.codequest.academy.shared.data.ActivityRecord>()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.Default) { summary = repository.getProgressSummary(); tracks = TrackIdentity.values().associate { it.id to repository.getTrackProgress(it.id) }; activity = repository.getRecentActivity(20) } }
    val data = summary ?: run { LoadingPage("Calculating real progress…"); return }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp)) {
        Text("Your Progress", style = DisplayStyle); Text("Only persisted completions and attempts are counted.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(24.dp)); Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("Tracks started" to data.tracksStarted, "Levels completed" to data.levelsCompleted, "Learning steps" to data.completedNodes, "Projects submitted" to data.projectsSubmitted).forEach { (label, value) -> InfoCard(label, value.toString(), Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(28.dp)); Text("Track progress", style = AppTypography.h2); Spacer(Modifier.height(12.dp))
        TrackIdentity.values().forEach { track ->
            val value = tracks[track.id] ?: 0f
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(track.title, style = AppTypography.h3); Text("${(value * 100).toInt()}%", style = AppTypography.button, color = track.primaryColor) }
                Spacer(Modifier.height(8.dp)); LinearProgressIndicator(value, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), track.primaryColor, Theme.colors.surfaceTertiary)
            }
        }
        Spacer(Modifier.height(18.dp)); Text("Activity history", style = AppTypography.h2); Spacer(Modifier.height(12.dp))
        if (activity.isEmpty()) InfoCard("No saved activity", "Complete a diagnostic, lesson, practice, challenge, quiz, or project to build your history.")
        else activity.forEach { InfoCard(it.title, it.eventType.replaceFirstChar { ch -> ch.uppercase() }, Modifier.padding(bottom = 10.dp)) }
        Spacer(Modifier.height(48.dp))
    }
}
