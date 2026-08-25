package com.codequest.academy.shared.update

import com.codequest.academy.shared.logging.AppLogger
import com.codequest.academy.shared.network.ApiClient
import com.codequest.academy.shared.network.UpdateCheckResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateCheckResponse) : UpdateState()
    object UpToDate : UpdateState()
    data class Failed(val info: UpdateCheckResponse?, val error: String) : UpdateState()
}

object AutoUpdateManager {
    /** The packaging task injects this value; the fallback keeps IDE runs deterministic. */
    val currentVersion: String = System.getProperty("codequest.version", "1.2.0")
    /** Automatic installation remains off until a signed platform updater is introduced. */
    var autoUpdateEnabled: Boolean = false
    var checkIntervalMs: Long = 2 * 60 * 60 * 1000L // 2 hours

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var periodicJob: Job? = null

    fun startPeriodicChecks() {
        periodicJob?.cancel()
        if (!autoUpdateEnabled) return
        periodicJob = scope.launch {
            while (true) {
                if (autoUpdateEnabled) {
                    checkForUpdates(manual = false)
                }
                delay(checkIntervalMs)
            }
        }
    }

    suspend fun checkForUpdates(manual: Boolean = false): UpdateCheckResponse? {
        AppLogger.info("Checking for updates (current version: $currentVersion, manual: $manual)...")
        _updateState.value = UpdateState.Checking

        val osName = System.getProperty("os.name").lowercase()
        val osKey = when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "macos"
            else -> "linux"
        }

        val result = ApiClient.checkUpdates(currentVersion, osKey)
        return result.fold(
            onSuccess = { response ->
                if (response.updateAvailable && isNewerVersion(response.latestVersion, currentVersion)) {
                    AppLogger.info("Update available: v${response.latestVersion} (${response.downloadUrl})")
                    _updateState.value = UpdateState.UpdateAvailable(response)
                    ApiClient.logUpdateStatus("local_user", currentVersion, response.latestVersion, "available")

                    response
                } else {
                    AppLogger.info("CodeQuest Academy is up to date (v$currentVersion).")
                    _updateState.value = UpdateState.UpToDate
                    null
                }
            },
            onFailure = { err ->
                val errorMsg = "Update check failed: ${err.message}"
                AppLogger.warn(errorMsg)
                _updateState.value = UpdateState.Failed(null, errorMsg)
                null
            }
        )
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

}
