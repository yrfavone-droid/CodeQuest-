package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.ProjectUiState
import com.codequest.academy.shared.ui.viewmodels.ProjectViewModel

@Composable
fun ProjectScreen(navigation: Navigation, viewModel: ProjectViewModel, levelId: String) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        ProjectUiState.Loading -> LoadingPage("Loading project workspace…")
        is ProjectUiState.NotFound -> ContentNotFoundState("Project not found", current.message, { navigation.pop() }, { navigation.resetTo(Screen.Dashboard) })
        is ProjectUiState.Locked -> ContentNotFoundState("Project locked", current.message, { navigation.pop() }, { navigation.navigateTo(Screen.LevelOverview(levelId)) })
        is ProjectUiState.Error -> ErrorPage("We couldn’t load this project.", current.message, viewModel::loadProject) { navigation.navigateTo(Screen.LevelOverview(levelId)) }
        is ProjectUiState.Loaded -> {
            if (current.submitted) { StateCompletionPage("Project submitted", "Your draft and submission were saved. Project Reflection is now available.", "Return to Learning Map") { navigation.navigateTo(Screen.LevelOverview(levelId)) }; return }
            var confirm by remember { mutableStateOf(false) }
            if (confirm) AlertDialog(
                onDismissRequest = { confirm = false }, title = { Text("Submit project?") }, text = { Text("Your current draft and milestone evidence will be saved as the submission.") },
                confirmButton = { PrimaryButton("Submit", onClick = { confirm = false; viewModel.submitProject() }) },
                dismissButton = { SecondaryButton("Cancel", onClick = { confirm = false }) }
            )
            Column(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Save & Close", onClick = { viewModel.saveDraft(); navigation.pop() })
                    Text(current.title, style = AppTypography.h3, modifier = Modifier.weight(1f))
                    SecondaryButton("Save Draft", onClick = viewModel::saveDraft)
                    PrimaryButton("Submit Project", onClick = { confirm = true })
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                    val wide = maxWidth >= 1200.dp
                    if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MilestonesPanel(current, viewModel, Modifier.width(270.dp).fillMaxHeight(), scrollable = true)
                        WorkspacePanel(current, viewModel, Modifier.weight(1f).fillMaxHeight(), scrollable = true)
                        RequirementsPanel(current, Modifier.width(310.dp).fillMaxHeight(), scrollable = true)
                    } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        WorkspacePanel(current, viewModel, Modifier.fillMaxWidth(), scrollable = false)
                        MilestonesPanel(current, viewModel, Modifier.fillMaxWidth(), scrollable = false)
                        RequirementsPanel(current, Modifier.fillMaxWidth(), scrollable = false)
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun MilestonesPanel(state: ProjectUiState.Loaded, viewModel: ProjectViewModel, modifier: Modifier, scrollable: Boolean = true) {
    val containerModifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(containerModifier.background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(18.dp)) {
        Text("Milestones", style = AppTypography.h2); Spacer(Modifier.height(10.dp))
        state.milestones.forEach { milestone -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
            Checkbox(milestone.number in state.completedMilestones, { viewModel.toggleMilestone(milestone.number) }); Column { Text("${milestone.number}. ${milestone.title}", style = AppTypography.button); Text(milestone.deliverable, style = AppTypography.caption, color = Theme.colors.textSecondary) }
        } }
    }
}

@Composable private fun WorkspacePanel(state: ProjectUiState.Loaded, viewModel: ProjectViewModel, modifier: Modifier, scrollable: Boolean = true) {
    val containerModifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(containerModifier.background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(24.dp)) {
        Text(state.title, style = DisplayStyle); Spacer(Modifier.height(12.dp)); Text(state.problem, style = AppTypography.body1); Spacer(Modifier.height(14.dp)); Text(state.brief, style = AppTypography.body2, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(24.dp)); Text("Project notes and structural evidence", style = AppTypography.h3); Spacer(Modifier.height(8.dp))
        OutlinedTextField(state.notes, viewModel::updateNotes, Modifier.fillMaxWidth().heightIn(min = 260.dp), label = { Text("Contracts, diagrams, traces, tests, and decisions") })
    }
}

@Composable private fun RequirementsPanel(state: ProjectUiState.Loaded, modifier: Modifier, scrollable: Boolean = true) {
    val containerModifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(containerModifier.background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).padding(18.dp)) {
        Text("Mandatory features", style = AppTypography.h3); Spacer(Modifier.height(8.dp)); state.mandatoryFeatures.forEach { Text("• $it", style = AppTypography.caption, modifier = Modifier.padding(bottom = 7.dp)) }
        Spacer(Modifier.height(16.dp)); Text("Rubric · 100 points", style = AppTypography.h3); Spacer(Modifier.height(8.dp)); state.rubric.forEach { Text("• $it", style = AppTypography.caption, modifier = Modifier.padding(bottom = 7.dp)) }
    }
}
