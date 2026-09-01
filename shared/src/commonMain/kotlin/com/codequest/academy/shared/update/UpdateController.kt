package com.codequest.academy.shared.update

import kotlinx.coroutines.flow.StateFlow

data class UpdateUiInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseDate: String,
    val releaseNotes: String,
    val updateType: String,
    val downloadSizeBytes: Long
)

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    object Offline : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class Available(val info: UpdateUiInfo) : UpdateUiState()
    object Downloading : UpdateUiState()
    object Verifying : UpdateUiState()
    object ReadyToRestart : UpdateUiState()
    object Installing : UpdateUiState()
    data class Failed(val message: String) : UpdateUiState()
}

/** Platform bridge for the secure desktop updater. Web/other targets can provide their own implementation later. */
expect object UpdateController {
    val state: StateFlow<UpdateUiState>
    fun checkForUpdates()
    fun installUpdate()
    fun cancel()
}
