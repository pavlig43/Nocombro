package ru.pavlig43.nocombro.mobile.sync

/**
 * Выбирает победившие строки между локальным и удалённым снимком.
 *
 * Для каждой строки сравнивается версия: `deletedAt`, если tombstone новее,
 * иначе `updatedAt`. Метод только строит план и не меняет Room/YDB/S3.
 */
class MobileReconciliationPlanner {
    /**
     * Возвращает изменения к отправке и получению без записи данных.
     *
     * Строка с большей [MobileMirrorRow.versionAt] становится победителем. Равные
     * версии с разным переносимым содержимым не перезаписываются автоматически:
     * они попадают в [MobileSyncPlan.conflicts]. Локальный путь файла исключён из
     * сравнения, поскольку он всегда различается между устройствами.
     *
     * @param local полный снимок поддерживаемых строк Room.
     * @param remote нормализованный снимок тех же строк из YDB.
     * @return неизменяемый план push, pull и конфликтов равных версий.
     */
    fun plan(
        local: MobileMirrorSnapshot,
        remote: MobileMirrorSnapshot,
    ): MobileSyncPlan {
        val push = mutableListOf<MobileMirrorChange>()
        val pull = mutableListOf<MobileMirrorChange>()
        val conflicts = mutableListOf<MobileVersionConflict>()

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
                    localRow != null && remoteRow != null && !localRow.hasSameSyncContent(remoteRow) ->
                        conflicts += MobileVersionConflict(table, localRow, remoteRow)
                }
            }
        }

        return MobileSyncPlan(pushChanges = push, pullChanges = pull, conflicts = conflicts)
    }
}
