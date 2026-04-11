package ru.pavlig43.doctor.internal.component

import ru.pavlig43.files.api.LocalFilesStorageOverview
import ru.pavlig43.files.api.model.LocalOrphanFile

enum class DoctorTool(
    val title: String,
    val subtitle: String,
) {
    StorageOverview(
        title = "Обзор хранилища",
        subtitle = "Показать размер локального каталога, число файлов и orphan-файлы.",
    ),
    FileCleanup(
        title = "Чистка файлов",
        subtitle = "Найти и удалить orphan-файлы из локального каталога приложения.",
    ),
}

sealed interface DoctorOrphanFilesLoadState {
    data object Loading : DoctorOrphanFilesLoadState
    data class Error(val message: String) : DoctorOrphanFilesLoadState
    data class Success(val files: List<LocalOrphanFile>) : DoctorOrphanFilesLoadState
}

sealed interface DoctorStorageOverviewLoadState {
    data object Loading : DoctorStorageOverviewLoadState
    data class Error(val message: String) : DoctorStorageOverviewLoadState
    data class Success(val overview: LocalFilesStorageOverview) : DoctorStorageOverviewLoadState
}
