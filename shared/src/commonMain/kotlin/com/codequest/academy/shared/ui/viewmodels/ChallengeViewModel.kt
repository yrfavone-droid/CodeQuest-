package com.codequest.academy.shared.ui.viewmodels
import com.codequest.academy.shared.data.ProgressRepository
class ChallengeViewModel(levelId: String, challengeId: String, repository: ProgressRepository) : CurriculumAssessmentViewModel(levelId, challengeId, "challenge", repository)
