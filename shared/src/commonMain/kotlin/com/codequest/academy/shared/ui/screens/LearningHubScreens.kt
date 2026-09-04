package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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

private enum class LessonPage(val label: String, val marker: String) {
    Article("Article", "01"),
    Review("Detailed Summary & Review", "02"),
    Quiz("Quiz", "03")
}

@Composable
fun LearningHubHomeScreen(navigation: Navigation) {
    val state by LearningHubContent.state.collectAsState()
    val progress by LearningHubProgress.state.collectAsState()
    val curriculum = state.curriculum
    var query by remember { mutableStateOf("") }
    val results = remember(query, state.packagePath) { LearningHubContent.searchLessons(query) }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(42.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("LEARNING HUB", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.3.sp), color = Orange)
        Text("A deliberate path into AI", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text("Twenty ordered sections from foundations to responsible AI. Every lesson and practice task is bundled locally and remains available offline.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        when {
            state.loading -> Text("Validating the bundled curriculum…", style = AppTypography.body1, color = Theme.colors.textSecondary)
            state.error != null -> Text("Learning Hub unavailable: ${state.error}", style = AppTypography.body1, color = Theme.colors.textSecondary)
            curriculum != null -> {
                Text("CURRICULUM ${state.curriculumVersion ?: curriculum.version} · ${curriculum.lessonCount} lessons · ${curriculum.problemCount} verified problems", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Orange)
                OutlinedTextField(query, { query = it }, label = { Text("Search sections, lessons, articles, summaries, and terms") }, modifier = Modifier.fillMaxWidth())
                if (query.length >= 2) {
                    ArticleSectionCard("Offline search results") {
                        if (results.isEmpty()) Text("No matching lesson found.", style = AppTypography.body2, color = Theme.colors.textSecondary)
                        results.forEach { result ->
                            Column(Modifier.fillMaxWidth().clickable { navigation.navigateToLearningHubLesson(result.lessonId) }.padding(vertical = 8.dp)) {
                                Text("${result.sectionId} · ${result.lessonTitle}", style = AppTypography.h3, color = Orange)
                                Text(result.excerpt, style = AppTypography.caption, color = Theme.colors.textSecondary)
                            }
                        }
                    }
                }
                ArticleSectionCard("Curriculum updates") {
                    Text("Installed: ${state.curriculumVersion ?: "verified bundled curriculum"}", style = AppTypography.body2, color = Theme.colors.textSecondary)
                    Text("Updates use a local ZIP only. The package is staged, fully validated, and atomically activated; learner data is never replaced.", style = AppTypography.caption, color = Theme.colors.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Update Curriculum", { LearningHubContent.selectAndInstallCurriculum() })
                        if (state.canRollback) SecondaryButton("Rollback", { LearningHubContent.rollbackCurriculum() })
                    }
                    state.updateMessage?.let { Text(it, style = AppTypography.caption, color = Orange) }
                }
                curriculum.sections.forEach { section -> LearningHubSectionCard(section, navigation, progress) }
            }
        }
    }
}

@Composable
private fun LearningHubSectionCard(section: LearningHubSection, navigation: Navigation, progress: Map<String, com.codequest.academy.shared.learning.LearningHubLessonProgress>) {
    val completed = section.lessons.count { progress[it.id]?.completed == true }
    Column(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(14.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("${section.position.toString().padStart(2, '0')}  ${section.title}", style = AppTypography.h2, color = Theme.colors.textPrimary)
                Text(section.level, style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Orange)
            }
            Text("${section.lessons.size} lessons", style = AppTypography.caption, color = Theme.colors.textMuted)
        }
        Text(section.description, style = AppTypography.body2, color = Theme.colors.textSecondary)
        LinearProgressIndicator(progress = if (section.lessons.isEmpty()) 0f else completed.toFloat() / section.lessons.size, modifier = Modifier.fillMaxWidth(), color = Orange, backgroundColor = Theme.colors.borderDefault)
        Text("$completed of ${section.lessons.size} lessons mastered", style = AppTypography.caption, color = Theme.colors.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Open section", { navigation.navigateToLearningHubSection(section.id) })
            SecondaryButton("Open section PDF", { LearningHubContent.openSectionPdf(section) })
            SecondaryButton("Save PDF", { LearningHubContent.chooseAndSaveSectionPdf(section) })
        }
    }
}

@Composable
fun LearningHubSectionScreen(navigation: Navigation) {
    val state by LearningHubContent.state.collectAsState()
    val progress by LearningHubProgress.state.collectAsState()
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
            val lessonProgress = progress[lesson.id]
            Row(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary, RoundedCornerShape(12.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Lesson ${lesson.position}: ${lesson.title}", style = AppTypography.h2, color = Theme.colors.textPrimary)
                    Text(lesson.objective, style = AppTypography.body2, color = Theme.colors.textSecondary)
                    Text("${lesson.unitCount.coerceAtLeast(8)} units · ${lesson.practiceCount.coerceAtLeast(80)} practice · ${lesson.quizCount.coerceAtLeast(20)} quiz · best quiz ${lessonProgress?.bestQuizCorrect ?: 0}/20", style = AppTypography.caption, color = Theme.colors.textMuted)
                }
                SecondaryButton("Open lesson", { navigation.navigateToLearningHubLesson(lesson.id) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Open section PDF", { LearningHubContent.openSectionPdf(section) })
            SecondaryButton("Download / Save PDF", { LearningHubContent.chooseAndSaveSectionPdf(section) })
            SecondaryButton("Back", { navigation.pop() })
        }
    }
}

@Composable
fun LearningHubLessonScreen(navigation: Navigation) {
    val state by LearningHubContent.state.collectAsState()
    val progressState by LearningHubProgress.state.collectAsState()
    val lesson = state.curriculum?.sections?.flatMap { it.lessons }?.firstOrNull { it.id == navigation.selectedLearningHubLessonId }
    if (lesson == null) {
        Column(Modifier.fillMaxSize().padding(42.dp)) { Text("This lesson is not available.", color = Theme.colors.textPrimary) }
        return
    }
    if (lesson.id.startsWith("S01-") && LearningHubContent.section1Lesson(lesson.id) != null) {
        LearningHubSection1LessonScreen(navigation, lesson)
        return
    }
    var page by remember(lesson.id) { mutableStateOf(LessonPage.Article) }
    val quiz = remember(lesson.id, state.packagePath) { LearningHubContent.lessonQuiz(lesson) }
    val blocks = remember(lesson.id, state.packagePath) { LearningHubContent.lessonArticleBlocks(lesson) }
    val practice = remember(lesson.id, state.packagePath) { LearningHubContent.allPractice(lesson) }
    val progress = progressState[lesson.id]
    var note by remember(lesson.id, progress?.note) { mutableStateOf(progress?.note.orEmpty()) }
    var showAllPractice by remember(lesson.id) { mutableStateOf(false) }
    val revealed = remember(lesson.id) { mutableStateMapOf<String, Boolean>() }
    val answers = remember(lesson.id) { mutableStateMapOf<String, String>() }
    val practiceResults = remember(lesson.id) { mutableStateMapOf<String, String>() }
    val locked = remember(lesson.id) { mutableStateMapOf<String, Boolean>() }
    var quizIndex by remember(lesson.id) { mutableStateOf(0) }
    var quizRecorded by remember(lesson.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        Column(Modifier.fillMaxWidth(0.24f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LESSON PAGES", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
            Text(lesson.title, style = AppTypography.h2, color = Theme.colors.textPrimary)
            Text("${lesson.articleWords ?: 0} words · ${lesson.unitCount} units", style = AppTypography.caption, color = Theme.colors.textMuted)
            LessonPage.values().forEach { item -> if (item == page) PrimaryButton("${item.marker}  ${item.label}", { page = item }) else SecondaryButton("${item.marker}  ${item.label}", { page = item }) }
            if (page == LessonPage.Article) {
                Text("ARTICLE CONTENTS", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Theme.colors.textMuted, modifier = Modifier.padding(top = 10.dp))
                blocks.filter { it.type == "heading" }.forEach { block -> Text(block.text.orEmpty(), style = AppTypography.caption, color = Theme.colors.textSecondary, modifier = Modifier.padding(vertical = 3.dp)) }
            }
            Text("Private notes", style = AppTypography.h3, color = Theme.colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Lesson notes") }, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Save notes", { LearningHubProgress.saveNote(lesson.id, note) })
            SecondaryButton(if (progress?.bookmarked == true) "Remove bookmark" else "Bookmark lesson", { LearningHubProgress.toggleBookmark(lesson.id) })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 42.dp, top = 34.dp, bottom = 42.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("LESSON ${lesson.id} / ${page.label.uppercase()}", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
            Text(lesson.title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
            when (page) {
                LessonPage.Article -> {
                    ArticleCallout("Learning objective", lesson.objective, Orange)
                    ArticleCallout("Core principle", lesson.principle, Color(0xFF9A4B27))
                    LinearProgressIndicator(progress = (progress?.articleUnitsRead ?: 0) / 8f, modifier = Modifier.fillMaxWidth(), color = Orange, backgroundColor = Theme.colors.borderDefault)
                    Text("Reading progress: ${progress?.articleUnitsRead ?: 0}/8 units", style = AppTypography.caption, color = Theme.colors.textMuted)
                    blocks.forEachIndexed { index, block ->
                        when (block.type) {
                            "hero", "summary_ref", "quiz_ref" -> Unit
                            "heading" -> Text(block.text.orEmpty(), style = AppTypography.h1, color = Theme.colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                            "article" -> MarkdownContent(block.text.orEmpty(), modifier = Modifier.fillMaxWidth())
                            "worked_example" -> ArticleCallout("Worked example", block.text.orEmpty(), Orange)
                            "misconception" -> ArticleCallout("Misconception warning", block.text.orEmpty(), Color(0xFFB45309))
                            "knowledge_check" -> {
                                val key = "${lesson.id}-$index"
                                ArticleSectionCard("Knowledge check") {
                                    Text(block.question.orEmpty(), style = AppTypography.body1, color = Theme.colors.textPrimary)
                                    SecondaryButton(if (revealed[key] == true) "Hide answer" else "Reveal answer", { revealed[key] = revealed[key] != true })
                                    if (revealed[key] == true) Text(block.answer.orEmpty(), style = AppTypography.body2, color = Theme.colors.textSecondary)
                                }
                            }
                            "project" -> ArticleCallout("Applied project", block.text.orEmpty(), Orange)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Mark all 8 units read", { LearningHubProgress.markArticleProgress(lesson.id, 8) })
                        SecondaryButton("Open lesson PDF", { LearningHubContent.openLessonPdf(lesson) })
                        SecondaryButton("Download / Save PDF", { LearningHubContent.chooseAndSaveLessonPdf(lesson) })
                    }
                    Text("Practice", style = AppTypography.h1, color = Theme.colors.textPrimary)
                    Text("The complete verified 80-question practice bank is available locally.", style = AppTypography.body2, color = Theme.colors.textSecondary)
                    (if (showAllPractice) practice else practice.take(10)).forEach { problem -> ProblemCard(problem, answers, practiceResults) }
                    SecondaryButton(if (showAllPractice) "Show first 10" else "Open all 80 practice tasks", { showAllPractice = !showAllPractice })
                }
                LessonPage.Review -> {
                    MarkdownContent(LearningHubContent.lessonReview(lesson), modifier = Modifier.fillMaxWidth())
                    ArticleSectionCard("Mark review lenses for later revision") {
                        (1..8).forEach { item ->
                            val marked = item in (progress?.reviewItems ?: emptySet())
                            SecondaryButton(if (marked) "✓ Lens $item marked" else "Mark lens $item", { LearningHubProgress.toggleReviewItem(lesson.id, item) })
                        }
                    }
                }
                LessonPage.Quiz -> {
                    Text("Multiple-choice knowledge check", style = AppTypography.h2, color = Theme.colors.textPrimary)
                    Text("Exactly ${quiz.size} supplied questions · mastery requires 16/20", style = AppTypography.body2, color = Theme.colors.textSecondary)
                    if (quiz.isNotEmpty()) {
                        val problem = quiz[quizIndex]
                        QuizQuestion(problem, quizIndex, quiz.size, answers, locked)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Previous", { if (quizIndex > 0) quizIndex-- })
                            SecondaryButton("Next", { if (quizIndex < quiz.lastIndex) quizIndex++ })
                        }
                        val submitted = locked.values.count { it }
                        Text("$submitted of ${quiz.size} answers submitted", style = AppTypography.caption, color = Theme.colors.textMuted)
                        if (submitted == quiz.size) {
                            val correct = quiz.count { answers[it.id]?.toIntOrNull() == it.answer.jsonPrimitive.int }
                            val incorrect = quiz.filterNot { answers[it.id]?.toIntOrNull() == it.answer.jsonPrimitive.int }.map { it.id }
                            ArticleCallout(if (correct >= 16) "Mastery achieved" else "Review required", "$correct/20 correct. ${if (incorrect.isEmpty()) "No incorrect answers." else "Review: ${incorrect.joinToString()}"}", if (correct >= 16) Orange else Color(0xFFB45309))
                            if (!quizRecorded) PrimaryButton("Save quiz result", { LearningHubProgress.recordQuizAttempt(lesson.id, correct, quiz.size, incorrect); quizRecorded = true })
                            SecondaryButton("Retry quiz", { answers.clear(); locked.clear(); quizIndex = 0; quizRecorded = false })
                        }
                    }
                }
            }
            SecondaryButton("Back to section", { navigation.pop() })
        }
    }
}

@Composable
private fun QuizQuestion(
    problem: LearningHubProblem,
    index: Int,
    total: Int,
    answers: MutableMap<String, String>,
    locked: MutableMap<String, Boolean>
) {
    val selected = answers[problem.id]
    val submitted = locked[problem.id] == true
    ArticleSectionCard("Question ${index + 1} of $total") {
        Text(problem.prompt, style = AppTypography.body1, color = Theme.colors.textPrimary)
        problem.options.forEachIndexed { optionIndex, option ->
            val chosen = selected == optionIndex.toString()
            val correct = submitted && optionIndex == problem.answer.jsonPrimitive.int
            val border = when { correct -> Color(0xFF17845C); chosen -> Orange; else -> Theme.colors.borderDefault }
            Row(
                Modifier.fillMaxWidth().border(1.dp, border, RoundedCornerShape(10.dp))
                    .background(if (chosen || correct) border.copy(alpha = 0.10f) else Theme.colors.surfaceSecondary, RoundedCornerShape(10.dp))
                    .clickable(enabled = !submitted) { answers[problem.id] = optionIndex.toString() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text("${optionIndex + 1}.", style = AppTypography.body2, color = border, modifier = Modifier.padding(end = 10.dp))
                Text(option, style = AppTypography.body2, color = Theme.colors.textPrimary, modifier = Modifier.weight(1f))
            }
        }
        if (!submitted) PrimaryButton("Submit answer", { if (selected != null) locked[problem.id] = true })
        else {
            val isCorrect = selected?.toIntOrNull() == problem.answer.jsonPrimitive.int
            Text(if (isCorrect) "Correct" else "Incorrect — the verified answer is highlighted.", style = AppTypography.h3, color = if (isCorrect) Color(0xFF17845C) else Color(0xFFB45309))
            Text(problem.explanation, style = AppTypography.body2, color = Theme.colors.textSecondary)
        }
    }
}

/** Article-first Section 1 experience backed exclusively by the verified section package. */
@Composable
private fun LearningHubSection1LessonScreen(navigation: Navigation, lesson: LearningHubLesson) {
    val meta = LearningHubContent.section1Lesson(lesson.id) ?: return
    var page by remember(lesson.id) { mutableStateOf(LessonPage.Article) }
    val blocks = remember(lesson.id) { LearningHubContent.section1ArticleBlocks(lesson.id) }
    val practice = remember(lesson.id) { LearningHubContent.section1Problems(lesson.id, quiz = false, limit = 10) }
    val quiz = remember(lesson.id) { LearningHubContent.section1Problems(lesson.id, quiz = true, limit = 20).filter { it.answerType == "multiple_choice" } }
    val answers = remember(lesson.id) { mutableStateMapOf<String, String>() }
    val results = remember(lesson.id) { mutableStateMapOf<String, String>() }
    var note by remember(lesson.id) { mutableStateOf(LearningHubContent.section1Note(lesson.id)) }
    val revealed = remember(lesson.id) { mutableStateMapOf<String, Boolean>() }
    val scroll = rememberScrollState()
    val sectionLessons = remember { LearningHubContent.state.value.curriculum?.sections?.firstOrNull { it.id == "S01" }?.lessons.orEmpty() }
    val index = sectionLessons.indexOfFirst { it.id == lesson.id }
    Row(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        Column(Modifier.fillMaxWidth(0.24f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LESSON PAGES", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
            Text(meta.title, style = AppTypography.h2, color = Theme.colors.textPrimary)
            Text("${meta.articleWords} words · ${meta.unitCount} units", style = AppTypography.caption, color = Theme.colors.textMuted)
            LessonPage.values().forEach { item ->
                if (item == page) PrimaryButton("${item.marker}  ${item.label}", { page = item })
                else SecondaryButton("${item.marker}  ${item.label}", { page = item })
            }
            if (page == LessonPage.Article) {
                Text("ARTICLE CONTENTS", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Theme.colors.textMuted, modifier = Modifier.padding(top = 12.dp))
                blocks.filter { it.type == "heading" }.forEachIndexed { i, block ->
                    Text("${i + 1}. ${block.title ?: block.text.orEmpty()}", style = AppTypography.caption, color = Theme.colors.textSecondary, modifier = Modifier.padding(vertical = 3.dp))
                }
                Text("Notes", style = AppTypography.h3, color = Theme.colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Private notes") }, modifier = Modifier.fillMaxWidth())
                SecondaryButton("Save notes", { LearningHubContent.saveSection1Note(lesson.id, note) })
            }
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(start = 12.dp, end = 42.dp, top = 32.dp, bottom = 42.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SECTION 01 / ${lesson.id} / ${page.label.uppercase()}", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
            Text(meta.title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
            Text(meta.subtitle, style = AppTypography.h2, color = Theme.colors.textSecondary)
            when (page) {
                LessonPage.Article -> {
                    SurfaceCard { Text(meta.bigQuestion, style = AppTypography.h2, color = Orange) }
                    blocks.forEachIndexed { idx, block ->
                        when (block.type) {
                            "hero", "quiz_ref", "glossary", "project" -> Unit
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
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Open lesson PDF", { LearningHubContent.openSection1Pdf(lesson.id) })
                        SecondaryButton("Download PDF", { LearningHubContent.saveSection1Pdf(lesson.id, "${lesson.id}-Nous-AI-Academy.pdf") })
                    }
                }
                LessonPage.Review -> {
                    MarkdownContent(LearningHubContent.section1Review(lesson.id), modifier = Modifier.fillMaxWidth())
                    blocks.filter { it.type == "project" }.forEach { ArticleCallout("Applied project", it.text.orEmpty(), Orange) }
                    ArticleSectionCard("Glossary") { LearningHubContent.section1Glossary().forEach { entry -> Text(entry.term, style = AppTypography.h3, color = Orange); Text(entry.definition, style = AppTypography.body2, color = Theme.colors.textSecondary) } }
                    Text("Practice review", style = AppTypography.h1, color = Theme.colors.textPrimary)
                    practice.forEach { ProblemCard(it, answers, results) }
                }
                LessonPage.Quiz -> {
                    Text("Multiple-choice lesson quiz", style = AppTypography.h2, color = Theme.colors.textPrimary)
                    Text("${quiz.size} questions use the existing verified Section 1 question bank.", style = AppTypography.body2, color = Theme.colors.textSecondary)
                    quiz.forEach { ProblemCard(it, answers, results) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SecondaryButton("Previous", { if (index > 0) navigation.navigateToLearningHubLesson(sectionLessons[index - 1].id) })
                SecondaryButton("Back to section", { navigation.pop() })
                SecondaryButton("Next", { if (index >= 0 && index < sectionLessons.lastIndex) navigation.navigateToLearningHubLesson(sectionLessons[index + 1].id) })
            }
        }
    }
}

@Composable
private fun LessonPageSidebar(title: String, selected: LessonPage, onSelect: (LessonPage) -> Unit) {
    Column(Modifier.fillMaxWidth(0.24f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("LESSON PAGES", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Orange)
        Text(title, style = AppTypography.h2, color = Theme.colors.textPrimary)
        LessonPage.values().forEach { page ->
            if (page == selected) PrimaryButton("${page.marker}  ${page.label}", { onSelect(page) })
            else SecondaryButton("${page.marker}  ${page.label}", { onSelect(page) })
        }
        Text("Your lesson progress is saved locally and remains available offline.", style = AppTypography.caption, color = Theme.colors.textMuted, modifier = Modifier.padding(top = 12.dp))
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
        if (problem.answerType == "multiple_choice") {
            problem.options.forEachIndexed { index, option ->
                val selected = value == index.toString()
                Row(
                    Modifier.fillMaxWidth()
                        .border(1.dp, if (selected) Orange else Theme.colors.borderDefault, RoundedCornerShape(10.dp))
                        .background(if (selected) Orange.copy(alpha = 0.10f) else Theme.colors.surfaceSecondary, RoundedCornerShape(10.dp))
                        .clickable {
                            value = index.toString()
                            answers[problem.id] = value
                            results.remove(problem.id)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text("${index + 1}.", style = AppTypography.body2, color = if (selected) Orange else Theme.colors.textMuted, modifier = Modifier.padding(end = 10.dp))
                    Text(option, style = AppTypography.body2, color = Theme.colors.textPrimary, modifier = Modifier.weight(1f))
                }
            }
        } else {
            OutlinedTextField(value, { value = it; answers[problem.id] = it }, label = { Text(if (problem.answerType == "rubric") "Your response" else "Answer") }, modifier = Modifier.fillMaxWidth())
        }
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
            if (selected == expected) "Correct" else "Try again"
        }
        "numeric" -> if (response.toDoubleOrNull() != null && kotlin.math.abs(response.toDouble() - problem.answer.jsonPrimitive.double) < 1e-6) "Correct" else "Try again"
        "rubric", "short_text" -> "Submitted — review the guidance below."
        else -> "Submitted"
    }
}
