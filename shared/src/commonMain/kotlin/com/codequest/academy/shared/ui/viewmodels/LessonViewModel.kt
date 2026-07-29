package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class LessonSection(val type: String, val title: String, val body: String)
data class LessonCheckpoint(val prompt: String, val answer: String)

sealed class LessonUiState {
    object Loading : LessonUiState()
    data class Loaded(
        val title: String,
        val goal: String,
        val estimatedMinutes: Int,
        val sections: List<LessonSection>,
        val checkpoints: List<LessonCheckpoint>,
        val currentSection: Int = 0,
        val isFinished: Boolean = false
    ) : LessonUiState()
    data class NotFound(val message: String) : LessonUiState()
    data class Error(val message: String) : LessonUiState()
}

class LessonViewModel(private val levelId: String, private val lessonId: String, private val repository: ProgressRepository) : BaseViewModel() {
    private val _state = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val state: StateFlow<LessonUiState> = _state
    private val json = Json { ignoreUnknownKeys = true }

    init { loadLesson() }

    fun loadLesson() {
        viewModelScope.launch {
            _state.value = LessonUiState.Loading
            try {
                val lesson = withContext(Dispatchers.Default) {
                    val row = repository.getLevelById(levelId) ?: return@withContext null
                    val level = json.decodeFromString<Level>(row.json_data)
                    level.lessons.mapNotNull { it as? JsonObject }.firstOrNull { it.string("id") == lessonId }
                }
                if (lesson == null) {
                    _state.value = LessonUiState.NotFound("Lesson '$lessonId' was not found in this level.")
                    return@launch
                }
                val sections = buildList {
                    (lesson["content_blocks"] as? JsonArray)?.mapNotNull { it as? JsonObject }?.forEach { block ->
                        add(LessonSection(block.string("type") ?: "explanation", block.string("title") ?: "Concept", block.string("body") ?: ""))
                    }
                    listSection(lesson, "constraints", "Constraints")?.let(::add)
                    listSection(lesson, "invariants", "Invariants")?.let(::add)
                    listSection(lesson, "step_by_step_reasoning", "Step-by-step reasoning")?.let(::add)
                    (lesson["worked_examples"] as? JsonArray)?.mapNotNull { it as? JsonObject }?.forEachIndexed { index, example ->
                        val reasoning = (example["reasoning"] as? JsonArray)?.joinToString("\n") { "• ${it.jsonPrimitive.content}" }.orEmpty()
                        add(LessonSection("worked_example", example.string("title") ?: "Worked Example ${index + 1}", listOfNotNull(example.string("scenario"), example.string("input"), reasoning, example.string("output")).joinToString("\n\n")))
                    }
                    listSection(lesson, "common_mistakes", "Common mistakes")?.let(::add)
                    listSection(lesson, "edge_cases", "Edge cases")?.let(::add)
                    listSection(lesson, "debugging_strategy", "Debugging strategy")?.let(::add)
                    lesson.string("summary")?.let { add(LessonSection("summary", "Lesson summary", it)) }
                }
                val checkpoints = (lesson["checkpoint_questions"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map {
                    LessonCheckpoint(it.string("prompt") ?: "Checkpoint", it.string("answer") ?: "")
                }
                _state.value = LessonUiState.Loaded(
                    title = lesson.string("title") ?: "Lesson",
                    goal = lesson.string("goal") ?: lesson.string("learning_objective") ?: "Build and verify the structural model.",
                    estimatedMinutes = lesson["estimated_minutes"]?.jsonPrimitive?.intOrNull ?: 30,
                    sections = sections,
                    checkpoints = checkpoints
                )
            } catch (error: Throwable) {
                println("Lesson load failed:\n${error.stackTraceToString()}")
                _state.value = LessonUiState.Error(error.message ?: "The lesson could not be loaded.")
            }
        }
    }

    fun nextSection() {
        val current = _state.value as? LessonUiState.Loaded ?: return
        if (current.currentSection < current.sections.lastIndex) _state.value = current.copy(currentSection = current.currentSection + 1)
    }

    fun previousSection() {
        val current = _state.value as? LessonUiState.Loaded ?: return
        if (current.currentSection > 0) _state.value = current.copy(currentSection = current.currentSection - 1)
    }

    fun finishLesson() {
        val current = _state.value as? LessonUiState.Loaded ?: return
        _state.value = current.copy(isFinished = true)
        viewModelScope.launch { withContext(Dispatchers.Default) { repository.getUserId()?.let { repository.updateNodeState(it, lessonId, "completed", current.title) } } }
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun listSection(source: JsonObject, key: String, title: String): LessonSection? {
        val values = (source[key] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()
        return values.takeIf { it.isNotEmpty() }?.let { LessonSection(key, title, it.joinToString("\n") { value -> "• $value" }) }
    }
}
