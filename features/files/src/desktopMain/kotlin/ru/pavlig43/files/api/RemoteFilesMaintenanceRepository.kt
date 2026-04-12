package ru.pavlig43.files.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.files.api.model.RemoteOrphanFile

class RemoteFilesMaintenanceRepository(
    db: NocombroDatabase,
    private val remoteFileStorageGateway: RemoteFileStorageGateway,
) {
    private val fileDao = db.fileDao

    suspend fun getOrphanRemoteFiles(): Result<List<RemoteOrphanFile>> {
        return runCatching {
            require(remoteFileStorageGateway.isConfigured()) {
                "Remote file storage is not configured."
            }

            val attachedKeys = fileDao.getAllRemoteObjectKeys().toSet()

            remoteFileStorageGateway.listObjects()
                .getOrThrow()
                .asSequence()
                .filter { it.objectKey !in attachedKeys }
                .sortedBy { it.objectKey }
                .map { remoteObject ->
                    RemoteOrphanFile(
                        objectKey = remoteObject.objectKey,
                        sizeBytes = remoteObject.sizeBytes,
                    )
                }
                .toList()
        }
    }

    suspend fun deleteRemoteFile(
        objectKey: String,
    ): Result<Unit> {
        require(remoteFileStorageGateway.isConfigured()) {
            return Result.failure(
                IllegalStateException("Remote file storage is not configured.")
            )
        }
        return remoteFileStorageGateway.delete(objectKey)
    }
}
