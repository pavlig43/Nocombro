package ru.pavlig43.database.data.files.remote

import java.io.File
import ru.pavlig43.database.data.sync.mirror.FileMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable

/**
 * Загружает активные локальные файлы перед записью их mirror-строк в YDB.
 *
 * Room остаётся постоянной очередью: если запись в YDB не удалась, та же локальная
 * строка снова попадёт в план следующей синхронизации.
 */
class RemoteFileBatchUploadRepository(
    private val remoteFileStorageGateway: RemoteFileStorageGateway,
) {
    /**
     * Последовательно загружает локальные file-победители и останавливается при первой ошибке.
     *
     * Tombstone и строки без ключа пропускаются. Если среди изменений нет файлов
     * для загрузки, настройка S3 не требуется.
     */
    suspend fun uploadLocalWinners(
        changes: List<MirrorPushEntityChange>,
    ): Result<Int> = runCatching {
        val files = changes.asSequence()
            .filter { change -> change.table == MirrorSyncTable.FILE }
            .map { change -> change.row }
            .filterIsInstance<FileMirrorRow>()
            .filter { row -> row.deletedAt == null }
            .mapNotNull { row ->
                row.remoteObjectKey
                    ?.takeIf(String::isNotBlank)
                    ?.let { objectKey -> PendingFile(row, objectKey) }
            }
            .distinctBy { pending -> pending.row.syncId }
            .toList()

        if (files.isEmpty()) return@runCatching 0
        require(remoteFileStorageGateway.isConfigured()) {
            "Remote file storage is not configured for pending file uploads"
        }

        files.forEach { pending ->
            require(File(pending.row.path).isFile) {
                "Local file is missing: table=file, sync_id=" + pending.row.syncId
            }
            remoteFileStorageGateway.upload(
                objectKey = pending.objectKey,
                localPath = pending.row.path,
            ).getOrElse { throwable ->
                throw IllegalStateException(
                    "S3 upload failed: table=file, sync_id=" + pending.row.syncId,
                    throwable,
                )
            }
        }
        files.size
    }

    private data class PendingFile(val row: FileMirrorRow, val objectKey: String)
}
