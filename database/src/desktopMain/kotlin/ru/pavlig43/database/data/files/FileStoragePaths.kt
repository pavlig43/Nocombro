package ru.pavlig43.database.data.files

import java.io.File

/**
 * Строит канонический ключ файла.
 *
 * Эта структура должна быть единой для локального кэша и для удалённого object storage,
 * чтобы восстановление и диагностика не зависели от платформы или локальных `id`.
 */
fun buildCanonicalFileKey(
    ownerType: OwnerType,
    fileSyncId: String,
    originalName: String,
): String {
    val sanitizedName = originalName.replace("\\", "_").replace("/", "_")
    return "files/${ownerType.name.lowercase()}/$fileSyncId/$sanitizedName"
}

/**
 * Возвращает абсолютный путь локального кэша файла в профиле текущего пользователя.
 */
fun buildManagedLocalFilePath(
    fileKey: String,
): String {
    val appDataDir = getNocombroAppDataDirectory()
    val normalizedKey = fileKey.replace("/", File.separator)
    return File(appDataDir, normalizedKey).absolutePath
}

/**
 * Корневой каталог локального кэша управляемых файлов.
 */
fun getManagedFilesRootDirectory(): File {
    return File(getNocombroAppDataDirectory(), "files").apply { mkdirs() }
}

/**
 * Извлекает имя файла из локального пути или object key.
 */
fun extractFileName(
    rawPath: String,
): String {
    return rawPath
        .replace("\\", "/")
        .substringAfterLast("/")
}

private fun getNocombroAppDataDirectory(): File {
    val baseDir = System.getenv("APPDATA")
        ?.takeIf(String::isNotBlank)
        ?.let(::File)
        ?: File(System.getProperty("user.home"))
    return File(baseDir, "Nocombro").apply { mkdirs() }
}
