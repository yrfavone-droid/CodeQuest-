package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.codequest.academy.shared.ui.viewmodels.DocumentNodeState
import com.codequest.academy.shared.ui.viewmodels.DocumentNodeViewModel

@Composable
fun DocumentNodeScreen(navigation: Navigation, viewModel: DocumentNodeViewModel, levelId: String, kind: String) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        DocumentNodeState.Loading -> LoadingPage("Loading $kind…")
        is DocumentNodeState.NotFound -> ContentNotFoundState("$kind not found", current.message, { navigation.pop() }, { navigation.resetTo(Screen.Dashboard) })
        is DocumentNodeState.Error -> ErrorPage("We couldn’t load this $kind.", current.message, viewModel::load) { navigation.navigateTo(Screen.LevelOverview(levelId)) }
        is DocumentNodeState.Loaded -> {
            if (current.completed) {
                StateCompletionPage("$kind complete", "Your progress was saved and the next required learning step is now available.", "Return to Learning Map") { navigation.navigateTo(Screen.LevelOverview(levelId)) }
                return
            }
            Column(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    SecondaryButton("Back to Map", onClick = { navigation.pop() })
                    if (kind.contains("Mastery")) SecondaryButton("Save Draft", onClick = viewModel::saveDraft)
                }
                LazyColumn(
                    Modifier.weight(1f).widthIn(max = 900.dp).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Text(kind.uppercase(), style = AppTypography.caption, color = Theme.colors.brandPrimary); Text(current.title, style = DisplayStyle); Spacer(Modifier.height(12.dp)) }
                    itemsIndexed(current.sections) { index, section ->
                        Column(Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).padding(24.dp)) {
                            Text(section.title, style = AppTypography.h2); Spacer(Modifier.height(10.dp)); Text(section.body, style = AppTypography.body1)
                            if (current.responses.isNotEmpty() && index >= current.sections.size - current.responses.size) {
                                val responseIndex = index - (current.sections.size - current.responses.size)
                                Spacer(Modifier.height(14.dp)); OutlinedTextField(
                                    current.responses[responseIndex], { viewModel.updateResponse(responseIndex, it) }, Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                    label = { Text("Your reflection") }
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)); PrimaryButton(if (current.responses.isNotEmpty()) "Submit Reflection" else "Complete $kind", onClick = viewModel::complete); Spacer(Modifier.height(48.dp)) }
                }
            }
        }
    }
}
