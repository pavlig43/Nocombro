package ru.pavlig43.files.api.model

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import ru.pavlig43.files.api.uploadState.UploadState


data class FileUi(
    val id: Int,
    val composeKey: Int,
    internal val platformFile: PlatformFile,
    internal val uploadState: UploadState,
) {
    val path by lazy { platformFile.path }
    val name by lazy { platformFile.name }
}