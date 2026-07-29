package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.LessonUiState
import com.codequest.academy.shared.ui.viewmodels.LessonViewModel

@Composable
fun LessonScreen(navigation: Navigation, viewModel: LessonViewModel, levelId: String) {
    val uiState by viewModel.state.collectAsState()
    when (val state = uiState) {
        LessonUiState.Loading -> LoadingPage("Loading lesson…")
        is LessonUiState.NotFound -> ContentNotFoundState("Lesson not found", state.message, { navigation.pop() }, { navigation.resetTo(Screen.Dashboard) })
        is LessonUiState.Error -> ErrorPage("We couldn’t load this lesson.", state.message, viewModel::loadLesson) { navigation.navigateTo(Screen.LevelOverview(levelId)) }
        is LessonUiState.Loaded -> {
            if (state.isFinished) {
                StateCompletionPage("Lesson complete", "Your progress was saved and the related practice is now available.", "Return to Learning Map") { navigation.navigateTo(Screen.LevelOverview(levelId)) }
                return
            }
            val section = state.sections.getOrNull(state.currentSection)
            Column(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SecondaryButton("Back to Map", onClick = { navigation.pop() })
                    LinearProgressIndicator(
                        progress = if (state.sections.isEmpty()) 0f else (state.currentSection + 1f) / state.sections.size,
                        modifier = Modifier.weight(1f).height(8.dp).padding(top = 18.dp).clip(RoundedCornerShape(4.dp)),
                        color = Theme.colors.brandPrimary, backgroundColor = Theme.colors.surfaceTertiary
                    )
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).widthIn(max = 820.dp).fillMaxWidth().padding(horizontal = 40.dp, vertical = 16.dp)) {
                    Text("${state.estimatedMinutes} minutes · structural lesson", style = AppTypography.caption, color = Theme.colors.brandPrimary)
                    Text(state.title, style = DisplayStyle); Spacer(Modifier.height(8.dp)); Text(state.goal, style = AppTypography.body1, color = Theme.colors.textSecondary)
                    Spacer(Modifier.height(28.dp))
                    if (section != null) Column(Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(28.dp)) {
                        Text(section.type.replace('_', ' ').uppercase(), style = AppTypography.caption, color = Theme.colors.brandPrimary)
                        Spacer(Modifier.height(6.dp)); Text(section.title, style = AppTypography.h2); Spacer(Modifier.height(14.dp)); Text(section.body, style = AppTypography.body1)
                    }
                    if (state.currentSection == state.sections.lastIndex && state.checkpoints.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp)); Text("Checkpoints", style = AppTypography.h2); Spacer(Modifier.height(12.dp))
                        state.checkpoints.forEach { checkpoint -> InfoCard(checkpoint.prompt, checkpoint.answer, Modifier.padding(bottom = 10.dp)) }
                    }
                    Spacer(Modifier.height(40.dp))
                }
                Row(Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary).padding(20.dp, 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    SecondaryButton("Previous section", onClick = viewModel::previousSection, enabled = state.currentSection > 0)
                    if (state.currentSection < state.sections.lastIndex) PrimaryButton("Continue", onClick = viewModel::nextSection)
                    else PrimaryButton("Complete Lesson", onClick = viewModel::finishLesson)
                }
            }
        }
    }
}
