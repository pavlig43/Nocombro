package ru.pavlig43.nocombro.mobile.sync

/**
 * Выбирает winners между local и remote snapshot по `updatedAt/deletedAt`.
 */
class MobileReconciliationPlanner {
    /**
     * Возвращает изменения к отправке и получению без изменения данных.
     */
    fun plan(
        local: MobileMirrorSnapshot,
        remote: MobileMirrorSnapshot,
    ): MobileSyncPlan {
        val push = mutableListOf<MobileMirrorChange>()
        val pull = mutableListOf<MobileMirrorChange>()

        MobileMirrorTable.entries.sortedBy(MobileMirrorTable::order).forEach { table ->
            val localRows = local.rowsByTable[table].orEmpty().associateBy(MobileMirrorRow::syncId)
            val remoteRows = remote.rowsByTable[table].orEmpty().associateBy(MobileMirrorRow::syncId)
            (localRows.keys + remoteRows.keys).sorted().forEach { syncId ->
                val localRow = localRows[syncId]
                val remoteRow = remoteRows[syncId]
                when {
                    localRow == null && remoteRow != null && remoteRow.deletedAt == null ->
                        pull += MobileMirrorChange(table, remoteRow)
                    localRow != null && remoteRow == null ->
                        push += MobileMirrorChange(table, localRow)
                    localRow != null && remoteRow != null && localRow.versionAt() > remoteRow.versionAt() ->
                        push += MobileMirrorChange(table, localRow)
                    localRow != null && remoteRow != null && remoteRow.versionAt() > localRow.versionAt() ->
                        pull += MobileMirrorChange(table, remoteRow)
                }
            }
        }

        return MobileSyncPlan(pushChanges = push, pullChanges = pull)
    }
}
