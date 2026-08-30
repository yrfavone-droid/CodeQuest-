package com.codequest.academy.shared.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme

/** Honest empty states used until the owner supplies a verified curriculum package. */
@Composable
fun CleanLibraryScreen(title: String, message: String, navigation: Navigation) {
    Column(
        Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(horizontal = 48.dp, vertical = 46.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("LIBRARY TRANSITION", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = Theme.colors.brandPrimary)
        Text(title, style = DisplayStyle.copy(color = Theme.colors.textPrimary))
        Text(message, style = AppTypography.body1, color = Theme.colors.textSecondary, modifier = Modifier.fillMaxWidth(.78f))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            Column(
                Modifier.weight(1.4f).clip(RoundedCornerShape(18.dp)).background(Theme.colors.surfacePrimary).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(18.dp)).padding(30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Theme.colors.brandSoft), contentAlignment = Alignment.Center) {
                    Text("N", fontWeight = FontWeight.Bold, color = Theme.colors.brandPrimary, fontSize = 22.sp)
                }
                Text("Ready for the official package", style = AppTypography.h2, color = Theme.colors.textPrimary)
                Text("This workspace intentionally contains no sample curriculum, placeholder PDFs, artificial counts, or invented activity. Verified content will be added only after the owner provides the final Nous package.", style = AppTypography.body1, color = Theme.colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("View library status", onClick = { navigation.navigateTo(Screen.LearningLibrary) })
                    SecondaryButton("Open settings", onClick = { navigation.navigateTo(Screen.Settings) })
                }
            }
            Column(
                Modifier.weight(.8f).clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(18.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text("PRIVATE WORKSPACE", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.brandPrimary)
                StatusRow("Accounts and settings", "Retained on this device")
                StatusRow("Bookmarks and reading history", "Preserved; unavailable documents stay closed")
                StatusRow("Curriculum content", "Awaiting the final verified package")
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(10.dp)).padding(14.dp)) {
        Text(label, style = AppTypography.body2.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = AppTypography.caption, color = Theme.colors.textSecondary)
    }
}
