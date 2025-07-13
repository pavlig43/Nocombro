package ru.pavlig43.addfile.api.data

import io.github.vinceglb.filekit.PlatformFile

data class AddedFile(
    val index: Int,
    val platformFile: PlatformFile,
    val uploadState: UploadState = UploadState.Error
)
sealed interface UploadState{

    data object Loading: UploadState
    data object Success: UploadState
    data object Error: UploadState
}
