package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.AssessmentUiState
import com.codequest.academy.shared.ui.viewmodels.CurriculumAssessmentViewModel

@Composable
fun AssessmentScreen(navigation: Navigation, viewModel: CurriculumAssessmentViewModel, levelId: String, kindLabel: String, accent: Color) {
    val uiState by viewModel.state.collectAsState()
    when (val state = uiState) {
        AssessmentUiState.Loading -> LoadingPage("Loading $kindLabel…")
        is AssessmentUiState.NotFound -> ContentNotFoundState("$kindLabel not found", state.message, { navigation.pop() }, { navigation.resetTo(Screen.Dashboard) })
        is AssessmentUiState.Error -> ErrorPage("We couldn’t load this $kindLabel.", state.message, viewModel::load) { navigation.navigateTo(Screen.LevelOverview(levelId)) }
        is AssessmentUiState.Completed -> AssessmentResult(state, kindLabel, levelId, navigation, viewModel, accent)
        is AssessmentUiState.Active -> AssessmentWorkspace(state, kindLabel, levelId, navigation, viewModel, accent)
    }
}

@Composable
private fun AssessmentWorkspace(state: AssessmentUiState.Active, kindLabel: String, levelId: String, navigation: Navigation, viewModel: CurriculumAssessmentViewModel, accent: Color) {
    val question = state.questions[state.index]
    val canCheck = state.selected.isNotEmpty() || state.freeText.isNotBlank()
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            SecondaryButton("Back to Map", onClick = { navigation.pop() })
            LinearProgressIndicator((state.index + 1f) / state.questions.size, Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)), accent, Theme.colors.surfaceTertiary)
            Text("${state.index + 1} / ${state.questions.size}", style = AppTypography.button)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 1100.dp
            if (wide) Row(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                QuestionColumn(state, question, viewModel, accent, Modifier.weight(1f).fillMaxHeight())
                HintPanel(state, question.hints, viewModel, Modifier.width(300.dp).fillMaxHeight())
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
                QuestionColumn(state, question, viewModel, accent, Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp)); HintPanel(state, question.hints, viewModel, Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary).padding(24.dp, 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (state.graded && !state.correct) SecondaryButton("Retry", onClick = viewModel::retryQuestion) else SecondaryButton("Show hint", onClick = viewModel::showHint, enabled = state.hintsShown < question.hints.size.coerceAtMost(3))
            if (!state.graded) PrimaryButton("Check Answer", onClick = viewModel::checkAnswer, enabled = canCheck, color = accent)
            else PrimaryButton(if (state.index == state.questions.lastIndex) "Finish $kindLabel" else "Continue", onClick = viewModel::continueAfterFeedback, color = accent)
        }
    }
}

@Composable
private fun QuestionColumn(state: AssessmentUiState.Active, question: com.codequest.academy.shared.ui.viewmodels.AssessmentQuestion, viewModel: CurriculumAssessmentViewModel, accent: Color, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        Text(question.type.replace('_', ' ').uppercase(), style = AppTypography.caption, color = accent)
        Spacer(Modifier.height(5.dp)); Text(state.title, style = AppTypography.h2); Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(26.dp)) {
            Text(question.prompt, style = AppTypography.body1)
            Spacer(Modifier.height(22.dp))
            if (question.options.isNotEmpty()) question.options.forEach { option ->
                val selected = option.id in state.selected
                val isAnswer = option.id in question.correctAnswers
                val color = when { state.graded && isAnswer -> Theme.colors.success; state.graded && selected -> Theme.colors.error; selected -> accent; else -> Theme.colors.borderDefault }
                val background = when { state.graded && isAnswer -> Theme.colors.successSoft; state.graded && selected -> Theme.colors.errorSoft; selected -> accent.copy(alpha = .08f); else -> Theme.colors.surfacePrimary }
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(10.dp)).background(background).border(1.dp, color, RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.graded) { viewModel.selectOption(option.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.id, style = AppTypography.button, color = color); Spacer(Modifier.width(14.dp)); Text(option.text, style = AppTypography.body2)
                }
            } else OutlinedTextField(
                value = state.freeText, onValueChange = viewModel::updateFreeText, enabled = !state.graded,
                modifier = Modifier.fillMaxWidth(), label = { Text("Your answer") }, textStyle = AppTypography.body2
            )
        }
        if (state.graded) {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().background(if (state.correct) Theme.colors.successSoft else Theme.colors.errorSoft, RoundedCornerShape(14.dp)).padding(20.dp)) {
                Text(if (state.correct) "Correct" else "Not yet", style = AppTypography.h3, color = if (state.correct) Theme.colors.success else Theme.colors.error)
                Spacer(Modifier.height(6.dp)); Text(question.explanation, style = AppTypography.body2)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HintPanel(state: AssessmentUiState.Active, hints: List<String>, viewModel: CurriculumAssessmentViewModel, modifier: Modifier) {
    Column(modifier.background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(20.dp)) {
        Text("Question support", style = AppTypography.h3); Spacer(Modifier.height(8.dp)); Text("Hints are progressive. Try the structure first, then reveal only what you need.", style = AppTypography.body2, color = Theme.colors.textSecondary)
        hints.take(state.hintsShown).forEachIndexed { index, hint ->
            Spacer(Modifier.height(14.dp)); Text("Hint ${index + 1}", style = AppTypography.caption, color = Theme.colors.warning); Text(hint, style = AppTypography.body2)
        }
        if (hints.isEmpty()) { Spacer(Modifier.height(14.dp)); Text("No hint is needed for this activity.", style = AppTypography.caption, color = Theme.colors.textMuted) }
    }
}

@Composable
private fun AssessmentResult(state: AssessmentUiState.Completed, kindLabel: String, levelId: String, navigation: Navigation, viewModel: CurriculumAssessmentViewModel, accent: Color) {
    val percent = if (state.total == 0) 0 else state.score * 100 / state.total
    val title = when { kindLabel == "Final Quiz" && state.passed -> "Project Unlocked"; kindLabel == "Final Quiz" -> "Keep building your mastery"; else -> "$kindLabel complete" }
    val detail = if (kindLabel == "Final Quiz") "Score: ${state.score}/${state.total} ($percent%). Required: ${state.passingPercent}%." else "Score: ${state.score}/${state.total}. Your attempt and skill evidence were saved."
    Box(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(18.dp)).border(1.dp, if (state.passed) Theme.colors.success else Theme.colors.warning, RoundedCornerShape(18.dp)).padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (state.passed) "✓" else "↻", style = DisplayStyle, color = if (state.passed) Theme.colors.success else Theme.colors.warning)
            Text(title, style = DisplayStyle); Spacer(Modifier.height(10.dp)); Text(detail, style = AppTypography.body1, color = Theme.colors.textSecondary)
            Spacer(Modifier.height(24.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!state.passed) SecondaryButton("Retry", onClick = viewModel::load)
                PrimaryButton(if (kindLabel == "Final Quiz" && state.passed) "Open Learning Map" else "Return to Map", onClick = { navigation.navigateTo(Screen.LevelOverview(levelId)) }, color = accent)
            }
        }
    }
}
