package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class DocumentSection(val title: String, val body: String)
sealed class DocumentNodeState {
    object Loading : DocumentNodeState()
    data class Loaded(val title: String, val sections: List<DocumentSection>, val responses: List<String> = emptyList(), val completed: Boolean = false) : DocumentNodeState()
    data class NotFound(val message: String) : DocumentNodeState()
    data class Error(val message: String) : DocumentNodeState()
}

class DocumentNodeViewModel(
    private val levelId: String,
    private val nodeId: String,
    private val nodeType: String,
    private val repository: ProgressRepository
) : BaseViewModel() {
    private val _state = MutableStateFlow<DocumentNodeState>(DocumentNodeState.Loading)
    val state: StateFlow<DocumentNodeState> = _state
    private val json = Json { ignoreUnknownKeys = true }

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = DocumentNodeState.Loading
            try {
                val node = withContext(Dispatchers.Default) {
                    val row = repository.getLevelById(levelId) ?: return@withContext null
                    val level = json.decodeFromString<Level>(row.json_data)
                    when (nodeType) {
                        "cheat_sheet" -> level.cheat_sheet as? JsonObject
                        "project_reflection" -> level.project_reflection as? JsonObject
                        "optional_mastery_challenge" -> level.optional_mastery_challenge as? JsonObject
                        else -> null
                    }
                }
                if (node == null || node.string("id") != nodeId) {
                    _state.value = DocumentNodeState.NotFound("$nodeType '$nodeId' was not found in this level.")
                    return@launch
                }
                val excluded = setOf("id", "title", "estimated_minutes", "unlock_rule", "completion_rule", "optional")
                val sections = node.entries.filter { it.key !in excluded && it.key != "prompts" }.mapNotNull { (key, value) ->
                    readable(value).takeIf { it.isNotBlank() }?.let { DocumentSection(key.pretty(), it) }
                }
                val prompts = (node["prompts"] as? JsonArray).orEmpty().mapIndexed { index, value ->
                    val obj = value as? JsonObject
                    DocumentSection("Reflection ${index + 1}", obj?.string("prompt") ?: readable(value))
                }
                val saved = withContext(Dispatchers.Default) { repository.getProjectDraft(nodeId)?.notes.orEmpty() }
                val responses = if (prompts.isEmpty()) emptyList() else saved.split("\u001F").let { values -> List(prompts.size) { values.getOrElse(it) { "" } } }
                _state.value = DocumentNodeState.Loaded(node.string("title") ?: nodeType.pretty(), sections + prompts, responses)
            } catch (error: Throwable) {
                println("Document node '$nodeId' failed:\n${error.stackTraceToString()}")
                _state.value = DocumentNodeState.Error(error.message ?: "The learning content could not be loaded.")
            }
        }
    }

    fun updateResponse(index: Int, value: String) {
        val current = _state.value as? DocumentNodeState.Loaded ?: return
        if (current.responses.isEmpty()) return
        val updated = current.responses.toMutableList().also { it[index] = value }
        _state.value = current.copy(responses = updated)
        viewModelScope.launch { withContext(Dispatchers.Default) { repository.saveProjectDraft(nodeId, updated.joinToString("\u001F")) } }
    }

    fun saveDraft() {
        val current = _state.value as? DocumentNodeState.Loaded ?: return
        viewModelScope.launch { withContext(Dispatchers.Default) { repository.saveProjectDraft(nodeId, current.responses.joinToString("\u001F")) } }
    }

    fun complete() {
        val current = _state.value as? DocumentNodeState.Loaded ?: return
        _state.value = current.copy(completed = true)
        viewModelScope.launch { withContext(Dispatchers.Default) {
            repository.saveProjectDraft(nodeId, current.responses.joinToString("\u001F"), submitted = current.responses.isNotEmpty())
            repository.getUserId()?.let { repository.updateNodeState(it, nodeId, "completed", current.title) }
        } }
    }

    private fun readable(element: JsonElement, depth: Int = 0): String = when (element) {
        is JsonPrimitive -> element.content
        is JsonArray -> element.joinToString("\n\n") { value -> if (value is JsonPrimitive) "• ${value.content}" else readable(value, depth + 1) }
        is JsonObject -> element.entries.joinToString("\n") { (key, value) -> "${key.pretty()}: ${readable(value, depth + 1)}" }
    }
    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun String.pretty(): String = replace('_', ' ').replaceFirstChar { it.uppercase() }
}
