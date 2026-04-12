package ru.pavlig43.files.api.model

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.files.api.uploadState.UploadState


data class FileUi(
    val id: Int,
    val syncId: String,
    val updatedAt: LocalDateTime,
    val composeKey: Int,
    val displayName: String,
    internal val platformFile: PlatformFile,
    internal val uploadState: UploadState,
    val remoteObjectKey: String? = null,
    val remoteStorageProvider: String? = null,
) {
    val path by lazy { platformFile.path }
    val name by lazy { displayName }
}
