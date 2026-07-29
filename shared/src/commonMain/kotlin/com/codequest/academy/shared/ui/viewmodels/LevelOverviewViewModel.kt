package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed class LevelOverviewUiState {
    object Loading : LevelOverviewUiState()
    data class Loaded(val level: Level, val nodeStates: Map<String, String>) : LevelOverviewUiState()
    data class NotFound(val levelId: String) : LevelOverviewUiState()
    data class Error(val message: String) : LevelOverviewUiState()
}

class LevelOverviewViewModel(private val levelId: String, private val repository: ProgressRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow<LevelOverviewUiState>(LevelOverviewUiState.Loading)
    val uiState: StateFlow<LevelOverviewUiState> = _uiState.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    init { loadLevel() }

    fun loadLevel() {
        viewModelScope.launch {
            _uiState.value = LevelOverviewUiState.Loading
            try {
                val loaded = withContext(Dispatchers.Default) {
                    val dbLevel = repository.getLevelById(levelId) ?: return@withContext null
                    val level = json.decodeFromString<Level>(dbLevel.json_data)
                    LevelOverviewUiState.Loaded(level, repository.getLevelNodeStates(levelId))
                }
                _uiState.value = loaded ?: LevelOverviewUiState.NotFound(levelId)
            } catch (error: Throwable) {
                println("Level overview load failed:\n${error.stackTraceToString()}")
                _uiState.value = LevelOverviewUiState.Error(error.message ?: "The level could not be loaded.")
            }
        }
    }
}
