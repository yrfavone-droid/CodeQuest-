package com.codequest.academy.shared.ui.viewmodels
import com.codequest.academy.shared.data.ProgressRepository
class DiagnosticViewModel(levelId: String, diagnosticId: String, repository: ProgressRepository) : CurriculumAssessmentViewModel(levelId, diagnosticId, "diagnostic", repository)
