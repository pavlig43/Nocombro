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
 * Перед физическим удалением код заново читает ключи из mirror и список S3.
 * Удаляется только объект, который есть в S3 и отсутствует в активных строках
 * удалённой таблицы `file`.
 */
class RemoteFilesMaintenanceRepository(
    db: NocombroDatabase,
    private val remoteFileStorageGateway: RemoteFileStorageGateway,
    private val mirrorSyncRemoteGateway: MirrorSyncRemoteGateway,
) {
    private val fileDao = db.fileDao

    /**
     * Возвращает текущие S3-объекты, не упомянутые активным удалённым зеркалом.
     *
     * Метод только диагностирует и ничего не удаляет.
     */
    suspend fun getOrphanRemoteFiles(): Result<List<RemoteOrphanFile>> {
        return runCatching {
            val activeMirrorKeys = loadActiveMirrorKeys()
            loadS3Objects()
                .asSequence()
                .filter { it.objectKey !in activeMirrorKeys }
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

    /** Повторно проверяет и удаляет один удалённый объект, только если у него нет активной строки. */
    suspend fun deleteRemoteFile(
        objectKey: String,
    ): Result<Unit> {
        return deleteRemoteFiles(setOf(objectKey)).map { }
    }

    /**
     * Повторно проверяет кандидатов и удаляет подтверждённые объекты без активной строки.
     *
     * @return число фактически удалённых объектов; отсутствующие и ставшие
     * активными ключи молча пропускаются.
     */
    suspend fun deleteRemoteFiles(
        objectKeys: Set<String>,
    ): Result<Int> = runCatching {
        if (objectKeys.isEmpty()) return@runCatching 0

        val activeMirrorKeys = loadActiveMirrorKeys()
        val existingS3Keys = loadS3Objects().mapTo(mutableSetOf()) { it.objectKey }
        val confirmedOrphans = objectKeys
            .filterTo(mutableSetOf()) { it in existingS3Keys && it !in activeMirrorKeys }

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
