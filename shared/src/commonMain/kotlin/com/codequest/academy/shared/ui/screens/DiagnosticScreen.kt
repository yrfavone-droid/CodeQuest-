package com.codequest.academy.shared.ui.screens
import androidx.compose.runtime.Composable
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.Theme
import com.codequest.academy.shared.ui.viewmodels.DiagnosticViewModel
@Composable fun DiagnosticScreen(navigation: Navigation, viewModel: DiagnosticViewModel, levelId: String) = AssessmentScreen(navigation, viewModel, levelId, "Diagnostic", Theme.colors.information)
