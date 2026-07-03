package ru.pavlig43.files.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.getManagedFilesRootDirectory
import ru.pavlig43.files.api.model.LocalOrphanFile
import java.io.File

/**
 * Scans the managed local files directory and deletes files that are no longer
 * referenced by active file metadata.
 */
class LocalFilesMaintenanceRepository(
    db: NocombroDatabase,
) {
    private val fileDao = db.fileDao

    /**
     * Counts managed files, their total size, and local orphans.
     */
    suspend fun getStorageOverview(): Result<LocalFilesStorageOverview> {
        return runCatching {
            val rootDirectory = getManagedFilesRootDirectory()
            if (!rootDirectory.exists()) {
                return@runCatching LocalFilesStorageOverview(
                    rootPath = rootDirectory.absolutePath,
                    localFilesCount = 0,
                    localFilesSizeBytes = 0,
                    orphanFilesCount = 0,
                )
            }

            val attachedPaths = fileDao.getActivePaths()
                .map(::normalizePathForComparison)
                .toSet()

            var totalFiles = 0
            var totalSizeBytes = 0L
            var orphanFiles = 0

            rootDirectory.walkTopDown()
                .filter(File::isFile)
                .forEach { file ->
                    totalFiles += 1
                    totalSizeBytes += file.length()
                    if (normalizePathForComparison(file.absolutePath) !in attachedPaths) {
                        orphanFiles += 1
                    }
                }

            LocalFilesStorageOverview(
                rootPath = rootDirectory.absolutePath,
                localFilesCount = totalFiles,
                localFilesSizeBytes = totalSizeBytes,
                orphanFilesCount = orphanFiles,
            )
        }
    }

    /**
     * Returns local files that are not referenced by active file rows.
     */
    suspend fun getOrphanLocalFiles(): Result<List<LocalOrphanFile>> {
        return runCatching {
            val rootDirectory = getManagedFilesRootDirectory()
            if (!rootDirectory.exists()) {
                return@runCatching emptyList()
            }

            val attachedPaths = fileDao.getActivePaths()
                .map(::normalizePathForComparison)
                .toSet()

            rootDirectory
                .walkTopDown()
                .filter(File::isFile)
                .map { file ->
                    LocalOrphanFile(
                        name = file.name,
                        path = file.absolutePath,
                        relativePath = file.relativeTo(rootDirectory).invariantSeparatorsPath,
                        sizeBytes = file.length(),
                    )
                }
                .filter { orphan ->
                    normalizePathForComparison(orphan.path) !in attachedPaths
                }
                .sortedBy { it.relativePath }
                .toList()
        }
    }

    /**
     * Deletes one orphan file under the managed files root and removes empty parent folders.
     */
    suspend fun deleteLocalFile(path: String): Result<Unit> {
        return runCatching {
            val rootDirectory = getManagedFilesRootDirectory().canonicalFile
            val targetFile = File(path).canonicalFile
            require(targetFile.exists()) { "Файл уже отсутствует." }
            require(targetFile.isFile) { "Можно удалить только файл." }
            require(targetFile.path.startsWith(rootDirectory.path)) {
                "Разрешено удалять только файлы из каталога приложения."
            }
            if (!targetFile.delete()) {
                error("Не удалось удалить локальный файл.")
            }
            pruneEmptyParents(targetFile.parentFile, rootDirectory)
        }
    }

    private fun pruneEmptyParents(
        startDirectory: File?,
        rootDirectory: File,
    ) {
        var current = startDirectory
        while (current != null && current != rootDirectory) {
            val children = current.listFiles()
            if (!children.isNullOrEmpty()) {
                break
            }
            if (!current.delete()) {
                break
            }
            current = current.parentFile
        }
    }

    private fun normalizePathForComparison(path: String): String {
        return File(path)
            .absolutePath
            .replace('/', File.separatorChar)
            .lowercase()
    }
}

/**
 * Summary of the managed local files directory.
 */
data class LocalFilesStorageOverview(
    val rootPath: String,
    val localFilesCount: Int,
    val localFilesSizeBytes: Long,
    val orphanFilesCount: Int,
)
