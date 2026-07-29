package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.database.Path
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.TrackIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class TrackDetailsUiState {
    object Loading : TrackDetailsUiState()
    data class Loaded(val track: TrackIdentity, val paths: List<Path>, val progress: Float, val pathProgress: Map<String, Float>) : TrackDetailsUiState()
    data class Error(val message: String) : TrackDetailsUiState()
}

class TrackDetailsViewModel(private val trackId: String, private val repository: ProgressRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow<TrackDetailsUiState>(TrackDetailsUiState.Loading)
    val uiState: StateFlow<TrackDetailsUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = TrackDetailsUiState.Loading
            try {
                val loaded = withContext(Dispatchers.Default) {
                    val track = TrackIdentity.fromId(trackId) ?: error("Track '$trackId' was not found.")
                    val paths = repository.getPaths().filter { it.track_id == track.id }
                    require(paths.size == 2) { "${track.title} curriculum is incomplete." }
                    TrackDetailsUiState.Loaded(track, paths, repository.getTrackProgress(track.id), paths.associate { it.id to repository.getPathProgress(it.id) })
                }
                _uiState.value = loaded
            } catch (error: Throwable) {
                println("Track detail load failed:\n${error.stackTraceToString()}")
                _uiState.value = TrackDetailsUiState.Error(error.message ?: "Track details could not be loaded.")
            }
        }
    }
}
