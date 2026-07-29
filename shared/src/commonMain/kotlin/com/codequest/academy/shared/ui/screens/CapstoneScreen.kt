package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme

@Composable fun LockedCapstoneScreen(title: String, requirement: String, navigation: Navigation) {
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(40.dp)) {
        SecondaryButton("Back", onClick = { navigation.pop() }); Spacer(Modifier.height(24.dp)); Text(title, style = DisplayStyle)
        Spacer(Modifier.height(10.dp)); Text("Professional integrated project workspace", style = AppTypography.body1, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(28.dp)); InfoCard("Locked prerequisite", requirement); Spacer(Modifier.height(18.dp)); SecondaryButton("Go to Learning", onClick = { navigation.navigateTo(Screen.TrackBrowser) })
    }
}
