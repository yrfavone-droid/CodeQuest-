package com.codequest.academy.shared.ui.viewmodels
import com.codequest.academy.shared.data.ProgressRepository
class AdaptiveReviewViewModel(levelId: String, reviewId: String, repository: ProgressRepository) : CurriculumAssessmentViewModel(levelId, reviewId, "adaptive_review", repository)
