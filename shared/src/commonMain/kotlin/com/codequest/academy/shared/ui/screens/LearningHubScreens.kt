package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.learning.LearningHubContent
import com.codequest.academy.shared.learning.LearningHubLesson
import com.codequest.academy.shared.learning.LearningHubProblem
import com.codequest.academy.shared.learning.LearningHubSection
import com.codequest.academy.shared.learning.LearningHubProgress
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.components.MarkdownContent
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.double

private val Orange = Color(0xFFEE6A36)

@Composable
fun LearningHubHomeScreen(navigation: Navigation) {
    val state by LearningHubContent.state.collectAsState()
    val curriculum = state.curriculum
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(42.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("LEARNING HUB", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.3.sp), color = Orange)
        Text("A deliberate path into AI", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text("Twenty ordered sections from foundations to responsible AI. Every lesson and practice task is bundled locally and remains available offline.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        when {
            state.loading -> Text("Validating the bundled curriculum…", style = AppTypography.body1, color = Theme.colors.textSecondary)
            state.error != null -> Text("Learning Hub unavailable: ${state.error}", style = AppTypography.body1, color = Theme.colors.textSecondary)
            curriculum != null -> {
                Text("CURRICULUM ${curriculum.version} · ${curriculum.lessonCount} lessons · ${curriculum.problemCount} practice tasks", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Orange)
                curriculum.sections.forEach { section -> LearningHubSectionCard(section, navigation) }
            }
        }
    }
}

@Composable
private fun LearningHubSectionCard(section: LearningHubSection, navigation: Navigation) {
    Column(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("${section.position.toString().padStart(2, '0')}  ${section.title}", style = AppTypography.h2, color = Theme.colors.textPrimary)
                Text(section.level, style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Orange)
            }
            Text("${section.lessons.size} lessons", style = AppTypography.caption, color = Theme.colors.textMuted)
        }
        Text(section.description, style = AppTypography.body2, color = Theme.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Open section", { navigation.navigateToLearningHubSection(section.id) })
            SecondaryButton("Open practice sheet", { LearningHubContent.openSectionPdf(section) })
        }
    }
}

@Composable
fun LearningHubSectionScreen(navigation: Navigation) {
    val state by LearningHubContent.state.collectAsState()
    val section = state.curriculum?.sections?.firstOrNull { it.id == navigation.selectedLearningHubSectionId }
    if (section == null) {
        Column(Modifier.fillMaxSize().padding(42.dp)) { Text("This section is not available.", color = Theme.colors.textPrimary) }
        return
    }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(42.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("LEARNING HUB / ${section.id}", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
        Text(section.title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text(section.description, style = AppTypography.body1, color = Theme.colors.textSecondary)
        section.lessons.forEach { lesson ->
            Row(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(12.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Lesson ${lesson.position}: ${lesson.title}", style = AppTypography.h2, color = Theme.colors.textPrimary)
                    Text(lesson.objective, style = AppTypography.body2, color = Theme.colors.textSecondary)
                }
                SecondaryButton("Open lesson", { navigation.navigateToLearningHubLesson(lesson.id) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Open section PDF", { LearningHubContent.openSectionPdf(section) })
            SecondaryButton("Download PDF", { LearningHubContent.saveSectionPdf(section, "${section.id}-practice-sheet.pdf") })
            SecondaryButton("Back", { navigation.pop() })
        }
    }
}

@Composable
fun LearningHubLessonScreen(navigation: Navigation) {
    val state by LearningHubContent.state.collectAsState()
    val lesson = state.curriculum?.sections?.flatMap { it.lessons }?.firstOrNull { it.id == navigation.selectedLearningHubLessonId }
    if (lesson == null) {
        Column(Modifier.fillMaxSize().padding(42.dp)) { Text("This lesson is not available.", color = Theme.colors.textPrimary) }
        return
    }
    val problems = remember(lesson.id, state.packagePath) { LearningHubContent.firstProblems(lesson) }
    val answers = remember(lesson.id) { mutableStateMapOf<String, String>() }
    val results = remember(lesson.id) { mutableStateMapOf<String, String>() }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(42.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("LESSON ${lesson.id}", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
        Text(lesson.title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text("Objective", style = AppTypography.h2, color = Theme.colors.textPrimary)
        Text(lesson.objective, style = AppTypography.body1, color = Theme.colors.textSecondary)
        Text("Worked example", style = AppTypography.h2, color = Theme.colors.textPrimary)
        Text(lesson.workedExample, style = AppTypography.body1, color = Theme.colors.textSecondary)
        Text("Lesson source", style = AppTypography.h2, color = Theme.colors.textPrimary)
        MarkdownContent(LearningHubContent.lessonMarkdown(lesson), modifier = Modifier.fillMaxWidth())
        Text("First ten practice tasks", style = AppTypography.h2, color = Theme.colors.textPrimary)
        problems.forEach { problem -> ProblemCard(problem, answers, results) }
        SecondaryButton("Back to section", { navigation.pop() })
    }
}

@Composable
private fun ProblemCard(problem: LearningHubProblem, answers: MutableMap<String, String>, results: MutableMap<String, String>) {
    var value by remember(problem.id) { mutableStateOf(answers[problem.id].orEmpty()) }
    Column(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(12.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(problem.prompt, style = AppTypography.body1, color = Theme.colors.textPrimary)
        if (problem.options.isNotEmpty()) Text(problem.options.mapIndexed { i, option -> "${i + 1}. $option" }.joinToString("\n"), style = AppTypography.body2, color = Theme.colors.textSecondary)
        OutlinedTextField(value, { value = it; answers[problem.id] = it }, label = { Text(if (problem.answerType == "rubric") "Your response" else "Answer") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Submit", {
                val result = scoreProblem(problem, value)
                results[problem.id] = result
                if (result == "Correct") LearningHubProgress.recordAttempt(problem.lessonId, 1.0)
                else if (result == "Try again") LearningHubProgress.recordAttempt(problem.lessonId, 0.0)
            })
            if (problem.answerType == "rubric" && results[problem.id] != null) {
                SecondaryButton("Retry", { results.remove(problem.id) })
                SecondaryButton("Needs review", { results[problem.id] = "Marked for review" })
                SecondaryButton("Mastered", { results[problem.id] = "Mastered"; LearningHubProgress.recordAttempt(problem.lessonId, 1.0, completed = true) })
            }
        }
        results[problem.id]?.let { result ->
            Text(result, style = AppTypography.body2, color = if (result == "Correct" || result == "Mastered") Orange else Theme.colors.textSecondary)
            if (problem.answerType == "rubric") Text("Rubric: ${problem.answer.jsonPrimitive.content}", style = AppTypography.caption, color = Theme.colors.textMuted)
            else Text(problem.explanation, style = AppTypography.caption, color = Theme.colors.textMuted)
        }
    }
}

private fun scoreProblem(problem: LearningHubProblem, response: String): String {
    if (response.isBlank()) return "Enter an answer to submit."
    return when (problem.answerType) {
        "multiple_choice" -> {
            val selected = response.toIntOrNull()
            val expected = problem.answer.jsonPrimitive.int
            if (selected == expected || selected?.minus(1) == expected) "Correct" else "Try again"
        }
        "numeric" -> if (response.toDoubleOrNull() != null && kotlin.math.abs(response.toDouble() - problem.answer.jsonPrimitive.double) < 1e-6) "Correct" else "Try again"
        "rubric", "short_text" -> "Submitted — review the guidance below."
        else -> "Submitted"
    }
}
