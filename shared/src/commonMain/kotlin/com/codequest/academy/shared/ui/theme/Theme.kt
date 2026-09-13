package com.codequest.academy.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppearanceMode { System, Light, Dark }

object AppearanceState {
    var mode by mutableStateOf(AppearanceMode.System)
    var reduceMotion by mutableStateOf(false)
    var largerText by mutableStateOf(false)
}

class NousColors(
    val appBackground: Color, val elevatedBackground: Color, val surfacePrimary: Color,
    val surfaceSecondary: Color, val surfaceTertiary: Color, val textPrimary: Color,
    val textSecondary: Color, val textMuted: Color, val textAccent: Color,
    val disabledText: Color, val borderDefault: Color, val borderStrong: Color,
    val focusRing: Color, val brandPrimary: Color, val brandPrimaryHover: Color,
    val brandPrimaryPressed: Color, val brandSoft: Color, val accentCyan: Color,
    val accentGold: Color, val accentLime: Color, val information: Color,
    val informationSoft: Color, val success: Color, val successSoft: Color,
    val warning: Color, val warningSoft: Color, val error: Color, val errorSoft: Color,
    val locked: Color, val lockedSoft: Color, val isDark: Boolean
)

private val LightNousColors = NousColors(
    AppBackground, ElevatedBackground, SurfacePrimary, SurfaceSecondary, SurfaceTertiary,
    TextPrimary, TextSecondary, TextMuted, TextAccent, DisabledText, BorderDefault, BorderStrong,
    FocusRing, BrandPrimary, BrandPrimaryHover, BrandPrimaryPressed, BrandSoft, AccentCyan,
    AccentGold, AccentLime, Information, InformationSoft, Success, SuccessSoft, Warning,
    WarningSoft, ErrorColor, ErrorSoft, Locked, LockedSoft, false
)

private val DarkNousColors = NousColors(
    DarkAppBackground, DarkElevatedBackground, DarkSurfacePrimary, DarkSurfaceSecondary,
    DarkSurfaceTertiary, DarkTextPrimary, DarkTextSecondary, DarkTextMuted, DarkBrandPrimary,
    DarkTextMuted, DarkBorderDefault, DarkBorderStrong, DarkInformation, DarkBrandPrimary,
    DarkBrandHover, DarkBrandPressed, DarkBrandSoft, DarkInformation, DarkTextPrimary, DarkSuccess,
    DarkInformation, DarkInformation.copy(alpha = .16f), DarkSuccess, DarkSuccess.copy(alpha = .16f),
    DarkTextPrimary, DarkTextPrimary.copy(alpha = .16f), DarkError, DarkError.copy(alpha = .16f),
    DarkTextMuted, DarkSurfaceTertiary, true
)

val LocalNousColors = staticCompositionLocalOf { LightNousColors }
val LocalMotionEnabled = staticCompositionLocalOf { true }

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun NousTheme(content: @Composable () -> Unit) {
    var osReducedMotion by remember { mutableStateOf(com.codequest.academy.shared.platform.systemReducedMotion()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            osReducedMotion = com.codequest.academy.shared.platform.systemReducedMotion()
        }
    }
    val dark = when (AppearanceState.mode) {
        AppearanceMode.System -> isSystemInDarkTheme()
        AppearanceMode.Light -> false
        AppearanceMode.Dark -> true
    }
    val palette = if (dark) DarkNousColors else LightNousColors
    val material = if (dark) darkColors(
        primary = palette.brandPrimary, primaryVariant = palette.brandPrimaryPressed,
        secondary = palette.information, background = palette.appBackground,
        surface = palette.surfacePrimary, onPrimary = Color.Black,
        onSecondary = palette.textPrimary, onBackground = palette.textPrimary,
        onSurface = palette.textPrimary, error = palette.error, onError = Color.White
    ) else lightColors(
        primary = palette.brandPrimary, primaryVariant = palette.brandPrimaryPressed,
        secondary = palette.information, background = palette.appBackground,
        surface = palette.surfacePrimary, onPrimary = Color.White,
        onSecondary = palette.textPrimary, onBackground = palette.textPrimary,
        onSurface = palette.textPrimary, error = palette.error, onError = Color.White
    )
    CompositionLocalProvider(
        LocalNousColors provides palette,
        LocalMotionEnabled provides (!AppearanceState.reduceMotion && !osReducedMotion)
    ) {
        MaterialTheme(colors = material, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

object Theme {
    val colors: NousColors @Composable get() = LocalNousColors.current
    val motionEnabled: Boolean @Composable get() = LocalMotionEnabled.current
}
