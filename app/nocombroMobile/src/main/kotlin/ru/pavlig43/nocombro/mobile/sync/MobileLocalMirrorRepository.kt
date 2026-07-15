package ru.pavlig43.nocombro.mobile.sync

import androidx.room.withTransaction
import java.io.File
import java.nio.file.Path
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryFileEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentReminderEntity

/**
 * Строит снимок локальной Room-БД и применяет удалённые версии обратно.
 *
 * Для файлов репозиторий работает только с метаданными. Бинарные данные живут
 * в S3 и скачиваются отдельным шагом после применения строк `file`.
 */
class MobileLocalMirrorRepository(
    private val db: NocombroMobileDatabase,
    private val filesDirPath: String,
) {
    /**
     * Читает локальные эксперименты, записи, напоминания и файлы как mirror rows.
     *
     * `remoteObjectKey` всегда отдаётся без S3-префикса. Префикс добавляет
     * только S3-шлюз, когда реально идёт запрос к bucket.
     */
    suspend fun loadSnapshot(config: MobileS3Config): MobileMirrorSnapshot {
        val experiments = db.experimentDao.getAll()
        val experimentById = experiments.associateBy(MobileExperimentEntity::id)
        val entries = db.experimentEntryDao.getAll()
        val entryById = entries.associateBy(MobileExperimentEntryEntity::id)
        val reminders = db.experimentReminderDao.getAll()
        val files = db.experimentEntryFileDao.getAll()

        return MobileMirrorSnapshot(
            loadedAt = getCurrentLocalDateTime(),
            rowsByTable = mapOf(
                MobileMirrorTable.EXPERIMENT to experiments.map { it.toMirrorRow() },
                MobileMirrorTable.EXPERIMENT_ENTRY to entries.mapNotNull { entry ->
                    experimentById[entry.experimentId]?.let { entry.toMirrorRow(it.syncId) }
                },
                MobileMirrorTable.EXPERIMENT_REMINDER to reminders.mapNotNull { reminder ->
                    experimentById[reminder.experimentId]?.let { reminder.toMirrorRow(it.syncId) }
                },
                MobileMirrorTable.FILE to files.mapNotNull { file ->
                    entryById[file.entryId]?.let { file.toMirrorRow(it.syncId, config) }
                },
            ),
        )
    }

    /**
     * Применяет удалённые изменения одной транзакцией.
     *
     * Родительские строки создаются раньше дочерних, а tombstone применяются
     * после активных строк. Это нужно, чтобы локальные foreign key не ломались
     * во время pull.
     */
    suspend fun applyRemoteChanges(
        changes: List<MobileMirrorChange>,
        config: MobileS3Config,
    ) {
        db.withTransaction {
            val ordered = changes.sortedWith(
                compareBy<MobileMirrorChange> { tombstoneOrder(it) }
                    .thenBy { if (it.row.deletedAt == null) it.table.order else -it.table.order }
                    .thenBy { it.row.syncId },
            )
            ordered.forEach { change ->
                when (val row = change.row) {
                    is MobileExperimentMirrorRow -> applyExperiment(row)
                    is MobileExperimentEntryMirrorRow -> applyEntry(row)
                    is MobileExperimentReminderMirrorRow -> applyReminder(row)
                    is MobileFileMirrorRow -> applyFile(row, config)
                }
            }
        }
    }

    /**
     * Строит путь локальной копии для файла, который пришёл из S3.
     *
     * В путь кладётся логический ключ без S3-префикса. Тогда файл, полученный с
     * другого устройства, при следующей отправке не создаст `prefix/prefix/...`.
     */
    fun localPathForObjectKey(objectKey: String): String {
        val root = Path.of(filesDirPath, "nocombro").toAbsolutePath().normalize()
        val resolved = normalizeMobileLogicalFileKey(objectKey)
            .split('/')
            .fold(root) { path, segment -> path.resolve(segment) }
            .normalize()
        require(resolved.startsWith(root)) { "File key escapes the managed root" }
        return resolved.toString()
    }

    /**
     * Применяет удалённую строку эксперимента, если её версия новее локальной.
     *
     * Для старого parent-only tombstone сперва создаёт tombstone всего локального
     * дерева. Это не даёт дочерним строкам воскреснуть на следующем push.
     */
    private suspend fun applyExperiment(row: MobileExperimentMirrorRow) {
        val current = db.experimentDao.getExperimentBySyncId(row.syncId)
        if (current != null && row.versionAt() <= current.entityVersionAt()) return
        if (current != null && row.deletedAt != null) {
            cascadeLegacyExperimentDelete(current.id, row.versionAt())
        }
        db.experimentDao.upsert(
            MobileExperimentEntity(
                id = current?.id ?: 0,
                syncId = row.syncId,
                title = row.title,
                ideaDescription = row.ideaDescription,
                isArchived = row.isArchived,
                updatedAt = row.updatedAt,
                deletedAt = row.deletedAt,
            )
        )
    }

    /**
     * Применяет запись журнала только при наличии родителя и более новой версии.
     */
    private suspend fun applyEntry(row: MobileExperimentEntryMirrorRow) {
        val experiment = db.experimentDao.getExperimentBySyncId(row.experimentSyncId) ?: return
        val current = db.experimentEntryDao.getEntryBySyncId(row.syncId)
        if (current != null && row.versionAt() <= current.entityVersionAt()) return
        db.experimentEntryDao.upsert(
            MobileExperimentEntryEntity(
                id = current?.id ?: 0,
                experimentId = experiment.id,
                syncId = row.syncId,
                entryDate = row.entryDate,
                createdAt = row.createdAt,
                content = row.content,
                updatedAt = row.updatedAt,
                deletedAt = row.deletedAt,
            )
        )
    }

    /**
     * Применяет напоминание только при наличии родителя и более новой версии.
     */
    private suspend fun applyReminder(row: MobileExperimentReminderMirrorRow) {
        val experiment = db.experimentDao.getExperimentBySyncId(row.experimentSyncId) ?: return
        val current = db.experimentReminderDao.getReminderBySyncId(row.syncId)
        if (current != null && row.versionAt() <= current.entityVersionAt()) return
        db.experimentReminderDao.upsert(
            MobileExperimentReminderEntity(
                id = current?.id ?: 0,
                experimentId = experiment.id,
                syncId = row.syncId,
                text = row.text,
                reminderDateTime = row.reminderDateTime,
                updatedAt = row.updatedAt,
                deletedAt = row.deletedAt,
            )
        )
    }

    /**
     * Применяет метаданные файла записи эксперимента.
     *
     * Путь с другого устройства не переносится в локальную файловую систему:
     * для новой строки он заново строится из проверенного логического ключа.
     */
    private suspend fun applyFile(
        row: MobileFileMirrorRow,
        config: MobileS3Config,
    ) {
        if (row.ownerType != MobileFileOwnerType.EXPERIMENT_ENTRY) return
        val entry = db.experimentEntryDao.getEntryBySyncId(row.ownerSyncId) ?: return
        val current = db.experimentEntryFileDao.getFileBySyncId(row.syncId)
        if (current != null && row.versionAt() <= current.entityVersionAt()) return
        val objectKey = config.normalizeObjectKey(row.remoteObjectKey ?: row.path)
        db.experimentEntryFileDao.upsert(
            MobileExperimentEntryFileEntity(
                id = current?.id ?: 0,
                entryId = entry.id,
                syncId = row.syncId,
                displayName = row.displayName,
                localPath = current?.localPath ?: localPathForObjectKey(objectKey),
                objectKey = objectKey,
                updatedAt = row.updatedAt,
                deletedAt = row.deletedAt,
            )
        )
    }

    /**
     * Превращает старый parent-only tombstone в удаление всего локального дерева.
     *
     * Версия каскада выбирается строго новее tombstone родителя и всех дочерних
     * строк. Файлы, напоминания и записи помечаются в порядке «потомок — родитель».
     *
     * @param experimentId локальный идентификатор удаляемого эксперимента.
     * @param parentDeleteVersion версия удалённого tombstone родителя.
     */
    private suspend fun cascadeLegacyExperimentDelete(
        experimentId: Int,
        parentDeleteVersion: LocalDateTime,
    ) {
        val entries = db.experimentEntryDao.getEntriesByExperiment(experimentId)
        val reminders = db.experimentReminderDao.getRemindersByExperiment(experimentId)
        val files = entries.map { it.id }.takeIf(List<Int>::isNotEmpty)
            ?.let { db.experimentEntryFileDao.getFilesByEntries(it) }
            .orEmpty()
        val floor = buildList {
            add(parentDeleteVersion)
            entries.forEach { add(it.entityVersionAt()) }
            reminders.forEach { add(it.entityVersionAt()) }
            files.forEach { add(it.entityVersionAt()) }
        }.maxOrNull()
        val deletedAt = mobileUpdatedAt(floor)
        files.forEach {
            db.experimentEntryFileDao.upsert(it.copy(updatedAt = deletedAt, deletedAt = deletedAt))
        }
        reminders.forEach {
            db.experimentReminderDao.upsert(it.copy(updatedAt = deletedAt, deletedAt = deletedAt))
        }
        entries.forEach {
            db.experimentEntryDao.upsert(it.copy(updatedAt = deletedAt, deletedAt = deletedAt))
        }
    }
}

private fun MobileExperimentEntity.toMirrorRow() = MobileExperimentMirrorRow(
    syncId = syncId,
    title = title,
    ideaDescription = ideaDescription,
    isArchived = isArchived,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun MobileExperimentEntryEntity.toMirrorRow(experimentSyncId: String) = MobileExperimentEntryMirrorRow(
    syncId = syncId,
    experimentSyncId = experimentSyncId,
    entryDate = entryDate,
    createdAt = createdAt,
    content = content,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun MobileExperimentReminderEntity.toMirrorRow(experimentSyncId: String) = MobileExperimentReminderMirrorRow(
    syncId = syncId,
    experimentSyncId = experimentSyncId,
    text = text,
    reminderDateTime = reminderDateTime,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun MobileExperimentEntryFileEntity.toMirrorRow(
    entrySyncId: String,
    config: MobileS3Config,
) = MobileFileMirrorRow(
    syncId = syncId,
    ownerType = MobileFileOwnerType.EXPERIMENT_ENTRY,
    ownerSyncId = entrySyncId,
    displayName = displayName,
    path = localPath,
    remoteObjectKey = config.normalizeObjectKey(objectKey),
    remoteStorageProvider = "S3",
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun tombstoneOrder(change: MobileMirrorChange): Int {
    return if (change.row.deletedAt == null) 0 else 1
}

/** Возвращает фактическую sync-версию локального эксперимента. */
private fun MobileExperimentEntity.entityVersionAt(): LocalDateTime =
    deletedAt?.takeIf { it > updatedAt } ?: updatedAt

/** Возвращает фактическую sync-версию локальной записи журнала. */
private fun MobileExperimentEntryEntity.entityVersionAt(): LocalDateTime =
    deletedAt?.takeIf { it > updatedAt } ?: updatedAt

/** Возвращает фактическую sync-версию локального напоминания. */
private fun MobileExperimentReminderEntity.entityVersionAt(): LocalDateTime =
    deletedAt?.takeIf { it > updatedAt } ?: updatedAt

/** Возвращает фактическую sync-версию локальных метаданных файла. */
private fun MobileExperimentEntryFileEntity.entityVersionAt(): LocalDateTime =
    deletedAt?.takeIf { it > updatedAt } ?: updatedAt
