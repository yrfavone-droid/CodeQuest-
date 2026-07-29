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

sealed class TrackBrowserUiState {
    object Loading : TrackBrowserUiState()
    data class Loaded(val paths: List<Path>, val progress: Map<String, Float>) : TrackBrowserUiState()
    object Empty : TrackBrowserUiState()
    data class Error(val message: String) : TrackBrowserUiState()
}

class TrackBrowserViewModel(private val repository: ProgressRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow<TrackBrowserUiState>(TrackBrowserUiState.Loading)
    val uiState: StateFlow<TrackBrowserUiState> = _uiState.asStateFlow()

    init { loadPaths() }

    fun loadPaths() {
        viewModelScope.launch {
            _uiState.value = TrackBrowserUiState.Loading
            try {
                val result = withContext(Dispatchers.Default) {
                    val paths = repository.getPaths()
                    require(paths.size == 10) { "Expected 10 paths but found ${paths.size}." }
                    TrackIdentity.values().forEach { track ->
                        require(paths.count { it.track_id == track.id } == 2) { "${track.title} does not contain two paths." }
                    }
                    paths to TrackIdentity.values().associate { it.id to repository.getTrackProgress(it.id) }
                }
                _uiState.value = TrackBrowserUiState.Loaded(result.first, result.second)
            } catch (error: Throwable) {
                println("Track browser load failed:\n${error.stackTraceToString()}")
                _uiState.value = TrackBrowserUiState.Error(error.message ?: "The saved curriculum index is unavailable.")
            }
        }
    }
}
