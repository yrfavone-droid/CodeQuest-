package com.codequest.academy.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nousaiacademy.shared.generated.resources.Res
import nousaiacademy.shared.generated.resources.nous_ai_academy_logo
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
            Screen.WorkspaceLoading, Screen.CreateAccount, Screen.SignIn, Screen.LegacyCredentialSetup
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
            Text("NOUS AI ACADEMY", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.textMuted)
            Text("Offline technical library", style = AppTypography.body2, color = Theme.colors.textSecondary)
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
        RailItem(RailIcon.Home, "Home", navigation.currentScreen == Screen.Home, expanded) { navigation.navigateTo(Screen.Home) }
        RailItem(RailIcon.Library, "Learning Library", navigation.currentScreen == Screen.LearningLibrary, expanded) { navigation.navigateTo(Screen.LearningLibrary) }
        RailItem(RailIcon.Book, "Books", navigation.currentScreen == Screen.Books, expanded) { navigation.navigateTo(Screen.Books) }
        RailItem(RailIcon.File, "Intensive Files", navigation.currentScreen == Screen.IntensiveFiles, expanded) { navigation.navigateTo(Screen.IntensiveFiles) }
        RailItem(RailIcon.Progress, "Reading Progress", navigation.currentScreen == Screen.ReadingProgress, expanded) { navigation.navigateTo(Screen.ReadingProgress) }
        RailItem(RailIcon.Bookmark, "Bookmarks", navigation.currentScreen == Screen.Bookmarks, expanded) { navigation.navigateTo(Screen.Bookmarks) }
        RailItem(RailIcon.Search, "Search", navigation.currentScreen == Screen.Search, expanded) { navigation.navigateTo(Screen.Search) }
        Spacer(Modifier.weight(1f)); RailSectionLabel("ACCOUNT", expanded)
        RailItem(RailIcon.Profile, "Profile & backup", navigation.currentScreen == Screen.Profile, expanded) { navigation.navigateTo(Screen.Profile) }
        RailItem(RailIcon.Settings, "Settings", navigation.currentScreen == Screen.Settings, expanded) { navigation.navigateTo(Screen.Settings) }
        RailItem(RailIcon.Info, "About", navigation.currentScreen == Screen.About, expanded) { navigation.navigateTo(Screen.About) }
        RailItem(if (expanded) RailIcon.Collapse else RailIcon.Expand, if (expanded) "Collapse" else "Expand", false, expanded, onToggle)
    }
}

@Composable
@OptIn(ExperimentalResourceApi::class)
private fun BrandLockup(expanded: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = if (expanded) 16.dp else 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(Res.drawable.nous_ai_academy_logo),
                contentDescription = "Nous AI Academy",
                modifier = Modifier.fillMaxSize()
            )
        }
        if (expanded) {
            Spacer(Modifier.width(11.dp)); Column {
                Text("Nous AI", style = AppTypography.body2.copy(fontWeight = FontWeight.Bold), color = Theme.colors.textPrimary)
                Text("ACADEMY", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.brandPrimary)
            }
        }
    }
}

@Composable
private fun RailSectionLabel(label: String, expanded: Boolean) {
    if (expanded) Text(label, Modifier.padding(start = 22.dp, bottom = 6.dp), style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.textMuted)
}

@Composable
private fun RailItem(icon: RailIcon, label: String, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) Theme.colors.brandSoft else Color.Transparent, tween(160))
    val foreground by animateColorAsState(if (selected) Theme.colors.textPrimary else Theme.colors.textSecondary, tween(160))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).height(44.dp).clip(RoundedCornerShape(8.dp)).background(background).clickable(onClick = onClick).semantics {
            contentDescription = label
            role = Role.Button
        }.padding(horizontal = if (expanded) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
    ) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(if (selected) Theme.colors.brandPrimary else Theme.colors.surfaceTertiary), contentAlignment = Alignment.Center) {
            RailIconGlyph(icon, if (selected) Color.White else Theme.colors.textMuted)
        }
        if (expanded) {
            Spacer(Modifier.width(11.dp)); Text(label, style = AppTypography.body2.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal), color = foreground)
        }
    }
}

private enum class RailIcon { Home, Library, Book, File, Progress, Bookmark, Search, Profile, Settings, Info, Collapse, Expand }

@Composable
private fun RailIconGlyph(icon: RailIcon, color: Color) {
    Canvas(Modifier.size(17.dp)) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            RailIcon.Home -> {
                val roof = Path().apply { moveTo(w * .15f, h * .46f); lineTo(w * .5f, h * .15f); lineTo(w * .85f, h * .46f) }
                drawPath(roof, color, style = stroke)
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .25f, h * .42f), size = androidx.compose.ui.geometry.Size(w * .5f, h * .4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .5f, h * .82f), androidx.compose.ui.geometry.Offset(w * .5f, h * .59f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            RailIcon.Library -> {
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .16f, h * .24f), size = androidx.compose.ui.geometry.Size(w * .68f, h * .56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .5f, h * .25f), androidx.compose.ui.geometry.Offset(w * .5f, h * .8f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .26f, h * .38f), androidx.compose.ui.geometry.Offset(w * .43f, h * .38f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .57f, h * .38f), androidx.compose.ui.geometry.Offset(w * .74f, h * .38f), strokeWidth = stroke.width)
            }
            RailIcon.Book -> {
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .19f, h * .16f), size = androidx.compose.ui.geometry.Size(w * .62f, h * .68f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .3f, h * .3f), androidx.compose.ui.geometry.Offset(w * .7f, h * .3f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .3f, h * .46f), androidx.compose.ui.geometry.Offset(w * .7f, h * .46f), strokeWidth = stroke.width)
            }
            RailIcon.File -> {
                val page = Path().apply { moveTo(w * .27f, h * .14f); lineTo(w * .62f, h * .14f); lineTo(w * .78f, h * .3f); lineTo(w * .78f, h * .86f); lineTo(w * .27f, h * .86f); close() }
                drawPath(page, color, style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .62f, h * .14f), androidx.compose.ui.geometry.Offset(w * .62f, h * .3f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .62f, h * .3f), androidx.compose.ui.geometry.Offset(w * .78f, h * .3f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .37f, h * .48f), androidx.compose.ui.geometry.Offset(w * .67f, h * .48f), strokeWidth = stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .37f, h * .64f), androidx.compose.ui.geometry.Offset(w * .61f, h * .64f), strokeWidth = stroke.width)
            }
            RailIcon.Progress -> {
                drawCircle(color, radius = w * .34f, center = androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = stroke)
                drawArc(color, -90f, 105f, false, topLeft = androidx.compose.ui.geometry.Offset(w * .16f, h * .16f), size = androidx.compose.ui.geometry.Size(w * .68f, h * .68f), style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
            }
            RailIcon.Bookmark -> {
                val mark = Path().apply { moveTo(w * .28f, h * .16f); lineTo(w * .72f, h * .16f); lineTo(w * .72f, h * .84f); lineTo(w * .5f, h * .66f); lineTo(w * .28f, h * .84f); close() }
                drawPath(mark, color, style = stroke)
            }
            RailIcon.Search -> {
                drawCircle(color, radius = w * .28f, center = androidx.compose.ui.geometry.Offset(w * .43f, h * .42f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .63f, h * .62f), androidx.compose.ui.geometry.Offset(w * .84f, h * .83f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            RailIcon.Profile -> {
                drawCircle(color, radius = w * .18f, center = androidx.compose.ui.geometry.Offset(w / 2, h * .3f), style = stroke)
                drawArc(color, 200f, 140f, false, topLeft = androidx.compose.ui.geometry.Offset(w * .22f, h * .42f), size = androidx.compose.ui.geometry.Size(w * .56f, h * .48f), style = stroke)
            }
            RailIcon.Settings -> {
                drawCircle(color, radius = w * .27f, center = androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = stroke)
                drawCircle(color, radius = w * .08f, center = androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = stroke)
                for (i in 0 until 8) {
                    val angle = (i * 45f) * kotlin.math.PI.toFloat() / 180f
                    val inner = w * .36f; val outer = w * .47f
                    val cosine = kotlin.math.cos(angle.toDouble()).toFloat()
                    val sine = kotlin.math.sin(angle.toDouble()).toFloat()
                    drawLine(color, androidx.compose.ui.geometry.Offset(w / 2 + cosine * inner, h / 2 + sine * inner), androidx.compose.ui.geometry.Offset(w / 2 + cosine * outer, h / 2 + sine * outer), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            RailIcon.Info -> {
                drawCircle(color, radius = w * .36f, center = androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = stroke)
                drawCircle(color, radius = w * .045f, center = androidx.compose.ui.geometry.Offset(w / 2, h * .32f))
                drawLine(color, androidx.compose.ui.geometry.Offset(w / 2, h * .45f), androidx.compose.ui.geometry.Offset(w / 2, h * .7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            RailIcon.Collapse, RailIcon.Expand -> {
                val pointsLeft = icon == RailIcon.Collapse
                val baseX = if (pointsLeft) w * .64f else w * .36f
                val tipX = if (pointsLeft) w * .36f else w * .64f
                drawLine(color, androidx.compose.ui.geometry.Offset(baseX, h * .2f), androidx.compose.ui.geometry.Offset(tipX, h / 2), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(tipX, h / 2), androidx.compose.ui.geometry.Offset(baseX, h * .8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}
