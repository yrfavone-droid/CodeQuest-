package com.codequest.academy.shared.update

import kotlinx.coroutines.flow.StateFlow

actual object UpdateController {
    actual val state: StateFlow<UpdateUiState> = AutoUpdateManager.uiState
    actual fun checkForUpdates() = AutoUpdateManager.requestCheck()
    actual fun installUpdate() = AutoUpdateManager.requestInstall()
    actual fun cancel() = AutoUpdateManager.cancelUpdate()
}
