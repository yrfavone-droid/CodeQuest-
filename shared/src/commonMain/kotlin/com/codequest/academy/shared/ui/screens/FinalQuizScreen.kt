package com.codequest.academy.shared.ui.screens
import androidx.compose.runtime.Composable
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.FinalQuizViewModel
@Composable fun FinalQuizScreen(navigation: Navigation, viewModel: FinalQuizViewModel, levelId: String) = AssessmentScreen(navigation, viewModel, levelId, "Final Quiz", Theme.colors.success)
