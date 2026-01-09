package ru.pavlig43.addfile.api.model

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path


sealed interface UploadState {

    data object Loading : UploadState
    data object Success : UploadState

    data class Error(val message: String) : UploadState
}

data class FileUi(
    val id: Int,
    val composeKey: Int,
    internal val platformFile: PlatformFile,
    val uploadState: UploadState,
) {
    val path by lazy { platformFile.path }
    val name by lazy { platformFile.name }
}