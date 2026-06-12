package ru.pavlig43.database.data.files.remote

import ru.pavlig43.database.NocombroDatabase
import java.io.File

/**
 * Массово восстанавливает локальные копии файлов, которые уже известны локальной БД,
 * но физически отсутствуют на диске.
 *
 * Репозиторий сознательно не ходит по удаленному bucket "вслепую": сначала он читает
 * локальную таблицу `file`, а затем скачивает только те объекты, для которых:
 * - запись уже пришла через обычную синхронизацию метаданных;
 * - есть `remoteObjectKey`;
 * - локальная копия файла отсутствует.
 *
 * Это гарантирует, что файлы не будут скачиваться "в никуда" до обновления локальной БД.
 */
class RemoteFileBatchDownloadRepository(
    db: NocombroDatabase,
    private val remoteFileStorageGateway: RemoteFileStorageGateway,
) {
    private val fileDao = db.fileDao

    /** Проверяет, настроен ли underlying remote storage gateway. */
    fun isConfigured(): Boolean = remoteFileStorageGateway.isConfigured()

    /**
     * Подгружает все отсутствующие локальные копии файлов, известные текущей локальной БД.
     *
     * Удаленные metadata, строки без object key и уже существующие локальные файлы
     * пропускаются. Ошибка отдельного файла не прерывает batch: имя добавляется в
     * [RemoteFileBatchDownloadSummary.failedFiles].
     */
    suspend fun downloadMissingLocalCopies(): Result<RemoteFileBatchDownloadSummary> {
        return runCatching {
            require(remoteFileStorageGateway.isConfigured()) {
                "Удаленное файловое хранилище не настроено."
            }

            val filesToDownload = fileDao.getAllFiles()
                .asSequence()
                .filter { it.deletedAt == null }
                .filter { !it.remoteObjectKey.isNullOrBlank() }
                .filterNot { File(it.path).exists() }
                .toList()

            var downloadedCount = 0
            val failedFiles = mutableListOf<String>()

            filesToDownload.forEach { file ->
                val downloadResult = remoteFileStorageGateway.download(
                    objectKey = file.remoteObjectKey.orEmpty(),
                    localPath = file.path,
                )
                if (downloadResult.isSuccess) {
                    downloadedCount += 1
                } else {
                    failedFiles += file.displayName
                }
            }

            RemoteFileBatchDownloadSummary(
                scannedCount = filesToDownload.size,
                downloadedCount = downloadedCount,
                failedFiles = failedFiles,
            )
        }
    }
}

/**
 * Итог массовой догрузки файлов для UI и sync result.
 *
 * @property scannedCount количество отсутствующих локальных файлов, для которых
 * была предпринята попытка скачивания.
 * @property downloadedCount количество успешных скачиваний.
 * @property failedFiles display names файлов, которые не удалось восстановить.
 */
data class RemoteFileBatchDownloadSummary(
    val scannedCount: Int,
    val downloadedCount: Int,
    val failedFiles: List<String>,
) {
    /** Количество неуспешных попыток, производное от [failedFiles]. */
    val failedCount: Int get() = failedFiles.size
}
