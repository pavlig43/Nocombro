package ru.pavlig43.database.data.files

import java.io.File
import java.nio.file.Path

/**
 * Строит канонический ключ файла.
 *
 * Эта структура должна быть единой для локального кэша и для удалённого хранилища,
 * чтобы восстановление и диагностика не зависели от платформы или локальных `id`.
 */
fun buildCanonicalFileKey(
    ownerType: OwnerType,
    fileSyncId: String,
    originalName: String,
): String {
    val sanitizedName = originalName.replace("\\", "_").replace("/", "_")
    return normalizeLogicalFileKey("files/${ownerType.name.lowercase()}/$fileSyncId/$sanitizedName")
}

/**
 * Возвращает абсолютный путь локального кэша файла в профиле текущего пользователя.
 *
 * [fileKey] здесь ожидается в каноническом формате проекта c `/`, независимо от ОС.
 * Функция сама адаптирует его под текущую платформу и привязывает к app-data каталогу Nocombro.
 */
fun buildManagedLocalFilePath(
    fileKey: String,
): String {
    val normalizedFileKey = normalizeLogicalFileKey(fileKey)
    val appDataDir = getNocombroAppDataDirectory().toPath().toAbsolutePath().normalize()
    return resolveLogicalFileKey(appDataDir, normalizedFileKey).toString()
}

/**
 * Проверяет и нормализует переносимый ключ управляемого файла.
 *
 * Разделители приводятся к `/`. Пустой ключ, абсолютный Unix-путь, Windows drive
 * prefix, пустые сегменты, `.` и `..` отклоняются до доступа к диску или S3.
 *
 * @param fileKey ключ из Room, YDB, S3 или внешнего вызова.
 * @return безопасный относительный ключ с единым разделителем.
 * @throws IllegalArgumentException если ключ способен выйти за управляемый корень.
 */
fun normalizeLogicalFileKey(fileKey: String): String {
    val normalizedSeparators = fileKey.replace('\\', '/')
    require(normalizedSeparators.isNotBlank()) { "File key is empty" }
    require(!normalizedSeparators.startsWith('/')) { "Absolute file key is forbidden" }
    require(!WINDOWS_DRIVE_PATH.matches(normalizedSeparators)) { "Drive-prefixed file key is forbidden" }
    val segments = normalizedSeparators.split('/')
    require(segments.all { it.isNotBlank() && it != "." && it != ".." }) {
        "File key contains an unsafe path segment"
    }
    return segments.joinToString("/")
}

/**
 * Разрешает логический ключ строго внутри указанного корня.
 *
 * Повторная проверка [Path.startsWith] служит защитой на границе файловой системы,
 * даже если правила нормализации ключа позже будут расширены.
 *
 * @param root каталог, за пределы которого путь не должен выйти.
 * @param fileKey относительный логический ключ.
 * @return нормализованный абсолютный путь внутри [root].
 */
internal fun resolveLogicalFileKey(root: Path, fileKey: String): Path {
    val normalizedRoot = root.toAbsolutePath().normalize()
    val resolved = normalizeLogicalFileKey(fileKey)
        .split('/')
        .fold(normalizedRoot) { path, segment -> path.resolve(segment) }
        .normalize()
    require(resolved.startsWith(normalizedRoot)) { "File key escapes the managed root" }
    return resolved
}

/**
 * Корневой каталог локального кэша управляемых файлов.
 *
 * Это не временная папка, а стабильное хранилище приложения,
 * в котором Nocombro хранит локальные копии файлов и служебные артефакты.
 */
fun getManagedFilesRootDirectory(): File {
    return File(getNocombroAppDataDirectory(), "files").apply { mkdirs() }
}

/**
 * Извлекает имя файла из локального пути или ключа объекта.
 */
fun extractFileName(
    rawPath: String,
): String {
    return rawPath
        .replace("\\", "/")
        .substringAfterLast("/")
}

/**
 * Возвращает корневой app-data каталог Nocombro для текущего пользователя.
 *
 * На Windows использует `%APPDATA%`, а если переменная окружения недоступна,
 * откатывается к домашнему каталогу пользователя.
 */
private fun getNocombroAppDataDirectory(): File {
    val baseDir = System.getenv("APPDATA")
        ?.takeIf(String::isNotBlank)
        ?.let(::File)
        ?: File(System.getProperty("user.home"))
    return File(baseDir, "Nocombro").apply { mkdirs() }
}

private val WINDOWS_DRIVE_PATH = Regex("^[A-Za-z]:.*")
