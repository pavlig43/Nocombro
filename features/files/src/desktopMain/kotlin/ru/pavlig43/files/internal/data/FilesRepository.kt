package ru.pavlig43.files.internal.data

import java.io.File
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.inTransaction

/**
 * Репозиторий вкладки файлов.
 *
 * Он отвечает за два разных слоя одновременно:
 * - метаданные файлов в локальной `Room` таблице `file`;
 * - операции с удалённым object storage, если он настроен.
 *
 * Такое разделение позволяет не смешивать сетевую интеграцию с UI-компонентом
 * и держать логику удаления/обновления вложений в одном месте.
 */
internal class FilesRepository(
    db: NocombroDatabase,
    private val remoteFileStorageGateway: RemoteFileStorageGateway,
    private val syncQueueRepository: SyncQueueRepository,
)  {
    private val dao = db.fileDao
    private val database = db

    /**
     * Загружает список вложений для конкретного владельца формы.
     */
    suspend fun getInit(ownerId: Int,ownerType: OwnerType): Result<List<FileBD>> {
        return runCatching {
            dao.getFiles(ownerId,ownerType)
        }
    }

    suspend fun getFileByOwnerAndDisplayName(
        ownerId: Int,
        ownerType: OwnerType,
        displayName: String,
    ): FileBD? {
        return dao.getFileByOwnerAndDisplayName(
            ownerId = ownerId,
            ownerFileType = ownerType,
            displayName = displayName,
        )
    }

    /**
     * Пытается отправить локальную копию файла в object storage.
     *
     * Если удалённое хранилище не настроено, метод считается успешным и ничего не делает.
     * Это позволяет приложению работать в локальном режиме без отдельной ветки кода в UI.
     */
    suspend fun uploadRemoteCopy(
        objectKey: String,
        localPath: String,
    ): Result<Unit> {
        if (!remoteFileStorageGateway.isConfigured()) {
            return Result.success(Unit)
        }
        return remoteFileStorageGateway.upload(
            objectKey = objectKey,
            localPath = localPath,
        ).map { Unit }
    }

    /**
     * Гарантирует, что по локальному пути существует копия файла, пригодная для открытия.
     *
     * Если локальный файл уже есть, ничего не делает. Если локального файла нет, но известен
     * `remoteObjectKey`, пытается скачать копию из object storage.
     */
    suspend fun ensureLocalFileForOpen(
        localPath: String,
        remoteObjectKey: String?,
    ): Result<Unit> {
        val localFile = File(localPath)
        if (localFile.exists()) {
            return Result.success(Unit)
        }
        val objectKey = remoteObjectKey
            ?: return Result.failure(
                IllegalStateException("Local file is missing and remote object key is empty.")
            )
        if (!remoteFileStorageGateway.isConfigured()) {
            return Result.failure(
                IllegalStateException("Local file is missing and remote storage is not configured.")
            )
        }
        return remoteFileStorageGateway.download(
            objectKey = objectKey,
            localPath = localPath,
        )
    }

    /**
     * Возвращает идентификатор провайдера, только если remote storage реально включен.
     */
    fun remoteProviderId(): String? {
        return remoteFileStorageGateway.providerId
            .takeIf { remoteFileStorageGateway.isConfigured() }
    }

    suspend fun replaceOwnedFile(
        ownerId: Int,
        ownerType: OwnerType,
        displayName: String,
        localPath: String,
        remoteObjectKey: String?,
        remoteStorageProvider: String?,
    ): Result<FileBD> {
        return runCatching {
            database.inTransaction {
                val existing = dao.getFileByOwnerAndDisplayName(ownerId, ownerType, displayName)
                val existingRemoteObjectKey = existing?.remoteObjectKey
                if (
                    existing != null &&
                    existingRemoteObjectKey != null &&
                    existingRemoteObjectKey != remoteObjectKey &&
                    remoteFileStorageGateway.isConfigured()
                ) {
                    remoteFileStorageGateway.delete(existingRemoteObjectKey).getOrThrow()
                }

                val file = FileBD(
                    ownerId = ownerId,
                    ownerFileType = ownerType,
                    displayName = displayName,
                    path = localPath,
                    remoteObjectKey = remoteObjectKey,
                    remoteStorageProvider = remoteStorageProvider,
                    id = existing?.id ?: 0,
                    syncId = existing?.syncId ?: ru.pavlig43.database.data.sync.defaultSyncId(),
                    updatedAt = ru.pavlig43.database.data.sync.defaultUpdatedAt(),
                    deletedAt = null,
                )

                dao.upsertFiles(listOf(file))
                syncQueueRepository.enqueueUpsert(
                    entityTable = FILE_TABLE_NAME,
                    entityLocalId = file.syncId,
                    createdAt = file.updatedAt,
                )
                file
            }
        }
    }

    /**
     * Сохраняет diff списка вложений.
     *
     * При удалении файла сначала пытается удалить remote-объект из bucket,
     * затем чистит строку из локальной БД. Вставка и обновление остаются на стороне Room.
     *
     * Сравнение ведётся по `syncId`, потому что локальный `id` не годится как стабильный
     * идентификатор для будущей multi-device синхронизации файлов.
     */
    suspend fun update(changeSet: ChangeSet<List<FileBD>>): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val old = changeSet.old.orEmpty()
                val new = changeSet.new
                val newBySyncId = new.associateBy(FileBD::syncId)

                val removedFiles = old.filter { it.syncId !in newBySyncId }
                if (removedFiles.isNotEmpty()) {
                    removedFiles.forEach { file ->
                        val remoteObjectKey = file.remoteObjectKey
                        if (remoteObjectKey != null && remoteFileStorageGateway.isConfigured()) {
                            remoteFileStorageGateway.delete(remoteObjectKey).getOrThrow()
                        }
                    }
                    dao.deleteFiles(removedFiles.map(FileBD::id))
                    removedFiles.forEach { file ->
                        syncQueueRepository.enqueueDelete(
                            entityTable = FILE_TABLE_NAME,
                            entityLocalId = file.syncId,
                            createdAt = file.updatedAt,
                        )
                    }
                }

                val filesForUpsert = if (old.isEmpty()) {
                    new
                } else {
                    val oldBySyncId = old.associateBy(FileBD::syncId)
                    new.filter { newFile ->
                        val oldFile = oldBySyncId[newFile.syncId]
                        oldFile == null || oldFile != newFile
                    }
                }
                if (filesForUpsert.isNotEmpty()) {
                    dao.upsertFiles(filesForUpsert)
                    filesForUpsert.forEach { file ->
                        syncQueueRepository.enqueueUpsert(
                            entityTable = FILE_TABLE_NAME,
                            entityLocalId = file.syncId,
                            createdAt = file.updatedAt,
                        )
                    }
                }
            }
        }

    }

}
