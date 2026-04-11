package ru.pavlig43.rootnocombro.internal.settings

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.getManagedFilesRootDirectory
import ru.pavlig43.rootnocombro.api.component.LocalOrphanFile
import java.io.File

internal class LocalFilesMaintenanceRepository(
    db: NocombroDatabase,
) {
    private val fileDao = db.fileDao

    suspend fun getOrphanLocalFiles(): Result<List<LocalOrphanFile>> {
        return runCatching {
            val rootDirectory = getManagedFilesRootDirectory()
            if (!rootDirectory.exists()) {
                return@runCatching emptyList()
            }

            val attachedPaths = fileDao.getAllPaths()
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
