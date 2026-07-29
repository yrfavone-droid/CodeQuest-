package com.codequest.academy.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.Theme

@Composable
fun AppShell(
    navigation: Navigation,
    isRailExpanded: Boolean,
    onToggleRail: () -> Unit,
    contextPanelContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        val showNavigation = navigation.currentScreen !in setOf(
            Screen.CurriculumLoading, Screen.CreateAccount, Screen.SignIn, Screen.LegacyCredentialSetup
        )
        val compact = maxWidth < 900.dp
        val wide = maxWidth >= 1500.dp
        Row(Modifier.fillMaxSize()) {
            if (showNavigation) SideNavigationRail(
                navigation, isExpanded = !compact && isRailExpanded, onToggle = onToggleRail,
                modifier = Modifier.width(if (compact || !isRailExpanded) 76.dp else 260.dp).fillMaxHeight()
            )
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.fillMaxSize().widthIn(max = 1440.dp)) { content() }
            }
            if (wide && contextPanelContent != null) Box(Modifier.width(340.dp).fillMaxHeight().background(Theme.colors.surfaceSecondary)) { contextPanelContent() }
        }
    }
}

@Composable
private fun SideNavigationRail(navigation: Navigation, isExpanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(Theme.colors.surfacePrimary).padding(vertical = 22.dp), horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (isExpanded) 20.dp else 0.dp).padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(
                    Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF00D9FF)))
                ).border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("{", color = Color(0xFF7C5CFF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Σ", color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("}", color = Color(0xFF00D9FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            if (isExpanded) {
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("CodeQuest", style = AppTypography.h3.copy(color = Color.White))
                    Text("MATH & CODE", style = AppTypography.caption.copy(color = Theme.colors.accentCyan, fontWeight = FontWeight.Bold))
                }
            }
        }
        NavItem("∑", "Dashboard", navigation.currentScreen == Screen.Dashboard, isExpanded) { navigation.navigateTo(Screen.Dashboard) }
        NavItem("∫", "Learn", isLearningScreen(navigation.currentScreen), isExpanded) { navigation.navigateTo(Screen.TrackBrowser) }
        NavItem("π", "Tracks", navigation.currentScreen == Screen.TrackBrowser || navigation.currentScreen is Screen.TrackDetails, isExpanded) { navigation.navigateTo(Screen.TrackBrowser) }
        NavItem("√", "Review", navigation.currentScreen == Screen.Review || navigation.currentScreen is Screen.AdaptiveReview, isExpanded) { navigation.navigateTo(Screen.Review) }
        NavItem("f(x)", "Projects", navigation.currentScreen == Screen.Projects || navigation.currentScreen is Screen.Project, isExpanded) { navigation.navigateTo(Screen.Projects) }
        NavItem("</>", "Code Editor", navigation.currentScreen == Screen.CodeEditor, isExpanded) { navigation.navigateTo(Screen.CodeEditor) }
        NavItem("↗", "Progress", navigation.currentScreen == Screen.Progress, isExpanded) { navigation.navigateTo(Screen.Progress) }
        Spacer(Modifier.weight(1f))
        NavItem("●", "Profile", navigation.currentScreen == Screen.Profile, isExpanded) { navigation.navigateTo(Screen.Profile) }
        NavItem("⚙", "Settings", navigation.currentScreen == Screen.Settings, isExpanded) { navigation.navigateTo(Screen.Settings) }
        NavItem(if (isExpanded) "‹" else "›", if (isExpanded) "Collapse" else "Expand", false, isExpanded, onToggle)
    }
}

private fun isLearningScreen(screen: Screen): Boolean = screen is Screen.PathDetails || screen is Screen.LevelOverview || screen is Screen.LearningMap || screen is Screen.Diagnostic || screen is Screen.CheatSheet || screen is Screen.Lesson || screen is Screen.Practice || screen is Screen.Challenge || screen is Screen.MixedReview || screen is Screen.FinalQuiz

@Composable
private fun NavItem(icon: String, label: String, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val animatedBg by animateColorAsState(
        targetValue = if (selected) Theme.colors.brandSoft else Color.Transparent,
        animationSpec = tween(durationMillis = 300)
    )
    val animatedColor by animateColorAsState(
        targetValue = if (selected) Theme.colors.accentCyan else Theme.colors.textSecondary,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp).height(50.dp).clip(RoundedCornerShape(10.dp)).background(animatedBg).clickable(onClick = onClick),
        contentAlignment = if (expanded) Alignment.CenterStart else Alignment.Center
    ) {
        Row(Modifier.padding(horizontal = if (expanded) 15.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                icon,
                fontSize = 18.sp,
                color = if (selected) Theme.colors.accentGold else animatedColor,
                fontWeight = FontWeight.Bold
            )
            if (expanded) {
                Spacer(Modifier.width(15.dp))
                Text(
                    label,
                    style = AppTypography.body2.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                    color = animatedColor
                )
            }
        }
    }
}
