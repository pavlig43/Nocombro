package ru.pavlig43.nocombro.mobile.sync

import androidx.room.withTransaction
import java.io.File
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryFileEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentReminderEntity

/**
 * Строит mobile snapshot из локальной Room-БД и применяет remote winners обратно.
 */
class MobileLocalMirrorRepository(
    private val db: NocombroMobileDatabase,
    private val filesDirPath: String,
) {
    /**
     * Читает локальные experiments, entries, reminders и files как mirror rows.
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
     * Применяет remote changes одной транзакцией с учётом порядка зависимостей.
     */
    suspend fun applyRemoteChanges(changes: List<MobileMirrorChange>) {
        db.withTransaction {
            val ordered = changes.sortedWith(
                compareBy<MobileMirrorChange> { tombstoneOrder(it) }
                    .thenBy { it.table.order }
                    .thenBy { it.row.syncId },
            )
            ordered.forEach { change ->
                when (val row = change.row) {
                    is MobileExperimentMirrorRow -> applyExperiment(row)
                    is MobileExperimentEntryMirrorRow -> applyEntry(row)
                    is MobileExperimentReminderMirrorRow -> applyReminder(row)
                    is MobileFileMirrorRow -> applyFile(row)
                }
            }
        }
    }

    /**
     * Строит локальный путь для файла, который пришёл из S3.
     */
    fun localPathForRemoteKey(remoteObjectKey: String): String {
        return File(File(filesDirPath, "nocombro"), remoteObjectKey).absolutePath
    }

    private suspend fun applyExperiment(row: MobileExperimentMirrorRow) {
        val current = db.experimentDao.getExperimentBySyncId(row.syncId)
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

    private suspend fun applyEntry(row: MobileExperimentEntryMirrorRow) {
        val experiment = db.experimentDao.getExperimentBySyncId(row.experimentSyncId) ?: return
        val current = db.experimentEntryDao.getEntryBySyncId(row.syncId)
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

    private suspend fun applyReminder(row: MobileExperimentReminderMirrorRow) {
        val experiment = db.experimentDao.getExperimentBySyncId(row.experimentSyncId) ?: return
        val current = db.experimentReminderDao.getReminderBySyncId(row.syncId)
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

    private suspend fun applyFile(row: MobileFileMirrorRow) {
        if (row.ownerType != MobileFileOwnerType.EXPERIMENT_ENTRY) return
        val entry = db.experimentEntryDao.getEntryBySyncId(row.ownerSyncId) ?: return
        val current = db.experimentEntryFileDao.getFileBySyncId(row.syncId)
        val objectKey = row.remoteObjectKey ?: row.path
        db.experimentEntryFileDao.upsert(
            MobileExperimentEntryFileEntity(
                id = current?.id ?: 0,
                entryId = entry.id,
                syncId = row.syncId,
                displayName = row.displayName,
                localPath = current?.localPath ?: localPathForRemoteKey(objectKey),
                objectKey = objectKey,
                updatedAt = row.updatedAt,
                deletedAt = row.deletedAt,
            )
        )
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
    remoteObjectKey = config.remoteKey(objectKey),
    remoteStorageProvider = "S3",
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun tombstoneOrder(change: MobileMirrorChange): Int {
    return if (change.row.deletedAt == null) 0 else 1
}
