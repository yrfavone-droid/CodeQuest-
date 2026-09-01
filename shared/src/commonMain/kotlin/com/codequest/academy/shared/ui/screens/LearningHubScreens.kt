package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme

data class LearningHubSectionShell(
    val id: String,
    val number: Int,
    val title: String,
    val description: String
)

object LearningHubCatalog {
    val sections = listOf(
        "AI Foundations" to "Core concepts, systems, and responsible practice.",
        "Python for AI" to "A practical programming foundation for technical work.",
        "Mathematics for AI" to "The mathematical language used to reason about models.",
        "Linear Algebra" to "Vectors, matrices, and transformations for data.",
        "Probability and Statistics" to "Uncertainty, variation, and evidence in data.",
        "Calculus and Optimization" to "Rates of change and methods for improving models.",
        "Data Structures" to "Reliable ways to organize and access information.",
        "Algorithms and Problem Solving" to "Structured approaches to efficient solutions.",
        "NumPy and Data Handling" to "Numerical arrays, cleaning, and reproducible analysis.",
        "SQL and Databases" to "Querying and modeling reliable data systems.",
        "Machine Learning Foundations" to "Problem framing, data splits, and baselines.",
        "Regression and Classification" to "Foundational predictive models and comparisons.",
        "Model Evaluation" to "Metrics, validation, calibration, and error analysis.",
        "Feature Engineering" to "Reproducible transformations without leakage.",
        "Deep Learning Foundations" to "Neural representations, losses, and training.",
        "PyTorch and Neural Training" to "Tensors, autograd, and dependable experiments.",
        "Computer Vision" to "Learning from images, pixels, and visual structure.",
        "Natural Language Processing" to "Representing, analyzing, and generating text.",
        "Transformers and LLMs" to "Attention, retrieval, and grounded language systems.",
        "AI Deployment and Responsible AI" to "Shipping, monitoring, privacy, and accountability."
    ).mapIndexed { index, (title, description) ->
        LearningHubSectionShell("HUB-${(index + 1).toString().padStart(2, '0')}", index + 1, title, description)
    }
}

@Composable
fun LearningHubHomeScreen(navigation: Navigation) {
    Column(
        Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(horizontal = 48.dp, vertical = 42.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("LEARNING HUB", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.3.sp), color = Theme.colors.brandPrimary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A deliberate path into AI", style = DisplayStyle.copy(color = Theme.colors.textPrimary))
                Text("Twenty structured domains are ready for the verified curriculum package. Nothing is counted as complete until official lessons and practice are imported.", style = AppTypography.body1, color = Theme.colors.textSecondary, modifier = Modifier.fillMaxWidth(.8f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CONTENT STATUS", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = .9.sp), color = Theme.colors.textMuted)
                Text("AWAITING IMPORT", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = .8.sp), color = Theme.colors.brandPrimary)
            }
        }
        LearningHubEmptyBanner()
        LearningHubCatalog.sections.chunked(2).forEach { rowSections ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                rowSections.forEach { section ->
                    LearningHubSectionCard(section, navigation, Modifier.weight(1f))
                }
                if (rowSections.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LearningHubEmptyBanner() {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.colors.brandSoft).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(16.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        HubGlyph(0, Theme.colors.brandPrimary)
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Curriculum content coming soon", style = AppTypography.h2, color = Theme.colors.textPrimary)
            Text("Lessons, Quick Sheets, and practice will appear after the verified written package is imported and validated.", style = AppTypography.body2, color = Theme.colors.textSecondary)
        }
    }
}

@Composable
private fun LearningHubSectionCard(section: LearningHubSectionShell, navigation: Navigation, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(15.dp)).padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HubGlyph(section.number, Theme.colors.brandPrimary)
                Spacer(Modifier.width(11.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(section.id, style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = .8.sp), color = Theme.colors.brandPrimary)
                    Text(section.title, style = AppTypography.h2.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Theme.colors.textPrimary)
                }
            }
            Text("NOT READY", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = .7.sp), color = Theme.colors.textMuted)
        }
        Text(section.description, style = AppTypography.body2, color = Theme.colors.textSecondary)
        Text("No lessons or practice imported yet", style = AppTypography.caption, color = Theme.colors.textMuted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SecondaryButton("View section", { navigation.navigateToLearningHubSection(section.id) }, Modifier.weight(1f))
            PrimaryButton("Start learning", {}, Modifier.weight(1f), enabled = false)
        }
    }
}

@Composable
fun LearningHubSectionScreen(navigation: Navigation) {
    val section = LearningHubCatalog.sections.firstOrNull { it.id == navigation.selectedLearningHubSectionId } ?: LearningHubCatalog.sections.first()
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(horizontal = 48.dp, vertical = 42.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("LEARNING HUB / ${section.id}", style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            HubGlyph(section.number, Theme.colors.brandPrimary)
            Spacer(Modifier.width(14.dp))
            Text(section.title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        }
        Text("Curriculum content coming soon", style = AppTypography.h2, color = Theme.colors.textPrimary)
        Text("Lessons, Quick Sheets, and practice will appear here after the verified curriculum package is imported. This section is intentionally empty so no placeholder learning is mistaken for official content.", style = AppTypography.body1, color = Theme.colors.textSecondary, modifier = Modifier.fillMaxWidth(.78f))
        DisabledHubArea("LESSONS", "Official lesson content has not been imported.")
        DisabledHubArea("QUICK SHEETS", "Quick Sheets will be generated only from verified lesson material.")
        DisabledHubArea("PRACTICE", "Practice questions and explanations will appear after validation.")
        DisabledHubArea("SECTION CHALLENGE", "The final challenge will unlock with the official section package.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Back to Learning Hub", { navigation.pop() })
            PrimaryButton("Start learning", {}, enabled = false)
        }
    }
}

@Composable
private fun DisabledHubArea(label: String, message: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(13.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        HubGlyph(label.length, Theme.colors.textMuted)
        Spacer(Modifier.width(13.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = AppTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = .9.sp), color = Theme.colors.textMuted)
            Text(message, style = AppTypography.body2, color = Theme.colors.textSecondary)
        }
    }
}

@Composable
private fun HubGlyph(seed: Int, color: Color) {
    Canvas(Modifier.size(36.dp).semantics { contentDescription = "Learning Hub section icon" }) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        when (seed % 5) {
            0 -> {
                drawCircle(color, radius = w * .28f, center = androidx.compose.ui.geometry.Offset(w * .5f, h * .5f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .5f, h * .1f), androidx.compose.ui.geometry.Offset(w * .5f, h * .28f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .5f, h * .72f), androidx.compose.ui.geometry.Offset(w * .5f, h * .9f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .1f, h * .5f), androidx.compose.ui.geometry.Offset(w * .28f, h * .5f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .72f, h * .5f), androidx.compose.ui.geometry.Offset(w * .9f, h * .5f), strokeWidth = stroke.width)
            }
            1 -> {
                val path = Path().apply { moveTo(w * .18f, h * .2f); lineTo(w * .44f, h * .2f); lineTo(w * .33f, h * .8f); lineTo(w * .18f, h * .8f); close() }
                drawPath(path, color, style = stroke)
                val right = Path().apply { moveTo(w * .56f, h * .2f); lineTo(w * .82f, h * .2f); lineTo(w * .82f, h * .8f); lineTo(w * .56f, h * .8f); close() }
                drawPath(right, color, style = stroke)
            }
            2 -> {
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .18f, h * .18f), size = androidx.compose.ui.geometry.Size(w * .64f, h * .64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .32f, h * .38f), androidx.compose.ui.geometry.Offset(w * .68f, h * .38f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .32f, h * .58f), androidx.compose.ui.geometry.Offset(w * .6f, h * .58f), strokeWidth = stroke.width)
            }
            3 -> {
                drawCircle(color, radius = w * .12f, center = androidx.compose.ui.geometry.Offset(w * .25f, h * .3f), style = stroke)
                drawCircle(color, radius = w * .12f, center = androidx.compose.ui.geometry.Offset(w * .75f, h * .3f), style = stroke)
                drawCircle(color, radius = w * .12f, center = androidx.compose.ui.geometry.Offset(w * .5f, h * .72f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .35f, h * .36f), androidx.compose.ui.geometry.Offset(w * .45f, h * .64f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .65f, h * .36f), androidx.compose.ui.geometry.Offset(w * .55f, h * .64f), strokeWidth = stroke.width)
            }
            else -> {
                drawArc(color, -75f, 210f, false, topLeft = androidx.compose.ui.geometry.Offset(w * .16f, h * .16f), size = androidx.compose.ui.geometry.Size(w * .68f, h * .68f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .5f, h * .5f), androidx.compose.ui.geometry.Offset(w * .76f, h * .33f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}
