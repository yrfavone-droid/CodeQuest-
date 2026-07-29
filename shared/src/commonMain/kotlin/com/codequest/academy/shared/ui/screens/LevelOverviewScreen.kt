package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.models.Level
import com.codequest.academy.shared.models.TimelineNode
import com.codequest.academy.shared.models.TrackIdentity
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.LevelOverviewUiState
import com.codequest.academy.shared.ui.viewmodels.LevelOverviewViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun LevelOverviewScreen(navigation: Navigation, viewModel: LevelOverviewViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        LevelOverviewUiState.Loading -> LoadingPage("Loading level map…")
        is LevelOverviewUiState.NotFound -> ContentNotFoundState(
            title = "Level not found",
            detail = "We couldn’t find '${state.levelId}' in the installed curriculum.",
            onBack = { navigation.pop() },
            onDashboard = { navigation.resetTo(Screen.Dashboard) }
        )
        is LevelOverviewUiState.Error -> ErrorPage(
            title = "We couldn’t load this level.",
            detail = state.message,
            onRetry = viewModel::loadLevel,
            onBack = { navigation.pop() }
        )
        is LevelOverviewUiState.Loaded -> LearningMap(state.level, state.nodeStates, navigation)
    }
}

@Composable
private fun LearningMap(level: Level, states: Map<String, String>, navigation: Navigation) {
    val track = TrackIdentity.fromId(level.track_id) ?: TrackIdentity.WEB_DEV
    var lockedNode by remember { mutableStateOf<TimelineNode?>(null) }
    val required = level.timeline_nodes.filter { it.required }
    val completed = required.count { states[it.id] == "completed" }
    val progress = if (required.isEmpty()) 0f else completed.toFloat() / required.size

    if (lockedNode != null) {
        val node = lockedNode!!
        val previous = level.timeline_nodes.filter { it.order < node.order && it.required }.lastOrNull { states[it.id] != "completed" }
        AlertDialog(
            onDismissRequest = { lockedNode = null },
            title = { Text("Complete the required step first") },
            text = { Text(previous?.let { "Finish ${nodeTitle(level, it)} to unlock ${nodeTitle(level, node)}." } ?: "Complete the earlier required activities to unlock this step.") },
            confirmButton = { SecondaryButton("Got it", onClick = { lockedNode = null }) }
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        val compact = maxWidth < 900.dp
        val pagePadding = if (compact) 20.dp else 40.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = pagePadding, end = pagePadding, top = 28.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(Modifier.widthIn(max = 920.dp).fillMaxWidth()) {
                    SecondaryButton("Back to Path", onClick = { navigation.pop() })
                    Spacer(Modifier.height(24.dp))
                    Text("${level.code} · ${level.difficulty.replaceFirstChar { it.uppercase() }}", style = AppTypography.caption, color = track.primaryColor)
                    Text(level.title, style = DisplayStyle, color = Theme.colors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(level.goal, style = AppTypography.body1, color = Theme.colors.textSecondary)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("${level.timeline_nodes.size} learning steps", style = AppTypography.caption)
                        Text("${level.estimated_total_minutes} minutes", style = AppTypography.caption)
                        Text("${(progress * 100).toInt()}% complete", style = AppTypography.caption)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress, Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), track.primaryColor, Theme.colors.surfaceTertiary)
                    Spacer(Modifier.height(36.dp))
                }
            }
            itemsIndexed(level.timeline_nodes.sortedBy { it.order }, key = { _, node -> node.id }) { index, node ->
                val status = states[node.id] ?: "locked"
                LearningNodeRow(
                    node = node,
                    title = nodeTitle(level, node),
                    status = status,
                    accent = track.primaryColor,
                    soft = track.softColor,
                    isLast = index == level.timeline_nodes.lastIndex,
                    onClick = {
                        if (status == "locked") lockedNode = node
                        else navigateToTimelineNode(navigation, level.id, node)
                    }
                )
            }
        }
    }
}

@Composable
private fun LearningNodeRow(node: TimelineNode, title: String, status: String, accent: Color, soft: Color, isLast: Boolean, onClick: () -> Unit) {
    val completed = status == "completed"
    val locked = status == "locked"
    val stateColor = when {
        completed -> Theme.colors.success
        locked -> Theme.colors.locked
        status == "failed" -> Theme.colors.error
        else -> accent
    }
    val stateSoft = when {
        completed -> Theme.colors.successSoft
        locked -> Theme.colors.lockedSoft
        status == "failed" -> Theme.colors.errorSoft
        else -> soft
    }
    Row(Modifier.widthIn(max = 760.dp).fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(stateSoft).border(2.dp, stateColor, CircleShape), contentAlignment = Alignment.Center) {
                Text(if (completed) "✓" else node.order.toString(), color = stateColor, fontWeight = FontWeight.Bold)
            }
            if (!isLast) Box(Modifier.width(3.dp).weight(1f).background(if (completed) Theme.colors.success else Theme.colors.borderDefault))
        }
        Spacer(Modifier.width(16.dp))
        Column(
            Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 18.dp).clip(RoundedCornerShape(14.dp))
                .background(Theme.colors.surfacePrimary).border(1.dp, if (locked) Theme.colors.borderDefault else stateColor, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick).padding(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(node.type.replace('_', ' ').uppercase(), style = AppTypography.caption, color = stateColor)
                Text(if (!node.required) "OPTIONAL" else status.replace('_', ' ').uppercase(), style = AppTypography.caption, color = stateColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(title, style = AppTypography.h3, color = if (locked) Theme.colors.textMuted else Theme.colors.textPrimary)
            if (locked) {
                Spacer(Modifier.height(4.dp))
                Text("Select to view the unlock requirement.", style = AppTypography.caption, color = Theme.colors.textMuted)
            }
        }
    }
}

private fun nodeTitle(level: Level, node: TimelineNode): String {
    fun JsonObject.string(name: String): String? = this[name]?.jsonPrimitive?.content
    fun objectTitle(element: kotlinx.serialization.json.JsonElement?): String? = (element as? JsonObject)?.string("title")
    return when (node.type) {
        "diagnostic" -> level.diagnostic?.title
        "cheat_sheet" -> objectTitle(level.cheat_sheet)
        "lesson" -> level.lessons.mapNotNull { it as? JsonObject }.firstOrNull { it.string("id") == node.content_ref }?.string("title")
        "practice" -> level.lessons.mapNotNull { it as? JsonObject }.mapNotNull { it["practice_set"] as? JsonObject }.firstOrNull { it.string("id") == node.content_ref }?.string("title")
        "challenge" -> level.lessons.mapNotNull { it as? JsonObject }.mapNotNull { it["challenge"] as? JsonObject }.firstOrNull { it.string("id") == node.content_ref }?.string("title")
        "mixed_review" -> level.mixed_reviews.mapNotNull { it as? JsonObject }.firstOrNull { it.string("id") == node.content_ref }?.string("title")
        "adaptive_review" -> objectTitle(level.adaptive_review)
        "final_quiz" -> objectTitle(level.final_quiz)
        "project" -> objectTitle(level.project)
        "project_reflection" -> objectTitle(level.project_reflection)
        "optional_mastery_challenge" -> objectTitle(level.optional_mastery_challenge)
        else -> null
    } ?: node.type.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun navigateToTimelineNode(navigation: Navigation, levelId: String, node: TimelineNode) {
    when (node.type) {
        "diagnostic" -> navigation.navigateTo(Screen.Diagnostic(levelId, node.content_ref))
        "cheat_sheet" -> navigation.navigateTo(Screen.CheatSheet(levelId, node.content_ref))
        "lesson" -> navigation.navigateTo(Screen.Lesson(levelId, node.content_ref))
        "practice" -> navigation.navigateTo(Screen.Practice(levelId, node.content_ref))
        "challenge" -> navigation.navigateTo(Screen.Challenge(levelId, node.content_ref))
        "mixed_review" -> navigation.navigateTo(Screen.MixedReview(levelId, node.content_ref))
        "adaptive_review" -> navigation.navigateTo(Screen.AdaptiveReview(levelId, node.content_ref))
        "final_quiz" -> navigation.navigateTo(Screen.FinalQuiz(levelId, node.content_ref))
        "project" -> navigation.navigateTo(Screen.Project(levelId, node.content_ref))
        "project_reflection" -> navigation.navigateTo(Screen.ProjectReflection(levelId, node.content_ref))
        "optional_mastery_challenge" -> navigation.navigateTo(Screen.MasteryChallenge(levelId, node.content_ref))
        else -> navigation.navigateTo(Screen.LearningMap(levelId))
    }
}
