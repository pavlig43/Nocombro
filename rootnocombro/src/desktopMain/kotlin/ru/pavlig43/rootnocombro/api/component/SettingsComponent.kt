package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.datastore.SettingsRepository

class SettingsComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository,
) :  ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    private val _isSettingsOpened = MutableStateFlow(false)
    val isSettingsOpened = _isSettingsOpened.asStateFlow()

    val darkMode: StateFlow<Boolean> = settingsRepository.isDarkMode.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    fun toggleDarkMode() {
        coroutineScope.launch {
            val currentDarkMode = darkMode.value
            settingsRepository.toggleDarkMode(!currentDarkMode)
        }
    }

    fun openSettings() {
        _isSettingsOpened.value = true
    }

    fun closeSettings() {
        _isSettingsOpened.value = false
    }
}
