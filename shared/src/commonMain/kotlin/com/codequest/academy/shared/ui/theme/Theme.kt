package com.codequest.academy.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Semantic Colors for composition local
class CodeQuestColors(
    val appBackground: Color = AppBackground,
    val surfacePrimary: Color = SurfacePrimary,
    val surfaceSecondary: Color = SurfaceSecondary,
    val surfaceTertiary: Color = SurfaceTertiary,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val textAccent: Color = TextAccent,
    val borderDefault: Color = BorderDefault,
    val borderStrong: Color = BorderStrong,
    val focusRing: Color = FocusRing,
    val brandPrimary: Color = BrandPrimary,
    val brandPrimaryHover: Color = BrandPrimaryHover,
    val brandPrimaryPressed: Color = BrandPrimaryPressed,
    val brandSoft: Color = BrandSoft,
    val accentCyan: Color = AccentCyan,
    val accentGold: Color = AccentGold,
    val accentLime: Color = AccentLime,
    val information: Color = Information,
    val informationSoft: Color = InformationSoft,
    val success: Color = Success,
    val successSoft: Color = SuccessSoft,
    val warning: Color = Warning,
    val warningSoft: Color = WarningSoft,
    val error: Color = ErrorColor,
    val errorSoft: Color = ErrorSoft,
    val locked: Color = Locked,
    val lockedSoft: Color = LockedSoft
)

val LocalCodeQuestColors = staticCompositionLocalOf { CodeQuestColors() }

private val DarkColorPalette = darkColors(
    primary = BrandPrimary,
    primaryVariant = BrandPrimaryHover,
    secondary = AccentCyan,
    background = AppBackground,
    surface = SurfacePrimary,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorColor,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // Standard cards
    large = RoundedCornerShape(16.dp)  // Panels and hero cards
)

@Composable
fun CodeQuestTheme(content: @Composable () -> Unit) {
    val codeQuestColors = CodeQuestColors()
    
    CompositionLocalProvider(LocalCodeQuestColors provides codeQuestColors) {
        MaterialTheme(
            colors = DarkColorPalette,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

object Theme {
    val colors: CodeQuestColors
        @Composable
        get() = LocalCodeQuestColors.current
}
