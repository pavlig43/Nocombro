package ru.pavlig43.database.data.sync.mirror

data class MirrorStartupCleanupResult(
    val transferredTombstones: Int,
    val deletedRows: Int,
)

class MirrorStartupMaintenance(
    private val db: ru.pavlig43.database.NocombroDatabase,
    private val localApplyRepository: MirrorLocalApplyRepository = MirrorLocalApplyRepository(
        db = db,
        entityApplyRepository = MirrorEntityApplyRepository(db),
        hardDeleteRepository = MirrorHardDeleteRepository(db),
    ),
) {
    suspend fun cleanupSoftDeletedRows(): MirrorStartupCleanupResult {
        val softDeletedRows = MirrorLocalSnapshotRepository(db)
            .loadDatabaseSnapshot(MirrorSyncTable.mirroredBusinessTables)
            .rowsByTable
            .flatMap { (table, rows) ->
                rows.filter { it.deletedAt != null }.map { MirrorPushEntityChange(table, it) }
        }
        if (softDeletedRows.isEmpty()) {
            return MirrorStartupCleanupResult(0, 0)
        }

        val result = localApplyRepository.apply(softDeletedRows)
        return MirrorStartupCleanupResult(
            transferredTombstones = result.persistedTombstones,
            deletedRows = result.deletedRows,
        )
    }
}
