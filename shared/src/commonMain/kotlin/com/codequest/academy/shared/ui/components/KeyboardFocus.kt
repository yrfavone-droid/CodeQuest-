package com.codequest.academy.shared.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.theme.Theme

/** Explicit focus outline in addition to hover/selection, never color-only feedback. */
fun Modifier.keyboardFocusRing(): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val color = Theme.colors.focusRing
    onFocusChanged { focused = it.hasFocus }.drawWithContent {
        drawContent()
        if (focused) drawRoundRect(color, cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()), style = Stroke(2.5.dp.toPx()))
    }
}
