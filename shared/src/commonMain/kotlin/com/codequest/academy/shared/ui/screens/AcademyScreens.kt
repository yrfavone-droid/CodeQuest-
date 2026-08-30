package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.data.AcademyLibraryItem
import com.codequest.academy.shared.data.AcademyDocumentHandler
import com.codequest.academy.shared.data.AcademyLessonRecord
import com.codequest.academy.shared.data.AcademyTrackRecord
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.components.StatusPill
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun AcademyHomeScreen(navigation: Navigation, repository: ProgressRepository) {
    val tracks = remember { repository.getAcademyTracks() }
    val lessons = remember { repository.getAcademyLessons() }
    Column(
        Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text("CODEQUEST AI ACADEMY", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Learn. Build. Improve.", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
                Spacer(Modifier.height(8.dp))
                Text("A private, offline learning workspace. Your progress stays on this device.", style = AppTypography.body1, color = Theme.colors.textSecondary)
            }
            StatusPill("OFFLINE READY", Theme.colors.success, Theme.colors.successSoft)
        }
        HomeCallout(lessons.firstOrNull(), navigation)
        Text("Your Academy path", style = AppTypography.h2, color = Theme.colors.textPrimary)
        TrackGrid(tracks.take(6), onOpen = { navigation.navigateTo(Screen.AcademyLearn) })
        Text("Installed learning material", style = AppTypography.h2, color = Theme.colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile("10,000", "planned problem slots", Theme.colors.brandPrimary)
            SummaryTile("3", "published foundation lessons", Theme.colors.success)
            SummaryTile("5 + 20", "books and deep dives", Theme.colors.information)
        }
    }
}

@Composable
private fun HomeCallout(lesson: AcademyLessonRecord?, navigation: Navigation) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.colors.surfaceSecondary).border(1.dp, Theme.colors.borderStrong, RoundedCornerShape(16.dp)).padding(28.dp)) {
        Text("CONTINUE LEARNING", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.brandPrimary)
        Spacer(Modifier.height(10.dp))
        Text(lesson?.title ?: "Academy content is being prepared", style = AppTypography.h1, color = Theme.colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(if (lesson == null) "The local content pack was not found." else "${lesson.estimatedMinutes} minutes · Instruction, guided practice, independent practice, and a mastery check.", style = AppTypography.body2, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Open lesson", enabled = lesson != null, onClick = { lesson?.let { navigation.navigateTo(Screen.AcademyLesson(it.id)) } })
    }
}

@Composable
fun AcademyLearnScreen(navigation: Navigation, repository: ProgressRepository) {
    val tracks = remember { repository.getAcademyTracks() }
    val lessons = remember { repository.getAcademyLessons() }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("LEARN", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text("A serious AI foundation, one verified step at a time", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text("The bundled manifest defines 10,000 production slots. Only completed, validated material is shown as published.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        TrackGrid(tracks, onOpen = { })
        Text("Published foundation lessons", style = AppTypography.h2, color = Theme.colors.textPrimary)
        lessons.forEach { lesson ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).clickable { navigation.navigateTo(Screen.AcademyLesson(lesson.id)) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Theme.colors.brandPrimary), contentAlignment = Alignment.Center) { Text("AI", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Color.White) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) { Text(lesson.title, style = AppTypography.h3, color = Theme.colors.textPrimary); Text("${lesson.estimatedMinutes} minutes · local lesson", style = AppTypography.caption, color = Theme.colors.textSecondary) }
                Text("Open", style = AppTypography.button, color = Theme.colors.brandPrimary)
            }
        }
    }
}

@Composable
fun AcademyLessonScreen(navigation: Navigation, repository: ProgressRepository, lessonId: String) {
    val lesson = remember(lessonId) { repository.getAcademyLessons().firstOrNull { it.id == lessonId } }
    val fields = remember(lesson?.contentJson) { lesson?.contentJson?.let(::lessonFields) ?: emptyMap() }
    if (lesson == null) {
        ErrorPage("Lesson unavailable", "This local lesson is not installed.", { navigation.pop() }, { navigation.navigateTo(Screen.AcademyLearn) })
        return
    }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp).widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SecondaryButton("Back to Learn", onClick = { navigation.pop() })
        Text("FOUNDATION LESSON", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text(lesson.title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text("${lesson.estimatedMinutes} minutes · Saved locally", style = AppTypography.body2, color = Theme.colors.textSecondary)
        LessonSection("Objectives", fields["objectives"])
        LessonSection("Concept", fields["explanation"])
        LessonSection("Worked example", fields["workedExample"])
        LessonSection("Guided practice", fields["guidedPractice"])
        LessonSection("Independent practice", fields["independentPractice"])
        LessonSection("Progressive hints", fields["hints"])
        LessonSection("Common mistakes", fields["commonMistakes"])
        LessonSection("Mastery check", fields["masteryCheck"])
        LessonSection("Accessibility", fields["accessibilityText"])
        Text("Sources: ${fields["sources"]}", style = AppTypography.caption, color = Theme.colors.textMuted)
    }
}

@Composable
fun AcademyPracticeScreen(navigation: Navigation, repository: ProgressRepository) {
    var selected by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val options = listOf("A source of guaranteed truth", "A pattern-based system that needs evaluation", "A replacement for accountability")
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("PRACTICE", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text("Adaptive review begins with real evidence", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text("This first locally authored question records a real attempt, updates mastery, schedules review, and adds a mistake note after an error.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        Column(Modifier.fillMaxWidth().widthIn(max = 760.dp).clip(RoundedCornerShape(16.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(16.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Which statement best describes an AI model?", style = AppTypography.h2, color = Theme.colors.textPrimary)
            options.forEachIndexed { index, option ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, if (selected == index) Theme.colors.brandPrimary else Theme.colors.borderDefault, RoundedCornerShape(10.dp)).clickable { selected = index }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == index, onClick = { selected = index })
                    Spacer(Modifier.width(8.dp)); Text(option, style = AppTypography.body1, color = Theme.colors.textPrimary)
                }
            }
            PrimaryButton("Check answer", enabled = selected != null, onClick = {
                val correct = selected == 1
                repository.recordAcademyAttempt("CQAI-00001", "{\"selected\":$selected}", correct, 0, if (correct) null else "ai-certainty")
                feedback = if (correct) "Correct. Models learn patterns, but their output still needs evaluation." else "Not yet. An AI model is pattern-based and must be evaluated before it informs a real decision. Your mistake notebook has a review item."
            })
            feedback?.let { Text(it, style = AppTypography.body1, color = if (selected == 1) Theme.colors.success else Theme.colors.warning) }
        }
        SecondaryButton("Return home", onClick = { navigation.navigateTo(Screen.AcademyHome) })
    }
}

@Composable
fun AcademyLabsScreen(navigation: Navigation) {
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("LABS", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text("Build carefully, run only in a verified sandbox", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text("Python execution is intentionally unavailable in this release: untrusted code will not run inside the desktop application process. Editor files, explanations, and visual learning tools remain local.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        listOf("Python editor" to "Local file drafts and structured tests; execution coming in the next verified local release.", "Math Lab" to "Guided notation and step reasoning are planned for the local content pack.", "Algorithm Visualizer" to "Step-by-step visual explanations will be bundled without a network dependency.", "ML Lab" to "Curated offline datasets and guided notebooks are planned; no fake compute output.").forEach { (title, detail) ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).padding(18.dp)) { Text(title, style = AppTypography.h3, color = Theme.colors.textPrimary); Spacer(Modifier.height(6.dp)); Text(detail, style = AppTypography.body2, color = Theme.colors.textSecondary) }
        }
        PrimaryButton("Open local editor", onClick = { navigation.navigateTo(Screen.CodeEditor) })
    }
}

@Composable
fun AcademyLibraryScreen(title: String, subtitle: String, items: List<AcademyLibraryItem>, documentHandler: AcademyDocumentHandler) {
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var actionSucceeded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title.uppercase(), style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text(title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text(subtitle, style = AppTypography.body1, color = Theme.colors.textSecondary)
        actionMessage?.let { message ->
            Text(message, style = AppTypography.body2, color = if (actionSucceeded) Theme.colors.success else Theme.colors.error)
        }
        items.forEach { item ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).padding(18.dp)) {
                Text(item.id, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Theme.colors.brandPrimary)
                Text(item.title, style = AppTypography.h3, color = Theme.colors.textPrimary)
                Spacer(Modifier.height(4.dp)); Text(item.detail, style = AppTypography.body2, color = Theme.colors.textSecondary)
                Text("Bundled PDF", style = AppTypography.caption, color = Theme.colors.textMuted)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Open PDF", onClick = {
                        val result = documentHandler.openPdf(item)
                        actionSucceeded = result.successful
                        actionMessage = result.message
                    })
                    SecondaryButton("Download PDF", onClick = {
                        val result = documentHandler.downloadPdf(item)
                        actionSucceeded = result.successful
                        actionMessage = result.message
                    })
                }
            }
        }
    }
}

@Composable
private fun TrackGrid(tracks: List<AcademyTrackRecord>, onOpen: () -> Unit) {
    val colors = listOf(Theme.colors.brandPrimary, Theme.colors.information, Theme.colors.success, Theme.colors.warning)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tracks.forEachIndexed { index, track ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).clickable(onClick = onOpen).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(colors[index % colors.size]), contentAlignment = Alignment.Center) { Text(track.id, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold), color = Color.White) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(track.title, style = AppTypography.body1.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary); Text("${track.problemSlots} production slots · ${track.status}", style = AppTypography.caption, color = Theme.colors.textSecondary) }
            }
        }
    }
}

@Composable
private fun SummaryTile(value: String, label: String, color: Color) {
    Column(Modifier.widthIn(min = 150.dp).clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).padding(16.dp)) { Text(value, style = AppTypography.h2.copy(fontSize = 24.sp), color = color); Text(label, style = AppTypography.caption, color = Theme.colors.textSecondary) }
}

@Composable
private fun LessonSection(title: String, body: String?) {
    if (body.isNullOrBlank()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).padding(18.dp)) { Text(title, style = AppTypography.h3, color = Theme.colors.textPrimary); Spacer(Modifier.height(6.dp)); Text(body, style = AppTypography.body1, color = Theme.colors.textSecondary) }
}

private fun lessonFields(content: String): Map<String, String> = runCatching {
    val objectValue = Json.parseToJsonElement(content).jsonObject
    objectValue.mapValues { (_, value) -> if (value is kotlinx.serialization.json.JsonArray) value.jsonArray.joinToString("\n") { "• ${it.jsonPrimitive.content}" } else value.jsonPrimitive.content }
}.getOrDefault(emptyMap())
