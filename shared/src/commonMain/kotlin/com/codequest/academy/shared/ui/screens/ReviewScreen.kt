package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.navigation.Screen
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme

@Composable fun ReviewScreen(navigation: Navigation, repository: ProgressRepository) {
    val failures = repository.getRecentActivity(20).filter { it.eventType == "failed" }
    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(40.dp)) {
        Text("Strengthen Your Skills", style = DisplayStyle); Text("Recommendations come only from saved incorrect attempts.", style = AppTypography.body1, color = Theme.colors.textSecondary)
        Spacer(Modifier.height(28.dp))
        if (failures.isEmpty()) { InfoCard("You are caught up", "No weak skills identified yet. Complete practices and quizzes to receive recommendations."); Spacer(Modifier.height(16.dp)); PrimaryButton("Continue Learning", onClick = { navigation.navigateTo(Screen.Dashboard) }) }
        else failures.forEach { InfoCard(it.title, "Recommended because the latest saved attempt did not pass.", Modifier.padding(bottom = 12.dp)) }
    }
}
