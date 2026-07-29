package com.codequest.academy.shared.ui.viewmodels
import com.codequest.academy.shared.data.ProgressRepository
class FinalQuizViewModel(levelId: String, quizId: String, repository: ProgressRepository) : CurriculumAssessmentViewModel(levelId, quizId, "final_quiz", repository)
