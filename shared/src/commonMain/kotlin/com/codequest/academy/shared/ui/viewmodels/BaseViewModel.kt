package com.codequest.academy.shared.ui.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class BaseViewModel {
    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    open fun onCleared() {
        viewModelScope.cancel()
    }
}

@androidx.compose.runtime.Composable
fun <T : BaseViewModel> rememberViewModel(vararg keys: Any?, factory: () -> T): T {
    val viewModel = androidx.compose.runtime.remember(*keys) { factory() }
    androidx.compose.runtime.DisposableEffect(viewModel) {
        onDispose {
            viewModel.onCleared()
        }
    }
    return viewModel
}
