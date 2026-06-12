package ru.pavlig43.database.data.sync.mirror

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction

/**
 * Перехватывает hard delete и сохраняет исчезнувшие строки как typed tombstone.
 *
 * Репозиторий сравнивает database-only snapshot до и после произвольной операции.
 * Это позволяет обнаруживать не только прямое удаление, но и каскады Room/SQLite.
 * Внешняя операция, удаление file metadata и запись журнала выполняются в одной
 * транзакции, поэтому бизнес-данные и tombstone не могут разойтись частично.
 */
class MirrorDeletionJournalRepository(
    private val db: NocombroDatabase,
    private val json: Json = Json { classDiscriminator = "_mirrorType" },
) {
    private val snapshotRepository = MirrorLocalSnapshotRepository(db)

    /**
     * Выполняет [block] в новой Room-транзакции и фиксирует все hard delete.
     *
     * Используется вызывающими слоями, которые еще не открыли транзакцию.
     * Исключение из [block] откатывает и бизнес-изменения, и журнал.
     */
    suspend fun <T> captureHardDeletes(block: suspend () -> T): T {
        return db.inTransaction {
            captureHardDeletesInCurrentTransaction(block)
        }
    }

    /**
     * Перехватывает удаления внутри уже открытой Room-транзакции.
     *
     * Этот вариант нужен transactional wrapper-ам, чтобы не разрывать атомарность
     * валидации, удаления, upsert и создания tombstone.
     */
    suspend fun <T> captureHardDeletesInCurrentTransaction(block: suspend () -> T): T {
        val before = snapshotRepository.loadDatabaseSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val result = block()
        // File metadata удаляется вместе с владельцем и попадет в тот же diff.
        deleteFilesOwnedByDeletedEntities(before)
        val after = snapshotRepository.loadDatabaseSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val deletedAt = defaultUpdatedAt()
        val tombstones = findDeletedRows(before, after)
            .map { change ->
                val row = change.row.markDeleted(deletedAt)
                MirrorDeletionJournalEntity(
                    entityTable = change.table.tableName,
                    syncId = row.syncId,
                    rowJson = json.encodeToString<MirrorSyncRow>(row),
                    deletedAt = deletedAt,
                )
            }

        if (tombstones.isNotEmpty()) {
            db.mirrorDeletionJournalDao.upsert(tombstones)
        }
        return result
    }

    private suspend fun deleteFilesOwnedByDeletedEntities(before: MirrorLocalSnapshot) {
        val tablesWithoutFiles = MirrorSyncTable.mirroredBusinessTables
            .filterNot { it == MirrorSyncTable.FILE }
        val after = snapshotRepository.loadDatabaseSnapshot(tablesWithoutFiles)
        val deletedKeys = tablesWithoutFiles.flatMapTo(mutableSetOf()) { table ->
            val remainingIds = after.rowsByTable[table].orEmpty()
                .mapTo(mutableSetOf(), MirrorSyncRow::syncId)
            before.rowsByTable[table].orEmpty()
                .filter { it.syncId !in remainingIds }
                .map { EntityKey(table, it.syncId) }
        }
        val orphanFileIds = before.rowsByTable[MirrorSyncTable.FILE].orEmpty()
            .filterIsInstance<FileMirrorRow>()
            .filter { file ->
                EntityKey(file.ownerType.ownerMirrorTable(), file.ownerSyncId) in deletedKeys
            }
            .map(FileMirrorRow::syncId)
        if (orphanFileIds.isNotEmpty()) {
            db.fileDao.deleteFilesBySyncIds(orphanFileIds)
        }
    }

    private fun findDeletedRows(
        before: MirrorLocalSnapshot,
        after: MirrorLocalSnapshot,
    ): List<MirrorPushEntityChange> {
        // Сравниваются только физические строки: старые journal tombstone не должны
        // повторно выглядеть как новое удаление.
        return MirrorSyncTable.mirroredBusinessTables.flatMap { table ->
            val afterIds = after.rowsByTable[table].orEmpty()
                .mapTo(mutableSetOf(), MirrorSyncRow::syncId)
            before.rowsByTable[table].orEmpty()
                .filter { it.syncId !in afterIds }
                .map { MirrorPushEntityChange(table, it) }
        }
    }
}

private data class EntityKey(
    val table: MirrorSyncTable,
    val syncId: String,
)

private fun OwnerType.ownerMirrorTable(): MirrorSyncTable = when (this) {
    OwnerType.DECLARATION -> MirrorSyncTable.DECLARATION
    OwnerType.PRODUCT -> MirrorSyncTable.PRODUCT
    OwnerType.VENDOR -> MirrorSyncTable.VENDOR
    OwnerType.DOCUMENT -> MirrorSyncTable.DOCUMENT
    OwnerType.TRANSACTION -> MirrorSyncTable.TRANSACTION
    OwnerType.EXPENSE -> MirrorSyncTable.EXPENSE
    OwnerType.EXPERIMENT_ENTRY -> MirrorSyncTable.EXPERIMENT_ENTRY
}
