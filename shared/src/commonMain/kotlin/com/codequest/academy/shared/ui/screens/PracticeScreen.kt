package com.codequest.academy.shared.ui.screens
import androidx.compose.runtime.Composable
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.PracticeViewModel
@Composable fun PracticeScreen(navigation: Navigation, viewModel: PracticeViewModel, levelId: String) = AssessmentScreen(navigation, viewModel, levelId, "Practice", Theme.colors.brandPrimary)
