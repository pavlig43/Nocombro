package ru.pavlig43.files.internal.data

import java.io.File
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.inTransaction
import ru.pavlig43.files.api.PendingUploadRegistry
import ru.pavlig43.database.data.sync.defaultUpdatedAt

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
    private val pendingUploadRegistry: PendingUploadRegistry = PendingUploadRegistry(),
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
     * Перед сетью ключ сохраняется в [PendingUploadRegistry]. Даже успешная загрузка
     * остаётся pending до записи метаданных в Room через [replaceOwnedFile] или [update].
     */
    suspend fun uploadRemoteCopy(
        objectKey: String,
        localPath: String,
    ): Result<Unit> {
        if (!remoteFileStorageGateway.isConfigured()) {
            return Result.success(Unit)
        }
        pendingUploadRegistry.markPending(objectKey, localPath)
        return remoteFileStorageGateway.upload(
            objectKey = objectKey,
            localPath = localPath,
        ).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) },
        )
    }

    /**
     * Гарантирует, что по локальному пути существует копия файла, пригодная для открытия.
     *
     * Если локальный файл уже есть, ничего не делает. Если локального файла нет, но известен
     * `remoteObjectKey`, пытается скачать копию из object storage.
     */
    @Suppress("ReturnCount")
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

    /**
     * Создаёт или заменяет метаданные файла с новой монотонной sync-версией.
     *
     * Старый S3-объект здесь не удаляется: физическую чистку делает Doctor после
     * сверки remote mirror. После успешной Room-транзакции ключ исключается из
     * реестра незавершённых загрузок.
     */
    @Suppress("LongParameterList")
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
                val file = FileBD(
                    ownerId = ownerId,
                    ownerFileType = ownerType,
                    displayName = displayName,
                    path = localPath,
                    remoteObjectKey = remoteObjectKey,
                    remoteStorageProvider = remoteStorageProvider,
                    id = existing?.id ?: 0,
                    syncId = existing?.syncId ?: ru.pavlig43.database.data.sync.defaultSyncId(),
                    updatedAt = defaultUpdatedAt(existing?.updatedAt),
                    deletedAt = null,
                )

                dao.upsertFiles(listOf(file))
                file
            }
        }.onSuccess { file -> file.remoteObjectKey?.let(pendingUploadRegistry::complete) }
    }

    /**
     * Сохраняет diff списка вложений.
     *
     * Удалённые из списка файлы не стираются из S3 и Room физически. Они получают
     * tombstone с версией строго новее прежней, после чего удаление уйдёт в mirror.
     * Физическую чистку S3 позже выполняет Doctor по свежему remote snapshot.
     *
     * Сравнение ведётся по `syncId`, потому что локальный `id` не годится как стабильный
     * идентификатор для будущей multi-device синхронизации файлов.
     * После успешной транзакции ключи нового списка удаляются из pending-реестра.
     */
    suspend fun update(changeSet: ChangeSet<List<FileBD>>): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val old = changeSet.old.orEmpty()
                val new = changeSet.new
                val newBySyncId = new.associateBy(FileBD::syncId)

                val removedFiles = old.filter { it.syncId !in newBySyncId }
                if (removedFiles.isNotEmpty()) {
                    val tombstones = removedFiles.map { file ->
                        val deletedAt = defaultUpdatedAt(
                            file.deletedAt?.takeIf { it > file.updatedAt } ?: file.updatedAt
                        )
                        file.copy(updatedAt = deletedAt, deletedAt = deletedAt)
                    }
                    dao.upsertFiles(tombstones)
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
                }
            }
        }.onSuccess {
            changeSet.new.mapNotNull(FileBD::remoteObjectKey)
                .forEach(pendingUploadRegistry::complete)
        }

    }

}
