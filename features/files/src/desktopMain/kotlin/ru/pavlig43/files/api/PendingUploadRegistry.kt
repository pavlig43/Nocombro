package ru.pavlig43.files.api

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.Base64

/**
 * Локальная запись S3-загрузки, ещё не закреплённой в Room и mirror.
 *
 * @param objectKey логический ключ объекта без префикса бакета.
 * @param localPath путь исходной локальной копии для ручного восстановления.
 * @param firstSeenAt время первой попытки загрузки.
 * @param lastAttemptAt время последней попытки.
 * @param attemptCount число вызовов [PendingUploadRegistry.markPending].
 */
data class PendingUpload(
    val objectKey: String,
    val localPath: String,
    val firstSeenAt: LocalDateTime,
    val lastAttemptAt: LocalDateTime,
    val attemptCount: Int,
)

/**
 * Хранит незавершённые S3-загрузки вне Room и mirror.
 *
 * Реестр закрывает окно между загрузкой бинарного объекта и сохранением строки
 * `file`: после сбоя приложения Doctor увидит объект как pending и не удалит его
 * как orphan. Запись ведётся через временный файл и атомарную замену, а общий lock
 * защищает несколько экземпляров реестра внутри процесса.
 *
 * @param registryPath путь TSV-файла реестра.
 * @param currentLocalDateTime источник времени, подменяемый в тестах.
 */
class PendingUploadRegistry(
    private val registryPath: Path = defaultRegistryPath(),
    private val currentLocalDateTime: () -> LocalDateTime = ::getCurrentLocalDateTime,
) {
    /**
     * Создаёт запись перед сетью или отмечает ещё одну попытку существующей загрузки.
     *
     * Время первого появления сохраняется, а время последней попытки и счётчик
     * обновляются при каждом вызове.
     */
    fun markPending(objectKey: String, localPath: String) = withRegistryLock {
        val entries = readEntries().associateByTo(linkedMapOf(), PendingUpload::objectKey)
        val previous = entries[objectKey]
        val attemptedAt = currentLocalDateTime()
        entries[objectKey] = PendingUpload(
            objectKey = objectKey,
            localPath = localPath,
            firstSeenAt = previous?.firstSeenAt ?: attemptedAt,
            lastAttemptAt = attemptedAt,
            attemptCount = (previous?.attemptCount ?: 0) + 1,
        )
        writeEntries(entries.values)
    }

    /** Удаляет ключ после того, как метаданные файла успешно закреплены в Room. */
    fun complete(objectKey: String) = withRegistryLock {
        val entries = readEntries().filterNot { it.objectKey == objectKey }
        writeEntries(entries)
    }

    /** Возвращает снимок записей от самой старой к самой новой. */
    fun list(): List<PendingUpload> = withRegistryLock {
        readEntries().sortedBy(PendingUpload::firstSeenAt)
    }

    /** Читает и строго проверяет TSV; повреждение не маскируется пустым списком. */
    private fun readEntries(): List<PendingUpload> {
        if (!Files.isRegularFile(registryPath)) return emptyList()
        return Files.readAllLines(registryPath, StandardCharsets.UTF_8)
            .filter(String::isNotBlank)
            .map { line ->
                val parts = line.split('\t')
                require(parts.size == FIELD_COUNT) { "Pending upload registry is corrupt" }
                PendingUpload(
                    objectKey = parts[0].decodeField(),
                    localPath = parts[1].decodeField(),
                    firstSeenAt = LocalDateTime.parse(parts[2]),
                    lastAttemptAt = LocalDateTime.parse(parts[3]),
                    attemptCount = parts[4].toInt(),
                )
            }
    }

    /**
     * Перезаписывает весь реестр через соседний `.part` файл.
     *
     * Если файловая система не поддерживает атомарный move, используется замена
     * без атомарности; временный файл удаляется при любом исходе.
     */
    private fun writeEntries(entries: Collection<PendingUpload>) {
        registryPath.parent?.let(Files::createDirectories)
        val part = registryPath.resolveSibling("${registryPath.fileName}.part")
        val lines = entries.map { entry ->
            listOf(
                entry.objectKey.encodeField(),
                entry.localPath.encodeField(),
                entry.firstSeenAt.toString(),
                entry.lastAttemptAt.toString(),
                entry.attemptCount.toString(),
            ).joinToString("\t")
        }
        Files.write(part, lines, StandardCharsets.UTF_8)
        try {
            Files.move(part, registryPath, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(part, registryPath, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(part)
        }
    }

    private companion object {
        /** Число полей в одной строке TSV-реестра. */
        const val FIELD_COUNT = 5

        /** Общий монитор, который последовательно выполняет операции всех экземпляров реестра. */
        val lock = Any()

        /**
         * Выбирает постоянный путь реестра в каталоге данных пользователя.
         *
         * В Windows используется `%APPDATA%`; в остальных средах и тестовых
         * запусках резервным корнем служит системное свойство `user.home`.
         */
        fun defaultRegistryPath(): Path {
            val base = System.getenv("APPDATA")?.takeIf(String::isNotBlank)
                ?: System.getProperty("user.home")
            return Path.of(base, "Nocombro", "pending-uploads.tsv")
        }

        /** Выполняет [block] под общим монитором файлового реестра. */
        fun <T> withRegistryLock(block: () -> T): T = synchronized(lock, block)
    }
}

/** Кодирует произвольное поле в URL-safe Base64, чтобы оно не нарушало TSV-разметку. */
private fun String.encodeField(): String = Base64.getUrlEncoder().withoutPadding()
    .encodeToString(toByteArray(StandardCharsets.UTF_8))

/** Декодирует поле, записанное функцией [encodeField]. */
private fun String.decodeField(): String = String(
    Base64.getUrlDecoder().decode(this),
    StandardCharsets.UTF_8,
)
