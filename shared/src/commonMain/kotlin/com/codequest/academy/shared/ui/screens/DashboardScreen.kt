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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.models.TrackIdentity
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.DashboardState
import com.codequest.academy.shared.ui.viewmodels.DashboardViewModel

@Composable
fun DashboardScreen(navigation: Navigation, viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsState()
    if (state.isLoading) { LoadingPage("Restoring your learning progress…"); return }
    if (state.error != null) { ErrorPage("We couldn’t load your dashboard.", state.error!!, viewModel::loadDashboardData) { navigation.resetTo(Screen.TrackBrowser) }; return }

    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        val compact = maxWidth < 980.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (compact) 20.dp else 40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("∑ ", style = DisplayStyle.copy(color = Theme.colors.accentGold, fontSize = 38.sp))
                Text("Hello, ${state.userName}!", style = DisplayStyle.copy(color = Color.White))
            }
            Text("Master coding through mathematical structures.", style = AppTypography.body1, color = Theme.colors.textSecondary)
            Spacer(Modifier.height(28.dp))
            ContinueCard(state, navigation)
            Spacer(Modifier.height(28.dp))
            if (compact) {
                DashboardMainColumn(state, navigation)
                Spacer(Modifier.height(24.dp))
                DashboardContextColumn(state, navigation)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1.7f)) { DashboardMainColumn(state, navigation) }
                    Column(Modifier.weight(1f)) { DashboardContextColumn(state, navigation) }
                }
            }
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun ContinueCard(state: DashboardState, navigation: Navigation) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.colors.surfaceSecondary)
            .border(1.dp, Theme.colors.brandPrimary, RoundedCornerShape(16.dp)).padding(28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("∫ ", style = AppTypography.caption.copy(color = Theme.colors.accentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp))
            Text(if (state.summary.completedNodes == 0) "BEGIN YOUR FIRST LEARNING PATH" else "CONTINUE LEARNING", style = AppTypography.caption, color = Theme.colors.accentCyan)
        }
        Spacer(Modifier.height(6.dp))
        Text(state.nextNodeId?.let { friendlyNodeName(it) } ?: "All available learning is complete", style = AppTypography.h2.copy(color = Color.White))
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.nextLevelId != null) "Your next required step is ready. Progress is saved locally on this computer."
            else "Review your completed tracks or open the curriculum browser.",
            style = AppTypography.body2, color = Theme.colors.textSecondary
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Continue Learning", onClick = {
                if (state.nextLevelId != null && state.nextNodeId != null) navigateToNodeId(navigation, state.nextLevelId, state.nextNodeId)
                else navigation.navigateTo(Screen.TrackBrowser)
            })
            if (state.nextLevelId != null) SecondaryButton("View Level Map", onClick = { navigation.navigateTo(Screen.LevelOverview(state.nextLevelId)) })
        }
    }
}

@Composable
private fun DashboardMainColumn(state: DashboardState, navigation: Navigation) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("∫ ", style = AppTypography.h2.copy(color = Theme.colors.accentGold))
        Text("Track Progress", style = AppTypography.h2.copy(color = Color.White))
    }
    Spacer(Modifier.height(14.dp))
    TrackIdentity.values().forEach { track ->
        val progress = state.trackProgress[track.id] ?: 0f
        Column(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary)
                .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).clickable { navigation.navigateTo(Screen.TrackDetails(track.id)) }.padding(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${track.icon}  ${track.title}", style = AppTypography.h3.copy(color = Color.White))
                Text("${(progress * 100).toInt()}%", style = AppTypography.button, color = Theme.colors.accentCyan)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), Theme.colors.accentCyan, Theme.colors.surfaceTertiary)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("f(x) ", style = AppTypography.h2.copy(color = Theme.colors.accentGold))
        Text("Current Project", style = AppTypography.h2.copy(color = Color.White))
    }
    Spacer(Modifier.height(14.dp))
    InfoCard("No project in progress", "Pass a level’s Final Quiz with at least 75% to unlock its project workspace.")
}

@Composable
private fun DashboardContextColumn(state: DashboardState, navigation: Navigation) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("π ", style = AppTypography.h2.copy(color = Theme.colors.accentGold))
        Text("Overall Learning", style = AppTypography.h2.copy(color = Color.White))
    }
    Spacer(Modifier.height(14.dp))
    Column(Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(12.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).padding(20.dp)) {
        StatRow("Tracks started", state.summary.tracksStarted.toString())
        StatRow("Levels completed", state.summary.levelsCompleted.toString())
        StatRow("Learning steps", state.summary.completedNodes.toString())
        StatRow("Projects submitted", state.summary.projectsSubmitted.toString())
    }
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("√ ", style = AppTypography.h2.copy(color = Theme.colors.accentGold))
        Text("Weak Skills", style = AppTypography.h2.copy(color = Color.White))
    }
    Spacer(Modifier.height(14.dp))
    InfoCard("No weak skills identified yet", "Complete practices and quizzes to receive recommendations.")
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("∑ ", style = AppTypography.h2.copy(color = Theme.colors.accentGold))
        Text("Recent Activity", style = AppTypography.h2.copy(color = Color.White))
    }
    Spacer(Modifier.height(14.dp))
    if (state.recentActivity.isEmpty()) InfoCard("No activity yet", "Complete your first learning step and it will appear here.")
    else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.recentActivity.take(6).forEach { activity -> InfoCard(activity.title, activity.eventType.replaceFirstChar { it.uppercase() }) }
    }
    Spacer(Modifier.height(18.dp))
    SecondaryButton("Open Progress", onClick = { navigation.navigateTo(Screen.Progress) })
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AppTypography.body2, color = Theme.colors.textSecondary)
        Text(value, style = AppTypography.button.copy(color = Theme.colors.accentCyan))
    }
}

private fun friendlyNodeName(nodeId: String): String = when {
    nodeId.endsWith("-DIAG") -> "Diagnostic"
    nodeId.endsWith("-CS") -> "Cheat Sheet"
    nodeId.endsWith("-PR") -> "Practice ${nodeId.substringAfterLast("-L").substringBefore("-").trimStart('0')}"
    nodeId.endsWith("-CH") -> "Challenge ${nodeId.substringAfterLast("-L").substringBefore("-").trimStart('0')}"
    nodeId.endsWith("-MR1") -> "Mixed Review 1"
    nodeId.endsWith("-MR2") -> "Mixed Review 2"
    nodeId.endsWith("-AR") -> "Adaptive Review"
    nodeId.endsWith("-QUIZ") -> "Final Quiz"
    nodeId.endsWith("-PROJECT") -> "Level Project"
    nodeId.endsWith("-REFLECTION") -> "Project Reflection"
    nodeId.endsWith("-MASTERY") -> "Optional Mastery Challenge"
    "-L" in nodeId -> "Lesson ${nodeId.substringAfterLast("-L").trimStart('0')}"
    else -> nodeId
}

private fun navigateToNodeId(navigation: Navigation, levelId: String, id: String) {
    when {
        id.endsWith("-DIAG") -> navigation.navigateTo(Screen.Diagnostic(levelId, id))
        id.endsWith("-CS") -> navigation.navigateTo(Screen.CheatSheet(levelId, id))
        id.endsWith("-PR") -> navigation.navigateTo(Screen.Practice(levelId, id))
        id.endsWith("-CH") -> navigation.navigateTo(Screen.Challenge(levelId, id))
        id.endsWith("-MR1") || id.endsWith("-MR2") -> navigation.navigateTo(Screen.MixedReview(levelId, id))
        id.endsWith("-AR") -> navigation.navigateTo(Screen.AdaptiveReview(levelId, id))
        id.endsWith("-QUIZ") -> navigation.navigateTo(Screen.FinalQuiz(levelId, id))
        id.endsWith("-PROJECT") -> navigation.navigateTo(Screen.Project(levelId, id))
        id.endsWith("-REFLECTION") -> navigation.navigateTo(Screen.ProjectReflection(levelId, id))
        id.endsWith("-MASTERY") -> navigation.navigateTo(Screen.MasteryChallenge(levelId, id))
        else -> navigation.navigateTo(Screen.Lesson(levelId, id))
    }
}
