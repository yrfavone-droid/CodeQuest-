package com.codequest.academy.shared.ui.theme

import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nousaiacademy.shared.generated.resources.Res
import nousaiacademy.shared.generated.resources.jetbrains_mono_variable
import nousaiacademy.shared.generated.resources.manrope_variable
import nousaiacademy.shared.generated.resources.source_serif_4_variable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

@Composable
@OptIn(ExperimentalResourceApi::class)
private fun interfaceFont() = FontFamily(
    Font(Res.font.manrope_variable, FontWeight.Normal),
    Font(Res.font.manrope_variable, FontWeight.SemiBold),
    Font(Res.font.manrope_variable, FontWeight.Bold)
)

@Composable
@OptIn(ExperimentalResourceApi::class)
private fun readingFont() = FontFamily(Font(Res.font.source_serif_4_variable, FontWeight.Normal))

@Composable
@OptIn(ExperimentalResourceApi::class)
private fun technicalFont() = FontFamily(Font(Res.font.jetbrains_mono_variable, FontWeight.Normal))

val AppTypography: Typography
    @Composable get() {
        val family = interfaceFont()
        return Typography(
            h1 = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
            h2 = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
            h3 = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
            body1 = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
            body2 = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
            button = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
            caption = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp)
        )
    }

val DisplayStyle: TextStyle
    @Composable get() = TextStyle(fontFamily = interfaceFont(), fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp)

val ArticleStyle: TextStyle
    @Composable get() = TextStyle(fontFamily = readingFont(), fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 29.sp)

val CodeStyle: TextStyle
    @Composable get() = TextStyle(fontFamily = technicalFont(), fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp)
