package ru.pavlig43.rootnocombro.internal.settings.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.datastore.SettingsRepository

/**
 * TODO сделать из dataStore
 */
internal class SettingsComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository
) : ISettingsComponent, ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    override val darkMode: StateFlow<Boolean> = settingsRepository.isDarkMode.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    override fun toggleDarkMode() {
        coroutineScope.launch {
            val currentDarkMode = darkMode.value
            settingsRepository.toggleDarkMode(!currentDarkMode)
        }

    }
}

