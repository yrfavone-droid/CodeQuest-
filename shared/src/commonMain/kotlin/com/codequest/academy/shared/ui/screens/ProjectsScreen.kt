package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectsScreen(navigation: Navigation, repository: ProgressRepository) {
    var drafts by remember { mutableStateOf(emptyList<com.codequest.academy.shared.data.ProjectDraftRecord>()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { drafts = withContext(Dispatchers.Default) { repository.getProjectDrafts().filter { it.projectId.endsWith("-PROJECT") } }; loading = false }
    if (loading) { LoadingPage("Loading project portfolio…"); return }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(40.dp)) {
        Text("Projects", style = DisplayStyle); Text("Level Projects · Path Capstones · Track Finals", style = AppTypography.body1, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(26.dp))
        if (drafts.isEmpty()) EmptyPage("No projects unlocked yet", "Pass the first available Final Quiz with at least 75% to unlock a Level Project.", "Continue Learning") { navigation.navigateTo(Screen.Dashboard) }
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(drafts, key = { it.projectId }) { draft ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoCard(draft.projectId, if (draft.submitted) "Submitted" else "Draft saved locally", Modifier.weight(1f))
                    Spacer(Modifier.width(14.dp)); SecondaryButton(if (draft.submitted) "Review" else "Continue", onClick = {
                        val level = repository.getAllLevels().firstOrNull { draft.projectId.startsWith(it.code) }
                        if (level != null) navigation.navigateTo(Screen.Project(level.id, draft.projectId))
                    })
                }
            }
        }
    }
}
