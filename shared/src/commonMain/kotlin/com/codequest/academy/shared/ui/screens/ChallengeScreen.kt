package com.codequest.academy.shared.ui.screens
import androidx.compose.runtime.Composable
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.ChallengeViewModel
@Composable fun ChallengeScreen(navigation: Navigation, viewModel: ChallengeViewModel, levelId: String) = AssessmentScreen(navigation, viewModel, levelId, "Challenge", Theme.colors.warning)
