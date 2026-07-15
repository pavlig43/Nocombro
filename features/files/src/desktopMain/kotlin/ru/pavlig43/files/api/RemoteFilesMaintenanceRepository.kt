package ru.pavlig43.files.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.sync.mirror.FileMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRemoteGateway
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.files.api.model.RemoteOrphanFile

/**
 * Выполняет диагностику и безопасную очистку объектов в удалённом хранилище.
 *
 * Источник истины для принадлежности объекта — активная типизированная mirror-таблица
 * `file`, а не локальная Room-база текущего компьютера. Так мы не удалим файл,
 * который прикрепили на другой синхронизированной установке.
 *
 * Перед физическим удалением код заново читает ключи из mirror, Room, реестра
 * незавершённых загрузок и S3. Удаляется лишь объект, который не защищён ни одним
 * из этих источников. Это закрывает окно между загрузкой бинарного файла и записью
 * его метаданных в Room/YDB.
 */
class RemoteFilesMaintenanceRepository(
    db: NocombroDatabase,
    private val remoteFileStorageGateway: RemoteFileStorageGateway,
    private val mirrorSyncRemoteGateway: MirrorSyncRemoteGateway,
    private val pendingUploadRegistry: PendingUploadRegistry = PendingUploadRegistry(),
) {
    private val fileDao = db.fileDao

    /**
     * Возвращает текущие S3-объекты, не защищённые mirror, Room или pending-реестром.
     *
     * Метод только диагностирует и ничего не удаляет. Ошибка любого источника
     * отменяет результат, чтобы неизвестное состояние не выглядело пустым списком.
     */
    suspend fun getOrphanRemoteFiles(): Result<List<RemoteOrphanFile>> {
        return runCatching {
            val protectedKeys = loadProtectedKeys()
            loadS3Objects()
                .asSequence()
                .filter { it.objectKey !in protectedKeys }
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

    /** Возвращает ключи удалённых объектов из локальной Room-базы. */
    suspend fun getAttachedRemoteObjectKeys(): Result<Set<String>> {
        return runCatching {
            fileDao.getAllRemoteObjectKeys()
                .map(remoteFileStorageGateway::normalizeObjectKey)
                .toSet()
        }
    }

    /** Повторно проверяет и удаляет объект лишь при отсутствии всех защитных ключей. */
    suspend fun deleteRemoteFile(
        objectKey: String,
    ): Result<Unit> {
        return deleteRemoteFiles(setOf(objectKey)).map { }
    }

    /**
     * Повторно проверяет кандидатов по S3, mirror, Room и pending-реестру.
     *
     * @return число фактически удалённых объектов; отсутствующие и ставшие
     * активными ключи молча пропускаются.
     */
    suspend fun deleteRemoteFiles(
        objectKeys: Set<String>,
    ): Result<Int> = runCatching {
        if (objectKeys.isEmpty()) return@runCatching 0

        val protectedKeys = loadProtectedKeys()
        val existingS3Keys = loadS3Objects().mapTo(mutableSetOf()) { it.objectKey }
        val confirmedOrphans = objectKeys
            .filterTo(mutableSetOf()) { it in existingS3Keys && it !in protectedKeys }

        confirmedOrphans.forEach { objectKey ->
            remoteFileStorageGateway.delete(objectKey).getOrThrow()
        }
        confirmedOrphans.size
    }

    /** Возвращает ключи объектов из активных строк удалённого зеркала `file`. */
    suspend fun getActiveMirrorObjectKeys(): Result<Set<String>> = runCatching {
        loadActiveMirrorKeys()
    }

    /** Возвращает актуальный набор ключей, физически существующих в S3. */
    suspend fun getS3ObjectKeys(): Result<Set<String>> = runCatching {
        loadS3Objects().mapTo(mutableSetOf()) {
            remoteFileStorageGateway.normalizeObjectKey(it.objectKey)
        }
    }

    /**
     * Возвращает незавершённые загрузки для диагностики Doctor.
     *
     * Ошибка чтения повреждённого реестра возвращается наружу и должна блокировать
     * очистку S3, а не трактоваться как пустой список.
     */
    fun getPendingUploads(): Result<List<PendingUpload>> = runCatching {
        pendingUploadRegistry.list()
    }

    /**
     * Собирает ключи, которые нельзя считать orphan-объектами.
     *
     * В набор входят активный remote mirror, текущая Room-БД и незавершённые
     * загрузки. Все ключи приводятся к одному логическому виду без S3-префикса.
     */
    private suspend fun loadProtectedKeys(): Set<String> {
        val remote = loadActiveMirrorKeys()
        val local = fileDao.getAllRemoteObjectKeys()
            .map(remoteFileStorageGateway::normalizeObjectKey)
        val pending = pendingUploadRegistry.list()
            .map { remoteFileStorageGateway.normalizeObjectKey(it.objectKey) }
        return remote + local + pending
    }

    private suspend fun loadActiveMirrorKeys(): Set<String> {
        val status = mirrorSyncRemoteGateway.getStatus()
        require(status.configured) { status.error ?: "Mirror sync is not configured." }
        require(status.error == null) { status.error ?: "Mirror sync is unavailable." }
        require(MirrorSyncTable.FILE.tableName in status.availableTables) {
            "Mirror file table is unavailable."
        }

        return mirrorSyncRemoteGateway
            .loadRemoteSnapshot(listOf(MirrorSyncTable.FILE))
            .getOrThrow()
            .rowsByTable[MirrorSyncTable.FILE]
            .orEmpty()
            .filterIsInstance<FileMirrorRow>()
            .asSequence()
            .filter { it.deletedAt == null }
            .mapNotNull { it.remoteObjectKey?.takeIf(String::isNotBlank) }
            .map(remoteFileStorageGateway::normalizeObjectKey)
            .toSet()
    }

    private suspend fun loadS3Objects() = run {
        require(remoteFileStorageGateway.isConfigured()) {
            "Remote file storage is not configured."
        }
        remoteFileStorageGateway.listObjects().getOrThrow()
            .map { remoteObject ->
                remoteObject.copy(
                    objectKey = remoteFileStorageGateway.normalizeObjectKey(
                        remoteObject.objectKey
                    )
                )
            }
    }
}
