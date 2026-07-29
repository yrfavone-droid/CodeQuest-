package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class ProjectMilestone(val number: Int, val title: String, val deliverable: String)
sealed class ProjectUiState {
    object Loading : ProjectUiState()
    data class Loaded(
        val title: String,
        val brief: String,
        val problem: String,
        val milestones: List<ProjectMilestone>,
        val mandatoryFeatures: List<String>,
        val rubric: List<String>,
        val notes: String,
        val completedMilestones: Set<Int>,
        val savedAt: Long? = null,
        val submitted: Boolean = false
    ) : ProjectUiState()
    data class Locked(val message: String) : ProjectUiState()
    data class NotFound(val message: String) : ProjectUiState()
    data class Error(val message: String) : ProjectUiState()
}

class ProjectViewModel(private val levelId: String, private val projectId: String, private val repository: ProgressRepository) : BaseViewModel() {
    private val _state = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val state: StateFlow<ProjectUiState> = _state
    private val json = Json { ignoreUnknownKeys = true }

    init { loadProject() }

    fun loadProject() {
        viewModelScope.launch {
            _state.value = ProjectUiState.Loading
            try {
                val result = withContext(Dispatchers.Default) {
                    val row = repository.getLevelById(levelId) ?: return@withContext ProjectUiState.NotFound("Level '$levelId' was not found.")
                    val level = json.decodeFromString<Level>(row.json_data)
                    val node = level.project as? JsonObject ?: return@withContext ProjectUiState.NotFound("This level has no project.")
                    if (node.string("id") != projectId) return@withContext ProjectUiState.NotFound("Project '$projectId' was not found in this level.")
                    if (repository.getLevelNodeStates(levelId)[projectId] == "locked") return@withContext ProjectUiState.Locked("Pass the Final Quiz with at least 75% to unlock this project.")
                    val draft = repository.getProjectDraft(projectId)
                    val pieces = draft?.notes?.split("\n---\n", limit = 2).orEmpty()
                    val completed = pieces.firstOrNull()?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                    val notes = pieces.getOrNull(1).orEmpty()
                    ProjectUiState.Loaded(
                        title = node.string("title") ?: "Level Project",
                        brief = node.string("project_brief") ?: "Build the required artifact and provide structural evidence.",
                        problem = node.string("problem_definition") ?: node.string("prompt") ?: "Complete the project requirements.",
                        milestones = (node["milestones"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.mapIndexed { index, item -> ProjectMilestone(item["number"]?.jsonPrimitive?.intOrNull ?: index + 1, item.string("title") ?: "Milestone ${index + 1}", item.string("deliverable") ?: "Record evidence") },
                        mandatoryFeatures = strings(node["mandatory_features"]),
                        rubric = (node["rubric"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map { "${it.string("category") ?: "Criterion"} · ${it["points"]?.jsonPrimitive?.intOrNull ?: 0} points" },
                        notes = notes,
                        completedMilestones = completed,
                        savedAt = draft?.updatedAt,
                        submitted = draft?.submitted == true
                    )
                }
                _state.value = result
            } catch (error: Throwable) {
                println("Project '$projectId' load failed:\n${error.stackTraceToString()}")
                _state.value = ProjectUiState.Error(error.message ?: "The project workspace could not be loaded.")
            }
        }
    }

    fun updateNotes(notes: String) { val current = _state.value as? ProjectUiState.Loaded ?: return; _state.value = current.copy(notes = notes) }
    fun toggleMilestone(number: Int) { val current = _state.value as? ProjectUiState.Loaded ?: return; val updated = current.completedMilestones.toMutableSet().also { if (!it.add(number)) it.remove(number) }; _state.value = current.copy(completedMilestones = updated) }
    fun saveDraft() { val current = _state.value as? ProjectUiState.Loaded ?: return; persist(current, false) }
    fun submitProject() { val current = _state.value as? ProjectUiState.Loaded ?: return; _state.value = current.copy(submitted = true); persist(current, true) }

    private fun persist(current: ProjectUiState.Loaded, submitted: Boolean) {
        viewModelScope.launch { withContext(Dispatchers.Default) {
            val payload = current.completedMilestones.sorted().joinToString(",") + "\n---\n" + current.notes
            repository.saveProjectDraft(projectId, payload, submitted)
            if (submitted) repository.getUserId()?.let { repository.updateNodeState(it, projectId, "completed", current.title) }
        } }
    }
    private fun strings(element: JsonElement?): List<String> = (element as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
