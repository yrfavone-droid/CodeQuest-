package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class AssessmentOption(val id: String, val text: String)
data class AssessmentQuestion(
    val id: String,
    val type: String,
    val prompt: String,
    val options: List<AssessmentOption>,
    val correctAnswers: Set<String>,
    val explanation: String,
    val hints: List<String>
)

fun quizPassed(score: Int, total: Int, passingPercent: Int = 75): Boolean =
    total > 0 && score * 100 / total >= passingPercent

sealed class AssessmentUiState {
    object Loading : AssessmentUiState()
    data class Active(
        val title: String,
        val estimatedMinutes: Int,
        val questions: List<AssessmentQuestion>,
        val index: Int = 0,
        val selected: Set<String> = emptySet(),
        val freeText: String = "",
        val graded: Boolean = false,
        val correct: Boolean = false,
        val hintsShown: Int = 0,
        val score: Int = 0,
        val answers: Map<String, String> = emptyMap()
    ) : AssessmentUiState()
    data class Completed(val title: String, val score: Int, val total: Int, val passed: Boolean, val passingPercent: Int) : AssessmentUiState()
    data class NotFound(val message: String) : AssessmentUiState()
    data class Error(val message: String) : AssessmentUiState()
}

open class CurriculumAssessmentViewModel(
    private val levelId: String,
    private val nodeId: String,
    private val nodeType: String,
    private val repository: ProgressRepository
) : BaseViewModel() {
    private val _state = MutableStateFlow<AssessmentUiState>(AssessmentUiState.Loading)
    val state: StateFlow<AssessmentUiState> = _state
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = AssessmentUiState.Loading
            try {
                val node = withContext(Dispatchers.Default) {
                    val row = repository.getLevelById(levelId) ?: return@withContext null
                    val level = json.decodeFromString<Level>(row.json_data)
                    findNode(level)
                }
                if (node == null || node.string("id") != nodeId) {
                    _state.value = AssessmentUiState.NotFound("$nodeType '$nodeId' was not found in this level.")
                    return@launch
                }
                val rawQuestions = ((node["questions"] ?: node["activities"]) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                val questions = rawQuestions.mapIndexed { index, question -> parseQuestion(question, index) }
                if (questions.isEmpty()) {
                    _state.value = AssessmentUiState.Error("This assessment contains no gradable activities.")
                    return@launch
                }
                _state.value = AssessmentUiState.Active(
                    title = node.string("title") ?: nodeType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    estimatedMinutes = node["estimated_minutes"]?.jsonPrimitive?.intOrNull ?: 15,
                    questions = questions
                )
            } catch (error: Throwable) {
                println("Assessment '$nodeId' load failed:\n${error.stackTraceToString()}")
                _state.value = AssessmentUiState.Error(error.message ?: "The assessment could not be loaded.")
            }
        }
    }

    fun selectOption(optionId: String) {
        val current = _state.value as? AssessmentUiState.Active ?: return
        if (current.graded) return
        val question = current.questions[current.index]
        val multi = question.correctAnswers.size > 1
        val selected = if (multi) current.selected.toMutableSet().also { if (!it.add(optionId)) it.remove(optionId) } else setOf(optionId)
        _state.value = current.copy(selected = selected)
    }

    fun updateFreeText(value: String) {
        val current = _state.value as? AssessmentUiState.Active ?: return
        if (!current.graded) _state.value = current.copy(freeText = value)
    }

    fun showHint() {
        val current = _state.value as? AssessmentUiState.Active ?: return
        val maximum = current.questions[current.index].hints.size.coerceAtMost(3)
        _state.value = current.copy(hintsShown = (current.hintsShown + 1).coerceAtMost(maximum))
    }

    fun checkAnswer() {
        val current = _state.value as? AssessmentUiState.Active ?: return
        if (current.graded) return
        val question = current.questions[current.index]
        val response = if (question.options.isNotEmpty()) current.selected else setOf(current.freeText.trim())
        if (response.all { it.isBlank() }) return
        val normalizedResponse = response.map { it.trim().lowercase() }.toSet()
        val normalizedCorrect = question.correctAnswers.map { it.trim().lowercase() }.toSet()
        val correct = normalizedCorrect.isNotEmpty() && normalizedResponse == normalizedCorrect
        val answerText = response.joinToString(" | ")
        _state.value = current.copy(
            graded = true,
            correct = correct,
            score = current.score + if (correct) 1 else 0,
            answers = current.answers + (question.id to answerText)
        )
    }

    fun retryQuestion() {
        val current = _state.value as? AssessmentUiState.Active ?: return
        if (current.graded && !current.correct) _state.value = current.copy(selected = emptySet(), freeText = "", graded = false)
    }

    fun continueAfterFeedback() {
        val current = _state.value as? AssessmentUiState.Active ?: return
        if (!current.graded) return
        if (current.index < current.questions.lastIndex) {
            _state.value = current.copy(index = current.index + 1, selected = emptySet(), freeText = "", graded = false, correct = false, hintsShown = 0)
            return
        }
        val total = current.questions.size
        val passing = if (nodeType == "final_quiz") 75 else 0
        val percent = if (total == 0) 0 else current.score * 100 / total
        val passed = if (nodeType == "final_quiz") quizPassed(current.score, total, passing) else true
        _state.value = AssessmentUiState.Completed(current.title, current.score, total, passed, passing)
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                repository.saveAssessmentAttempt(nodeId, current.score, total, current.answers.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":\"${it.value.replace("\"", "\\\"")}\"" })
                repository.getUserId()?.let { repository.updateNodeState(it, nodeId, if (passed) "completed" else "failed", current.title) }
            }
        }
    }

    private fun findNode(level: Level): JsonObject? {
        fun objects(items: List<JsonElement>) = items.mapNotNull { it as? JsonObject }
        return when (nodeType) {
            "diagnostic" -> level.diagnostic?.let { json.encodeToJsonElement(com.codequest.academy.shared.models.Diagnostic.serializer(), it) as JsonObject }
            "practice" -> objects(level.lessons).mapNotNull { it["practice_set"] as? JsonObject }.firstOrNull { it.string("id") == nodeId }
            "challenge" -> objects(level.lessons).mapNotNull { it["challenge"] as? JsonObject }.firstOrNull { it.string("id") == nodeId }
            "mixed_review" -> objects(level.mixed_reviews).firstOrNull { it.string("id") == nodeId }
            "adaptive_review" -> level.adaptive_review as? JsonObject
            "final_quiz" -> level.final_quiz as? JsonObject
            else -> null
        }
    }

    private fun parseQuestion(question: JsonObject, index: Int): AssessmentQuestion {
        val options = (question["options"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map {
            AssessmentOption(it.string("id") ?: (it.string("text") ?: "Option"), it.string("text") ?: it.string("label") ?: "Option")
        }
        val explicit = flattenAnswers(question["correct_answer"])
        val marked = (question["options"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.filter { it["is_correct"]?.jsonPrimitive?.booleanOrNull == true }.mapNotNull { it.string("id") }.toSet()
        return AssessmentQuestion(
            id = question.string("id") ?: "$nodeId-Q${index + 1}",
            type = question.string("type") ?: "response",
            prompt = question.string("prompt") ?: question.string("title") ?: "Complete this activity.",
            options = options,
            correctAnswers = if (explicit.isNotEmpty()) explicit else marked,
            explanation = question.string("explanation") ?: "Compare your response with the governing rule and trace the first differing state.",
            hints = (question["hints"] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
        )
    }

    private fun flattenAnswers(element: JsonElement?): Set<String> = when (element) {
        is JsonPrimitive -> setOf(element.content)
        is JsonArray -> element.flatMap { flattenAnswers(it) }.toSet()
        is JsonObject -> element.values.flatMap { flattenAnswers(it) }.toSet()
        else -> emptySet()
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
