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
import com.codequest.academy.shared.learning.Section1ArticleBlock
import com.codequest.academy.shared.learning.Section1LessonMeta
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.components.MarkdownContent
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
    if (lesson.id.startsWith("S01-") && LearningHubContent.section1Lesson(lesson.id) != null) {
        LearningHubSection1LessonScreen(navigation, lesson)
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

/** Article-first Section 1 experience backed exclusively by the verified section package. */
@Composable
private fun LearningHubSection1LessonScreen(navigation: Navigation, lesson: LearningHubLesson) {
    val meta = LearningHubContent.section1Lesson(lesson.id) ?: return
    val blocks = remember(lesson.id) { LearningHubContent.section1ArticleBlocks(lesson.id) }
    val practice = remember(lesson.id) { LearningHubContent.section1Problems(lesson.id, quiz = false, limit = 10) }
    val quiz = remember(lesson.id) { LearningHubContent.section1Problems(lesson.id, quiz = true, limit = 20) }
    val answers = remember(lesson.id) { mutableStateMapOf<String, String>() }
    val results = remember(lesson.id) { mutableStateMapOf<String, String>() }
    var note by remember(lesson.id) { mutableStateOf(LearningHubContent.section1Note(lesson.id)) }
    val revealed = remember(lesson.id) { mutableStateMapOf<String, Boolean>() }
    val scroll = rememberScrollState()
    val sectionLessons = remember { LearningHubContent.state.value.curriculum?.sections?.firstOrNull { it.id == "S01" }?.lessons.orEmpty() }
    val index = sectionLessons.indexOfFirst { it.id == lesson.id }
    Row(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        Column(Modifier.fillMaxWidth(0.24f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ON THIS LESSON", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
            Text(meta.title, style = AppTypography.h2, color = Theme.colors.textPrimary)
            Text("${meta.articleWords} words · ${meta.unitCount} units", style = AppTypography.caption, color = Theme.colors.textMuted)
            blocks.filter { it.type == "heading" }.forEachIndexed { i, block ->
                Text("${i + 1}. ${block.title.orEmpty()}", style = AppTypography.caption, color = Theme.colors.textSecondary, modifier = Modifier.padding(vertical = 3.dp))
            }
            Text("Notes", style = AppTypography.h3, color = Theme.colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Private notes") }, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Save notes", { LearningHubContent.saveSection1Note(lesson.id, note) })
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(start = 12.dp, end = 42.dp, top = 32.dp, bottom = 42.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SECTION 01 / ${lesson.id}", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
            Text(meta.title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
            Text(meta.subtitle, style = AppTypography.h2, color = Theme.colors.textSecondary)
            SurfaceCard { Text(meta.bigQuestion, style = AppTypography.h2, color = Orange) }
            blocks.forEachIndexed { idx, block ->
                when (block.type) {
                    "hero" -> Unit
                    "objectives" -> ArticleSectionCard("Objectives") { block.items.asStrings().forEach { Text("• $it", style = AppTypography.body1, color = Theme.colors.textSecondary) } }
                    "heading" -> Text(block.title ?: block.text.orEmpty(), style = AppTypography.h1, color = Theme.colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                    "article" -> MarkdownContent(block.text.orEmpty(), modifier = Modifier.fillMaxWidth())
                    "worked_example" -> ArticleCallout("Worked example", block.text.orEmpty(), Orange)
                    "misconception" -> ArticleCallout("Common misconception", block.text.orEmpty(), Color(0xFFB45309))
                    "knowledge_check" -> {
                        val key = "${lesson.id}-$idx"
                        ArticleSectionCard("Knowledge check") {
                            Text(block.question.orEmpty(), style = AppTypography.body1, color = Theme.colors.textPrimary)
                            SecondaryButton(if (revealed[key] == true) "Hide answer" else "Reveal answer", { revealed[key] = revealed[key] != true })
                            if (revealed[key] == true) Text(block.answer.orEmpty(), style = AppTypography.body2, color = Theme.colors.textSecondary)
                        }
                    }
                    "project" -> ArticleCallout("Applied project", block.text.orEmpty(), Orange)
                    "glossary" -> ArticleSectionCard("Glossary") { LearningHubContent.section1Glossary().forEach { entry -> Text(entry.term, style = AppTypography.h3, color = Orange); Text(entry.definition, style = AppTypography.body2, color = Theme.colors.textSecondary) } }
                }
            }
            Text("Practice", style = AppTypography.h1, color = Theme.colors.textPrimary)
            Text("First ten tasks are shown here; the complete bank remains available through Practice.", style = AppTypography.body2, color = Theme.colors.textSecondary)
            practice.forEach { ProblemCard(it, answers, results) }
            Text("Lesson quiz", style = AppTypography.h1, color = Theme.colors.textPrimary)
            quiz.forEach { ProblemCard(it, answers, results) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Open lesson PDF", { LearningHubContent.openSection1Pdf(lesson.id) })
                SecondaryButton("Download PDF", { LearningHubContent.saveSection1Pdf(lesson.id, "${lesson.id}-Nous-AI-Academy.pdf") })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SecondaryButton("Previous", { if (index > 0) navigation.navigateToLearningHubLesson(sectionLessons[index - 1].id) })
                SecondaryButton("Back to section", { navigation.pop() })
                SecondaryButton("Next", { if (index >= 0 && index < sectionLessons.lastIndex) navigation.navigateToLearningHubLesson(sectionLessons[index + 1].id) })
            }
        }
    }
}

private fun kotlinx.serialization.json.JsonElement?.asStrings(): List<String> = when (this) {
    is kotlinx.serialization.json.JsonArray -> mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
    is kotlinx.serialization.json.JsonObject -> entries.map { "${it.key}: ${runCatching { it.value.jsonPrimitive.content }.getOrDefault(it.value.toString())}" }
    else -> emptyList()
}

@Composable
private fun SurfaceCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, Orange, RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).padding(20.dp)) { content() }
}

@Composable
private fun ArticleSectionCard(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(12.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = AppTypography.h3, color = Orange)
        content()
    }
}

@Composable
private fun ArticleCallout(title: String, body: String, accent: Color) {
    Column(Modifier.fillMaxWidth().border(1.dp, accent, RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(12.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = AppTypography.h3, color = accent)
        MarkdownContent(body, modifier = Modifier.fillMaxWidth())
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
