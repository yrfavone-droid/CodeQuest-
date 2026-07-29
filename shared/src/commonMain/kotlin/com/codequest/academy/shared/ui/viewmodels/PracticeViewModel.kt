package com.codequest.academy.shared.ui.viewmodels
import com.codequest.academy.shared.data.ProgressRepository
class PracticeViewModel(levelId: String, practiceId: String, repository: ProgressRepository) : CurriculumAssessmentViewModel(levelId, practiceId, "practice", repository)
