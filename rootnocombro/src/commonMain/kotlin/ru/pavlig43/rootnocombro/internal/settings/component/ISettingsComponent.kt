package ru.pavlig43.rootnocombro.internal.settings.component

import kotlinx.coroutines.flow.StateFlow

interface ISettingsComponent {

    val darkMode:StateFlow<Boolean>

    fun toggleDarkMode()
}