package com.codequest.academy.shared.learning

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LearningHubCurriculum(
    val format: String,
    val version: String,
    val brand: String,
    val language: String,
    val offline: Boolean,
    @SerialName("generated_on") val generatedOn: String? = null,
    @SerialName("section_count") val sectionCount: Int,
    @SerialName("lesson_count") val lessonCount: Int,
    @SerialName("problem_count") val problemCount: Int,
    val sections: List<LearningHubSection>
)

@Serializable
data class LearningHubSection(
    val id: String,
    val position: Int,
    val title: String,
    val level: String,
    val description: String,
    @SerialName("lesson_count") val lessonCount: Int,
    val lessons: List<LearningHubLesson>
)

@Serializable
data class LearningHubLesson(
    val id: String,
    @SerialName("section_id") val sectionId: String,
    val position: Int,
    val title: String,
    val objective: String,
    val principle: String,
    @SerialName("worked_example") val workedExample: String,
    val lab: String,
    @SerialName("content_path") val contentPath: String,
    @SerialName("answer_path") val answerPath: String,
    @SerialName("problem_start_id") val problemStartId: String,
    @SerialName("problem_end_id") val problemEndId: String,
    @SerialName("problem_count") val problemCount: Int
)

@Serializable
data class LearningHubProblem(
    val id: String,
    @SerialName("section_id") val sectionId: String,
    @SerialName("lesson_id") val lessonId: String,
    val sequence: Int? = null,
    val difficulty: String? = null,
    val mode: String? = null,
    val context: String? = null,
    val prompt: String,
    @SerialName("answer_type") val answerType: String,
    val answer: JsonElement,
    val explanation: String,
    val options: List<String> = emptyList(),
    @SerialName("estimated_minutes") val estimatedMinutes: Int? = null
)

data class LearningHubContentState(
    val loading: Boolean = true,
    val curriculum: LearningHubCurriculum? = null,
    val packagePath: String? = null,
    val error: String? = null
)

/** Platform bridge for the verified, read-only Learning Hub package. */
expect object LearningHubContent {
    val state: StateFlow<LearningHubContentState>
    fun initialize(databasePath: String)
    /** Installs a user-provided update archive; false means the active version was left untouched. */
    fun installPackage(packagePath: String, databasePath: String): Boolean
    fun lessonMarkdown(lesson: LearningHubLesson): String
    fun answerKey(lesson: LearningHubLesson): String
    fun firstProblems(lesson: LearningHubLesson, limit: Int = 10): List<LearningHubProblem>
    fun sectionPdfPath(section: LearningHubSection): String?
    fun openSectionPdf(section: LearningHubSection): Boolean
    /** Saves to the user's Downloads folder when a plain filename is supplied. */
    fun saveSectionPdf(section: LearningHubSection, destinationPath: String): Boolean
}
