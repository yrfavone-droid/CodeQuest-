package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.learning.LearningHubContent
import com.codequest.academy.shared.learning.LearningHubProgress
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.CodeStyle
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme

@Composable
private fun WorkspacePage(eyebrow: String, title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(eyebrow, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textAccent)
        Text(title, style = DisplayStyle, color = Theme.colors.textPrimary)
        Text(description, style = AppTypography.body1, color = Theme.colors.textSecondary, modifier = Modifier.widthIn(max = 820.dp))
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PracticeWorkspaceScreen(navigation: Navigation) {
    val content by LearningHubContent.state.collectAsState()
    val progress by LearningHubProgress.state.collectAsState()
    val lesson = content.curriculum?.sections?.flatMap { it.lessons }?.let { lessons ->
        lessons.firstOrNull { (progress[it.id]?.attempts ?: 0) > 0 && progress[it.id]?.completed != true }
            ?: lessons.firstOrNull()
    }
    val answers = remember { mutableStateMapOf<String, String>() }
    val results = remember { mutableStateMapOf<String, String>() }
    WorkspacePage("ADAPTIVE PRACTICE", "Focused practice", "Work from the verified local problem bank. Attempts and mastery evidence are saved only to this profile.") {
        if (lesson == null) {
            WorkspaceNotice("Practice is unavailable", content.error ?: "The local curriculum is still being validated.")
        } else {
            val problem = remember(lesson.id, content.packagePath) { LearningHubContent.firstProblems(lesson, 1).firstOrNull() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(lesson.id, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.brandPrimary)
                    Text(lesson.title, style = AppTypography.h2, color = Theme.colors.textPrimary)
                }
                SecondaryButton("Open lesson", { navigation.navigateToLearningHubLesson(lesson.id) })
            }
            if (problem == null) WorkspaceNotice("No practice item found", "The lesson package did not expose a valid practice item.")
            else LearningProblemCard(problem, answers, results)
        }
    }
}

@Composable
fun PythonLabWorkspaceScreen(repository: ProgressRepository) {
    var source by remember { mutableStateOf(repository.getSetting("python_lab_draft", "# Write a small Python experiment here\n")) }
    WorkspacePage("LOCAL LAB", "Python Lab", "A focused editor for drafting and studying Python. Your draft is autosaved locally.") {
        Row(Modifier.fillMaxWidth().height(470.dp).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(16.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(16.dp))) {
            Column(Modifier.width(210.dp).fillMaxHeight().background(Theme.colors.surfaceSecondary).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("WORKSPACE", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textMuted)
                Text("main.py", style = AppTypography.body2.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary)
                Spacer(Modifier.weight(1f))
                Text("Saved to this profile", style = AppTypography.caption, color = Theme.colors.success)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PYTHON 3 · LOCAL DRAFT", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textMuted)
                    PrimaryButton("Run securely", {}, enabled = false)
                }
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it; repository.setSetting("python_lab_draft", it) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = CodeStyle.copy(color = Theme.colors.textPrimary),
                    label = { Text("main.py") }
                )
                WorkspaceNotice("Execution intentionally disabled", "No approved isolated runner is installed. Nous never executes learner code through the desktop application process.")
            }
        }
    }
}

@Composable
fun MathLabWorkspaceScreen() {
    val content by LearningHubContent.state.collectAsState()
    val problem = remember(content.packagePath) {
        content.curriculum?.sections?.asSequence()?.flatMap { it.lessons.asSequence() }
            ?.mapNotNull { lesson -> LearningHubContent.firstProblems(lesson, 10).firstOrNull { it.answerType == "numeric" } }
            ?.firstOrNull()
    }
    val answers = remember { mutableStateMapOf<String, String>() }
    val results = remember { mutableStateMapOf<String, String>() }
    WorkspacePage("MATHEMATICAL REASONING", "Math Lab", "Solve a verified numeric task step by step. Feedback comes from the installed answer data, never a fabricated result.") {
        if (problem == null) WorkspaceNotice("No numeric task available", "The current local package does not expose a compatible numeric exercise.")
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1.1f)) { LearningProblemCard(problem, answers, results) }
                Column(Modifier.weight(.9f).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(16.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reasoning canvas", style = AppTypography.h3, color = Theme.colors.textPrimary)
                    Text("Use paper or the answer field to work through the task. Graphs appear only when the installed exercise provides validated visualization data.", style = AppTypography.body2, color = Theme.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun ProgressWorkspaceScreen(repository: ProgressRepository, navigation: Navigation) {
    val content by LearningHubContent.state.collectAsState()
    val progress by LearningHubProgress.state.collectAsState()
    val lessonCount = content.curriculum?.lessonCount ?: 0
    val started = progress.values.count { it.attempts > 0 || it.articleUnitsRead > 0 || it.quizAttempts > 0 }
    val mastered = progress.values.count { it.completed }
    val bookmarks = progress.values.count { it.bookmarked }
    WorkspacePage("LOCAL EVIDENCE", "Progress", "A private record built from your real reading, practice, and quiz activity on this device.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricPanel("LESSONS STARTED", started.toString(), "of $lessonCount", Modifier.weight(1f))
            MetricPanel("MASTERED", mastered.toString(), "verified lessons", Modifier.weight(1f))
            MetricPanel("LESSON BOOKMARKS", bookmarks.toString(), "saved locally", Modifier.weight(1f))
            MetricPanel("PDFS OPENED", repository.readingStates().size.toString(), "reading records", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Reading progress", { navigation.navigateTo(Screen.ReadingProgress) })
            SecondaryButton("Bookmarks", { navigation.navigateTo(Screen.Bookmarks) })
        }
        if (started == 0) WorkspaceNotice("No learning evidence yet", "Open a lesson, read an article unit, or submit practice to begin your progress record.")
    }
}

@Composable
fun UpdatesWorkspaceScreen() {
    val curriculum by LearningHubContent.state.collectAsState()
    WorkspacePage("VERIFIED RELEASES", "Updates", "Application and curriculum updates remain explicit, verifiable, and recoverable.") {
        UpdateSettingsPanel()
        Column(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(16.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Curriculum package", style = AppTypography.h3, color = Theme.colors.textPrimary)
            Text("Installed: ${curriculum.curriculumVersion ?: curriculum.curriculum?.version ?: "validating"}", style = AppTypography.body2, color = Theme.colors.textSecondary)
            Text("A local package is staged, checked, and atomically activated. Learner data is stored separately.", style = AppTypography.body2, color = Theme.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Update curriculum", { LearningHubContent.selectAndInstallCurriculum() })
                if (curriculum.canRollback) SecondaryButton("Rollback", { LearningHubContent.rollbackCurriculum() })
            }
            curriculum.updateMessage?.let { Text(it, style = AppTypography.caption, color = Theme.colors.textAccent) }
        }
    }
}

@Composable
private fun MetricPanel(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier.border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(16.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textMuted)
        Text(value, style = AppTypography.h1, color = Theme.colors.brandPrimary)
        Text(detail, style = AppTypography.caption, color = Theme.colors.textSecondary)
    }
}

@Composable
private fun WorkspaceNotice(title: String, body: String) {
    Column(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).background(Theme.colors.elevatedBackground, RoundedCornerShape(14.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = AppTypography.h3, color = Theme.colors.textPrimary)
        Text(body, style = AppTypography.body2, color = Theme.colors.textSecondary)
    }
}
