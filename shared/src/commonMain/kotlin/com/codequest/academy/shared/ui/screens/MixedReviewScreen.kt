package com.codequest.academy.shared.ui.screens
import androidx.compose.runtime.Composable
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.MixedReviewViewModel
@Composable fun MixedReviewScreen(navigation: Navigation, viewModel: MixedReviewViewModel, levelId: String) = AssessmentScreen(navigation, viewModel, levelId, "Mixed Review", Theme.colors.information)
