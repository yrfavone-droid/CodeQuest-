package com.codequest.academy.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codequestacademy.shared.generated.resources.Res
import codequestacademy.shared.generated.resources.codequest_ai_book_icon
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.platform.applicationVersion
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
fun AppShell(
    navigation: Navigation,
    isRailExpanded: Boolean,
    onToggleRail: () -> Unit,
    contextPanelContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        val authenticatedScreen = navigation.currentScreen !in setOf(
            Screen.CurriculumLoading, Screen.CreateAccount, Screen.SignIn, Screen.LegacyCredentialSetup
        )
        val compact = maxWidth < 980.dp
        val wide = maxWidth >= 1540.dp
        Row(Modifier.fillMaxSize()) {
            if (authenticatedScreen) {
                WorkspaceRail(navigation, !compact && isRailExpanded, onToggleRail, Modifier.width(if (compact || !isRailExpanded) 76.dp else 252.dp).fillMaxHeight())
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (authenticatedScreen) WorkspaceHeader()
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.fillMaxSize().widthIn(max = 1500.dp)) { content() }
                }
            }
            if (wide && authenticatedScreen && contextPanelContent != null) {
                Box(Modifier.width(320.dp).fillMaxHeight().background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault)) { contextPanelContent() }
            }
        }
    }
}

@Composable
private fun WorkspaceHeader() {
    Row(
        Modifier.fillMaxWidth().height(64.dp).background(Theme.colors.appBackground).border(1.dp, Theme.colors.borderDefault).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("CODEQUEST WORKSPACE", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.textMuted)
            Text("Learning systems, not shortcuts", style = AppTypography.body2, color = Theme.colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusPill("LOCAL DATA", Theme.colors.success, Theme.colors.successSoft)
            Text("v${applicationVersion()}", style = AppTypography.caption, color = Theme.colors.textMuted)
        }
    }
}

@Composable
fun StatusPill(label: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(999.dp)).background(background).padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(99.dp)).background(color))
        Text(label, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp), color = color)
    }
}

@Composable
private fun WorkspaceRail(navigation: Navigation, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault).padding(vertical = 18.dp), horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally) {
        BrandLockup(expanded)
        Spacer(Modifier.height(20.dp)); RailSectionLabel("WORKSPACE", expanded)
        RailItem("HM", "Home", navigation.currentScreen == Screen.AcademyHome, expanded) { navigation.navigateTo(Screen.AcademyHome) }
        RailItem("LN", "Learn", navigation.currentScreen == Screen.AcademyLearn || navigation.currentScreen is Screen.AcademyLesson, expanded) { navigation.navigateTo(Screen.AcademyLearn) }
        RailItem("PR", "Practice", navigation.currentScreen == Screen.AcademyPractice, expanded) { navigation.navigateTo(Screen.AcademyPractice) }
        RailItem("LB", "Labs", navigation.currentScreen == Screen.AcademyLabs || navigation.currentScreen == Screen.CodeEditor, expanded) { navigation.navigateTo(Screen.AcademyLabs) }
        RailItem("PJ", "Projects", navigation.currentScreen == Screen.Projects || navigation.currentScreen is Screen.Project, expanded) { navigation.navigateTo(Screen.Projects) }
        RailItem("BK", "Books", navigation.currentScreen == Screen.AcademyBooks, expanded) { navigation.navigateTo(Screen.AcademyBooks) }
        RailItem("KN", "Knowledge", navigation.currentScreen == Screen.AcademyKnowledge, expanded) { navigation.navigateTo(Screen.AcademyKnowledge) }
        RailItem("PR", "Progress", navigation.currentScreen == Screen.Progress, expanded) { navigation.navigateTo(Screen.Progress) }
        Spacer(Modifier.weight(1f)); RailSectionLabel("ACCOUNT", expanded)
        RailItem("AC", "Profile & backup", navigation.currentScreen == Screen.Profile, expanded) { navigation.navigateTo(Screen.Profile) }
        RailItem("ST", "Settings", navigation.currentScreen == Screen.Settings, expanded) { navigation.navigateTo(Screen.Settings) }
        RailItem(if (expanded) "‹" else "›", if (expanded) "Collapse" else "Expand", false, expanded, onToggle)
    }
}

@Composable
@OptIn(ExperimentalResourceApi::class)
private fun BrandLockup(expanded: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = if (expanded) 16.dp else 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(Res.drawable.codequest_ai_book_icon),
                contentDescription = "CodeQuest AI Academy",
                modifier = Modifier.fillMaxSize()
            )
        }
        if (expanded) {
            Spacer(Modifier.width(11.dp)); Column {
                Text("CodeQuest", style = AppTypography.body2.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textPrimary)
                Text("ACADEMY", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.accentCyan)
            }
        }
    }
}

@Composable
private fun RailSectionLabel(label: String, expanded: Boolean) {
    if (expanded) Text(label, Modifier.padding(start = 22.dp, bottom = 6.dp), style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.textMuted)
}

private fun isLearningScreen(screen: Screen): Boolean = screen is Screen.AcademyLearn || screen is Screen.AcademyLesson || screen is Screen.PathDetails || screen is Screen.LevelOverview || screen is Screen.LearningMap || screen is Screen.Diagnostic || screen is Screen.CheatSheet || screen is Screen.Lesson || screen is Screen.Practice || screen is Screen.Challenge || screen is Screen.MixedReview || screen is Screen.FinalQuiz

@Composable
private fun RailItem(token: String, label: String, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) Theme.colors.brandSoft else Color.Transparent, tween(160))
    val foreground by animateColorAsState(if (selected) Theme.colors.textPrimary else Theme.colors.textSecondary, tween(160))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).height(44.dp).clip(RoundedCornerShape(8.dp)).background(background).clickable(onClick = onClick).padding(horizontal = if (expanded) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
    ) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(if (selected) Theme.colors.brandPrimary else Theme.colors.surfaceTertiary), contentAlignment = Alignment.Center) {
            Text(token, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else Theme.colors.textMuted)
        }
        if (expanded) {
            Spacer(Modifier.width(11.dp)); Text(label, style = AppTypography.body2.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal), color = foreground)
        }
    }
}
