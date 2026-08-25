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
import com.codequest.academy.shared.ui.components.StatusPill
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
    if (state.isLoading) { LoadingPage("Restoring your workspace…"); return }
    if (state.error != null) { ErrorPage("We couldn’t load your workspace.", state.error!!, viewModel::loadDashboardData) { navigation.resetTo(Screen.TrackBrowser) }; return }

    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        val compact = maxWidth < 1060.dp
        val pagePadding = if (compact) 22.dp else 40.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = pagePadding, vertical = 34.dp)) {
            DashboardHeader(state.userName)
            Spacer(Modifier.height(28.dp))
            NextActionPanel(state, navigation)
            Spacer(Modifier.height(28.dp))
            LearningMetrics(state)
            Spacer(Modifier.height(32.dp))
            if (compact) {
                LearningPortfolio(state, navigation)
                Spacer(Modifier.height(28.dp))
                ActivityColumn(state, navigation)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1.65f)) { LearningPortfolio(state, navigation) }
                    Column(Modifier.weight(1f)) { ActivityColumn(state, navigation) }
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun DashboardHeader(name: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column {
            Text("OVERVIEW", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp), color = Theme.colors.accentCyan)
            Spacer(Modifier.height(6.dp))
            Text("Welcome back, $name", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
            Spacer(Modifier.height(7.dp))
            Text("Your private, local learning workspace is ready.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        }
        StatusPill("OFFLINE READY", Theme.colors.success, Theme.colors.successSoft)
    }
}

@Composable
private fun NextActionPanel(state: DashboardState, navigation: Navigation) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surfaceSecondary)
            .border(1.dp, Theme.colors.brandPrimary.copy(alpha = .52f), RoundedCornerShape(16.dp))
            .padding(28.dp)
    ) {
        Text("NEXT ACTION", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.accentCyan)
        Spacer(Modifier.height(10.dp))
        Text(state.nextNodeId?.let { friendlyNodeName(it) } ?: "Review your learning portfolio", style = AppTypography.h1.copy(color = Theme.colors.textPrimary))
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.nextLevelId != null) "Resume the next required activity. Completion is persisted to this device."
            else "All currently available activities are complete. Explore a track or review past work.",
            style = AppTypography.body2, color = Theme.colors.textSecondary
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Resume learning", onClick = {
                if (state.nextLevelId != null && state.nextNodeId != null) navigateToNodeId(navigation, state.nextLevelId, state.nextNodeId)
                else navigation.navigateTo(Screen.TrackBrowser)
            })
            if (state.nextLevelId != null) SecondaryButton("Open level map", onClick = { navigation.navigateTo(Screen.LevelOverview(state.nextLevelId)) })
        }
    }
}

@Composable
private fun LearningMetrics(state: DashboardState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricTile("TRACKS", state.summary.tracksStarted.toString(), "started", Theme.colors.accentCyan, Modifier.weight(1f))
        MetricTile("LEVELS", state.summary.levelsCompleted.toString(), "completed", Theme.colors.brandPrimaryHover, Modifier.weight(1f))
        MetricTile("ACTIVITIES", state.summary.completedNodes.toString(), "saved locally", Theme.colors.success, Modifier.weight(1f))
        MetricTile("PROJECTS", state.summary.projectsSubmitted.toString(), "submitted", Theme.colors.accentGold, Modifier.weight(1f))
    }
}

@Composable
private fun MetricTile(label: String, value: String, detail: String, accent: Color, modifier: Modifier) {
    Column(modifier.heightIn(min = 112.dp).clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text(label, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = .8.sp), color = Theme.colors.textMuted)
        Spacer(Modifier.height(8.dp)); Text(value, style = AppTypography.h1.copy(fontSize = 25.sp), color = accent)
        Text(detail, style = AppTypography.caption, color = Theme.colors.textSecondary)
    }
}

@Composable
private fun LearningPortfolio(state: DashboardState, navigation: Navigation) {
    SectionTitle("Learning portfolio", "Progress across your current technical domains")
    Spacer(Modifier.height(14.dp))
    TrackIdentity.values().forEach { track ->
        val progress = state.trackProgress[track.id] ?: 0f
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary)
                .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).clickable { navigation.navigateTo(Screen.TrackDetails(track.id)) }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(track.primaryColor).border(1.dp, track.primaryColor, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                Text(trackCode(track), style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(track.title, style = AppTypography.body2.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary)
                    Text("${(progress * 100).toInt()}%", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = track.primaryColor)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), track.primaryColor, Theme.colors.surfaceTertiary)
            }
        }
    }
}

@Composable
private fun ActivityColumn(state: DashboardState, navigation: Navigation) {
    SectionTitle("Review signal", "Recommendations use saved attempt history")
    Spacer(Modifier.height(14.dp))
    InfoCard("No weak skills identified", "Complete practices and final quizzes to generate a focused review plan.")
    Spacer(Modifier.height(22.dp))
    SectionTitle("Recent activity", "Most recent local learning events")
    Spacer(Modifier.height(14.dp))
    if (state.recentActivity.isEmpty()) InfoCard("No activity yet", "Your completed learning activities will appear here.")
    else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.recentActivity.take(5).forEach { activity -> InfoCard(activity.title, activity.eventType.replaceFirstChar { it.uppercase() }) }
    }
    Spacer(Modifier.height(18.dp)); SecondaryButton("Open progress report", onClick = { navigation.navigateTo(Screen.Progress) })
}

@Composable
private fun SectionTitle(title: String, detail: String) {
    Column {
        Text(title, style = AppTypography.h2.copy(color = Theme.colors.textPrimary))
        Spacer(Modifier.height(3.dp)); Text(detail, style = AppTypography.caption, color = Theme.colors.textSecondary)
    }
}

private fun friendlyNodeName(nodeId: String): String = when {
    nodeId.endsWith("-DIAG") -> "Diagnostic"
    nodeId.endsWith("-CS") -> "Reference sheet"
    nodeId.endsWith("-PR") -> "Practice activity"
    nodeId.endsWith("-CH") -> "Challenge activity"
    nodeId.endsWith("-MR1") || nodeId.endsWith("-MR2") -> "Mixed review"
    nodeId.endsWith("-AR") -> "Adaptive review"
    nodeId.endsWith("-QUIZ") -> "Final quiz"
    nodeId.endsWith("-PROJECT") -> "Level project"
    nodeId.endsWith("-REFLECTION") -> "Project reflection"
    nodeId.endsWith("-MASTERY") -> "Mastery challenge"
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

private fun trackCode(track: TrackIdentity): String = when (track) {
    TrackIdentity.WEB_DEV -> "WD"
    TrackIdentity.APP_DEV -> "AD"
    TrackIdentity.CYBERSECURITY -> "CY"
    TrackIdentity.PROBLEM_SOLVING -> "PS"
    TrackIdentity.AI_ML -> "ML"
}
