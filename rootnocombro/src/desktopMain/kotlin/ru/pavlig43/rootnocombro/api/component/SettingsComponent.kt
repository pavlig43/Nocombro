package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.datastore.SettingsRepository
import ru.pavlig43.rootnocombro.internal.settings.LocalFilesMaintenanceRepository
import java.awt.Desktop
import java.io.File

class SettingsComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository,
    private val localFilesMaintenanceRepository: LocalFilesMaintenanceRepository,
) :  ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    private val _isSettingsOpened = MutableStateFlow(false)
    val isSettingsOpened = _isSettingsOpened.asStateFlow()

    val darkMode: StateFlow<Boolean> = settingsRepository.isDarkMode.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    private val _orphanFilesState = MutableStateFlow<SettingsOrphanFilesLoadState>(
        SettingsOrphanFilesLoadState.Loading
    )
    val orphanFilesState = _orphanFilesState.asStateFlow()
    private val _orphanFilesActionError = MutableStateFlow<String?>(null)
    val orphanFilesActionError = _orphanFilesActionError.asStateFlow()

    fun toggleDarkMode() {
        coroutineScope.launch {
            val currentDarkMode = darkMode.value
            settingsRepository.toggleDarkMode(!currentDarkMode)
        }
    }

    fun openSettings() {
        _isSettingsOpened.value = true
        refreshOrphanFiles()
    }

    fun closeSettings() {
        _isSettingsOpened.value = false
    }

    fun refreshOrphanFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            _orphanFilesState.value = SettingsOrphanFilesLoadState.Loading
            localFilesMaintenanceRepository.getOrphanLocalFiles()
                .onSuccess { files ->
                    _orphanFilesState.value = SettingsOrphanFilesLoadState.Success(files)
                }
                .onFailure { throwable ->
                    _orphanFilesState.value = SettingsOrphanFilesLoadState.Error(
                        throwable.message ?: "Не удалось загрузить orphan-файлы."
                    )
                }
        }
    }

    fun openOrphanFile(path: String) {
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                Desktop.getDesktop().open(File(path))
            }.onFailure { throwable ->
                _orphanFilesActionError.value = throwable.message ?: "Не удалось открыть файл."
            }
        }
    }

    fun deleteOrphanFile(path: String) {
        coroutineScope.launch(Dispatchers.IO) {
            localFilesMaintenanceRepository.deleteLocalFile(path)
                .onSuccess { refreshOrphanFiles() }
                .onFailure { throwable ->
                    _orphanFilesActionError.value = throwable.message ?: "Не удалось удалить файл."
                }
        }
    }

    fun deleteAllOrphanFiles() {
        val currentState = orphanFilesState.value as? SettingsOrphanFilesLoadState.Success ?: return
        coroutineScope.launch(Dispatchers.IO) {
            currentState.files.forEach { orphan ->
                localFilesMaintenanceRepository.deleteLocalFile(orphan.path)
                    .onFailure { throwable ->
                        _orphanFilesActionError.value =
                            throwable.message ?: "Не удалось удалить orphan-файлы."
                        return@launch
                    }
            }
            refreshOrphanFiles()
        }
    }

    fun dismissOrphanFilesActionError() {
        _orphanFilesActionError.value = null
    }
}

sealed interface SettingsOrphanFilesLoadState {
    data object Loading : SettingsOrphanFilesLoadState
    data class Error(val message: String) : SettingsOrphanFilesLoadState
    data class Success(val files: List<LocalOrphanFile>) : SettingsOrphanFilesLoadState
}
