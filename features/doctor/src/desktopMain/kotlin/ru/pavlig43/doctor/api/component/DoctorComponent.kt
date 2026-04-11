package ru.pavlig43.doctor.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.doctor.api.DoctorDependencies
import ru.pavlig43.doctor.internal.component.DoctorOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorStorageOverviewLoadState
import ru.pavlig43.doctor.internal.component.DoctorTool
import java.awt.Desktop
import java.io.File

class DoctorComponent(
    componentContext: ComponentContext,
    dependencies: DoctorDependencies,
) : ComponentContext by componentContext, MainTabComponent {
    private val coroutineScope = componentCoroutineScope()
    private val localFilesMaintenanceRepository = dependencies.localFilesMaintenanceRepository

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Доктор"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    private val _selectedTool = MutableStateFlow(DoctorTool.StorageOverview)
    val selectedTool = _selectedTool.asStateFlow()

    private val _storageOverviewState = MutableStateFlow<DoctorStorageOverviewLoadState>(
        DoctorStorageOverviewLoadState.Loading
    )
    val storageOverviewState = _storageOverviewState.asStateFlow()

    private val _orphanFilesState = MutableStateFlow<DoctorOrphanFilesLoadState>(
        DoctorOrphanFilesLoadState.Loading
    )
    val orphanFilesState = _orphanFilesState.asStateFlow()

    private val _orphanFilesActionError = MutableStateFlow<String?>(null)
    val orphanFilesActionError = _orphanFilesActionError.asStateFlow()

    init {
        refreshStorageOverview()
        refreshOrphanFiles()
    }

    fun selectTool(tool: DoctorTool) {
        _selectedTool.value = tool
    }

    fun refreshOrphanFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            _orphanFilesState.value = DoctorOrphanFilesLoadState.Loading
            localFilesMaintenanceRepository.getOrphanLocalFiles()
                .onSuccess { files ->
                    _orphanFilesState.value = DoctorOrphanFilesLoadState.Success(files)
                }
                .onFailure { throwable ->
                    _orphanFilesState.value = DoctorOrphanFilesLoadState.Error(
                        throwable.message ?: "Не удалось загрузить orphan-файлы."
                    )
                }
        }
    }

    fun refreshStorageOverview() {
        coroutineScope.launch(Dispatchers.IO) {
            _storageOverviewState.value = DoctorStorageOverviewLoadState.Loading
            localFilesMaintenanceRepository.getStorageOverview()
                .onSuccess { overview ->
                    _storageOverviewState.value = DoctorStorageOverviewLoadState.Success(overview)
                }
                .onFailure { throwable ->
                    _storageOverviewState.value = DoctorStorageOverviewLoadState.Error(
                        throwable.message ?: "Не удалось загрузить обзор хранилища."
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
                .onSuccess {
                    refreshStorageOverview()
                    refreshOrphanFiles()
                }
                .onFailure { throwable ->
                    _orphanFilesActionError.value = throwable.message ?: "Не удалось удалить файл."
                }
        }
    }

    fun deleteAllOrphanFiles() {
        val currentState = orphanFilesState.value as? DoctorOrphanFilesLoadState.Success ?: return
        coroutineScope.launch(Dispatchers.IO) {
            currentState.files.forEach { orphan ->
                localFilesMaintenanceRepository.deleteLocalFile(orphan.path)
                    .onFailure { throwable ->
                        _orphanFilesActionError.value =
                            throwable.message ?: "Не удалось удалить orphan-файлы."
                        return@launch
                    }
            }
            refreshStorageOverview()
            refreshOrphanFiles()
        }
    }

    fun dismissOrphanFilesActionError() {
        _orphanFilesActionError.value = null
    }
}
