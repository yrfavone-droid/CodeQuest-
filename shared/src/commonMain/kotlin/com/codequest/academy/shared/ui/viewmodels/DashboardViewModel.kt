package com.codequest.academy.shared.ui.viewmodels

import com.codequest.academy.shared.data.ActivityRecord
import com.codequest.academy.shared.data.LearningProgressSummary
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.TrackIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val nextLevelId: String? = null,
    val nextNodeId: String? = null,
    val nextNodeTitle: String = "",
    val summary: LearningProgressSummary = LearningProgressSummary(0, 0, 0, 0, 0),
    val trackProgress: Map<String, Float> = emptyMap(),
    val recentActivity: List<ActivityRecord> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(private val repository: ProgressRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init { loadDashboardData() }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.Default) {
                    val next = repository.getNextAvailableNode()
                    DashboardState(
                        isLoading = false,
                        userName = repository.getProfileName(),
                        nextLevelId = next?.first,
                        nextNodeId = next?.second,
                        nextNodeTitle = next?.second?.replace('-', ' ') ?: "",
                        summary = repository.getProgressSummary(),
                        trackProgress = TrackIdentity.values().associate { it.id to repository.getTrackProgress(it.id) },
                        recentActivity = repository.getRecentActivity()
                    )
                }
                _state.value = loaded
            } catch (error: Throwable) {
                println("Dashboard load failed:\n${error.stackTraceToString()}")
                _state.value = DashboardState(isLoading = false, userName = "Learner", error = error.message ?: "Dashboard data could not be loaded.")
            }
        }
    }
}
