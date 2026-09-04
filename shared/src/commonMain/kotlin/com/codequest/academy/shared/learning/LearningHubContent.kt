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
    val lessons: List<LearningHubLesson>,
    @SerialName("pdf_path") val pdfPath: String? = null,
    @SerialName("content_version") val contentVersion: String? = null
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
    @SerialName("problem_count") val problemCount: Int,
    @SerialName("summary_path") val summaryPath: String? = null,
    @SerialName("pdf_path") val pdfPath: String? = null,
    @SerialName("article_words") val articleWords: Int? = null,
    @SerialName("unit_count") val unitCount: Int = 0,
    @SerialName("practice_count") val practiceCount: Int = 0,
    @SerialName("quiz_count") val quizCount: Int = 0,
    @SerialName("content_version") val contentVersion: String? = null
)

@Serializable
data class LearningHubArticleBlock(
    val type: String,
    val title: String? = null,
    val section: String? = null,
    val principle: String? = null,
    val level: Int? = null,
    val text: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val path: String? = null
)

data class LearningHubSearchResult(
    val lessonId: String,
    val sectionId: String,
    val sectionTitle: String,
    val lessonTitle: String,
    val excerpt: String
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

@Serializable
data class Section1LessonMeta(
    val id: String,
    val position: Int,
    val title: String,
    val subtitle: String,
    @SerialName("big_question") val bigQuestion: String,
    @SerialName("article_words") val articleWords: Int,
    @SerialName("unit_count") val unitCount: Int,
    @SerialName("practice_count") val practiceCount: Int,
    @SerialName("quiz_count") val quizCount: Int,
    @SerialName("article_path") val articlePath: String,
    @SerialName("notes_path") val notesPath: String,
    @SerialName("quick_sheet_path") val quickSheetPath: String,
    @SerialName("answers_path") val answersPath: String,
    @SerialName("pdf_path") val pdfPath: String
)

@Serializable
data class Section1ArticleBlock(
    val type: String,
    val title: String? = null,
    val subtitle: String? = null,
    @SerialName("big_question") val bigQuestion: String? = null,
    val items: JsonElement? = null,
    val level: Int? = null,
    val text: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val path: String? = null
)

@Serializable
data class Section1GlossaryEntry(val term: String, val definition: String)

data class LearningHubContentState(
    val loading: Boolean = true,
    val curriculum: LearningHubCurriculum? = null,
    val packagePath: String? = null,
    val error: String? = null,
    val curriculumVersion: String? = null,
    val updateMessage: String? = null,
    val canRollback: Boolean = false
)

/** Platform bridge for the verified, read-only Learning Hub package. */
expect object LearningHubContent {
    val state: StateFlow<LearningHubContentState>
    fun initialize(databasePath: String)
    /** Installs a user-provided update archive; false means the active version was left untouched. */
    fun installPackage(packagePath: String, databasePath: String): Boolean
    fun lessonMarkdown(lesson: LearningHubLesson): String
    fun lessonArticleBlocks(lesson: LearningHubLesson): List<LearningHubArticleBlock>
    fun lessonReview(lesson: LearningHubLesson): String
    fun answerKey(lesson: LearningHubLesson): String
    fun firstProblems(lesson: LearningHubLesson, limit: Int = 10): List<LearningHubProblem>
    fun allPractice(lesson: LearningHubLesson): List<LearningHubProblem>
    fun lessonQuiz(lesson: LearningHubLesson, limit: Int = 20): List<LearningHubProblem>
    fun lessonPdfPath(lesson: LearningHubLesson): String?
    fun openLessonPdf(lesson: LearningHubLesson): Boolean
    fun chooseAndSaveLessonPdf(lesson: LearningHubLesson): Boolean
    fun sectionPdfPath(section: LearningHubSection): String?
    fun openSectionPdf(section: LearningHubSection): Boolean
    /** Saves to the user's Downloads folder when a plain filename is supplied. */
    fun saveSectionPdf(section: LearningHubSection, destinationPath: String): Boolean
    fun chooseAndSaveSectionPdf(section: LearningHubSection): Boolean
    fun searchLessons(query: String, limit: Int = 30): List<LearningHubSearchResult>
    /** Opens a local file picker, validates the selected package, and activates it atomically. */
    fun selectAndInstallCurriculum(): Boolean
    fun rollbackCurriculum(): Boolean
    fun section1Lesson(lessonId: String): Section1LessonMeta?
    fun section1ArticleBlocks(lessonId: String): List<Section1ArticleBlock>
    fun section1Review(lessonId: String): String
    fun section1Problems(lessonId: String, quiz: Boolean = false, limit: Int = 10): List<LearningHubProblem>
    fun section1Glossary(): List<Section1GlossaryEntry>
    fun section1Note(lessonId: String): String
    fun saveSection1Note(lessonId: String, note: String)
    fun openSection1Pdf(lessonId: String): Boolean
    fun saveSection1Pdf(lessonId: String, destinationPath: String): Boolean
}
