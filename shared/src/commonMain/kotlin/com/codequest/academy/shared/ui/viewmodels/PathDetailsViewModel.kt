package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.Level
import com.codequest.academy.shared.models.TrackIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class LevelProgress(val level: Level, val progress: Float, val status: String)

sealed class PathDetailsUiState {
    object Loading : PathDetailsUiState()
    data class Loaded(val pathId: String, val pathTitle: String, val track: TrackIdentity, val levels: List<LevelProgress>, val progress: Float) : PathDetailsUiState()
    object Empty : PathDetailsUiState()
    data class Error(val message: String) : PathDetailsUiState()
}

class PathDetailsViewModel(private val pathId: String, private val repository: ProgressRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow<PathDetailsUiState>(PathDetailsUiState.Loading)
    val uiState: StateFlow<PathDetailsUiState> = _uiState.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    init { loadLevels() }

    fun loadLevels() {
        viewModelScope.launch {
            _uiState.value = PathDetailsUiState.Loading
            try {
                val result = withContext(Dispatchers.Default) {
                    val path = repository.getPathById(pathId) ?: error("Path '$pathId' was not found.")
                    val track = TrackIdentity.fromId(path.track_id) ?: error("Track '${path.track_id}' was not found.")
                    val levels = repository.getLevelsForPath(pathId).map { json.decodeFromString<Level>(it.json_data) }
                    if (levels.isEmpty()) return@withContext PathDetailsUiState.Empty
                    var priorComplete = true
                    val levelProgress = levels.map { level ->
                        val states = repository.getLevelNodeStates(level.id)
                        val required = level.timeline_nodes.filter { it.required }
                        val completed = required.count { states[it.id] == "completed" }
                        val complete = required.isNotEmpty() && completed == required.size
                        val progress = if (required.isEmpty()) 0f else completed.toFloat() / required.size
                        val status = when {
                            complete -> "Completed"
                            progress > 0f -> "In Progress"
                            priorComplete -> "Available"
                            else -> "Locked"
                        }
                        priorComplete = complete
                        LevelProgress(level, progress, status)
                    }
                    PathDetailsUiState.Loaded(path.id, path.title, track, levelProgress, repository.getPathProgress(path.id))
                }
                _uiState.value = result
            } catch (error: Throwable) {
                println("Path detail load failed:\n${error.stackTraceToString()}")
                _uiState.value = PathDetailsUiState.Error(error.message ?: "Path details could not be loaded.")
            }
        }
    }
}
