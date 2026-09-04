package com.codequest.academy.shared.ui.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(
    initialRailExpanded: Boolean = true,
    private val persistRailState: (Boolean) -> Unit = {}
) : BaseViewModel() {
    private val _isRailExpanded = MutableStateFlow(initialRailExpanded)
    val isRailExpanded: StateFlow<Boolean> = _isRailExpanded.asStateFlow()

    fun toggleRail() {
        _isRailExpanded.value = !_isRailExpanded.value
        persistRailState(_isRailExpanded.value)
    }
}
