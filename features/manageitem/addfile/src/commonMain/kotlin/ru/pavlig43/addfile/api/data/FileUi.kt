package ru.pavlig43.addfile.api.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path


data class FileUi(
    val id: Int,
    val composeKey: Int,
    internal val platformFile: PlatformFile,
    val uploadState: UploadState,
    val removeState: RemoveState
) {
    val path by lazy { platformFile.path }
}

sealed interface UploadState {

    data object Loading : UploadState
    data object Success : UploadState
    data class Error(val message: String) : UploadState
}
sealed interface RemoveState{
    data object Init: RemoveState
    data object InProgress: RemoveState
    data class Error(val message: String) : RemoveState
}
