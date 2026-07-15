package ru.pavlig43.database.data.sync.mirror

/**
 * Результат двустороннего сравнения локального и удаленного snapshot.
 *
 * Списки взаимоисключающие для одной пары `table + sync_id`: строка либо должна
 * победить удалённо через [pushChanges], либо локально через [pullChanges], либо
 * требовать ручного выбора через [conflicts], либо уже быть согласованной.
 */
data class MirrorReconciliationPlan(
    val pushChanges: List<MirrorPushEntityChange>,
    val pullChanges: List<MirrorPushEntityChange>,
    val conflicts: List<MirrorVersionConflict> = emptyList(),
)

/**
 * Две строки одной сущности с равной версией и разным sync-содержимым.
 *
 * @param table typed mirror-таблица спорной сущности.
 * @param localRow вариант из Room и deletion journal.
 * @param remoteRow вариант, прочитанный из YDB.
 */
data class MirrorVersionConflict(
    val table: MirrorSyncTable,
    val localRow: MirrorSyncRow,
    val remoteRow: MirrorSyncRow,
)

/**
 * Детерминированно выбирает победившую версию каждой mirror-строки.
 *
 * Identity строки задается парой [MirrorSyncTable] и [MirrorSyncRow.syncId].
 * Версией считается более позднее из `updatedAt` и `deletedAt`. При равных версиях
 * одинаковые строки считаются согласованными, а разные попадают в конфликт.
 *
 * Remote tombstone без локальной строки игнорируется. Локально удалять уже
 * отсутствующую сущность не требуется, а tombstone продолжает храниться remote.
 */
class MirrorReconciliationPlanner {

    /**
     * Сравнивает все зарегистрированные business tables и строит план синхронизации.
     *
     * @param localSnapshot Room snapshot, уже объединенный с deletion journal.
     * @param remoteSnapshot typed snapshot из remote gateway.
     * @return план, не изменяющий ни локальное, ни удаленное состояние.
     */
    fun plan(
        localSnapshot: MirrorLocalSnapshot,
        remoteSnapshot: MirrorRemoteSnapshot,
    ): MirrorReconciliationPlan {
        val pushChanges = mutableListOf<MirrorPushEntityChange>()
        val pullChanges = mutableListOf<MirrorPushEntityChange>()
        val conflicts = mutableListOf<MirrorVersionConflict>()

        MirrorSyncTable.mirroredBusinessTables.forEach { table ->
            val localBySyncId = localSnapshot.rowsByTable[table].orEmpty().associateBy(MirrorSyncRow::syncId)
            val remoteBySyncId = remoteSnapshot.rowsByTable[table].orEmpty().associateBy(MirrorSyncRow::syncId)

            // Сортировка делает план воспроизводимым для тестов, логов и batch-записи.
            (localBySyncId.keys + remoteBySyncId.keys).sorted().forEach { syncId ->
                val local = localBySyncId[syncId]
                val remote = remoteBySyncId[syncId]
                when {
                    local == null && remote != null && remote.deletedAt == null ->
                        pullChanges += MirrorPushEntityChange(table, remote)
                    local != null && remote == null -> pushChanges += MirrorPushEntityChange(table, local)
                    local != null && remote != null && local.versionAt() > remote.versionAt() ->
                        pushChanges += MirrorPushEntityChange(table, local)
                    local != null && remote != null && remote.versionAt() > local.versionAt() ->
                        pullChanges += MirrorPushEntityChange(table, remote)
                    local != null && remote != null && !local.hasSameSyncContent(remote) ->
                        conflicts += MirrorVersionConflict(table, local, remote)
                }
            }
        }

        return MirrorReconciliationPlan(
            pushChanges = pushChanges,
            pullChanges = pullChanges.filterBlockedActiveRows(localSnapshot, remoteSnapshot),
            conflicts = conflicts,
        )
    }
}

/** Возвращает логическую версию строки с учетом более позднего tombstone. */
internal fun MirrorSyncRow.versionAt() = deletedAt?.takeIf { it > updatedAt } ?: updatedAt

private fun List<MirrorPushEntityChange>.filterBlockedActiveRows(
    localSnapshot: MirrorLocalSnapshot,
    remoteSnapshot: MirrorRemoteSnapshot,
): List<MirrorPushEntityChange> {
    val winners = buildWinnerRows(localSnapshot, remoteSnapshot)
    val blockedKeys = winners
        .filterValues { row -> row.deletedAt != null }
        .keys
        .toMutableSet()
    val activeChanges = filter { change -> change.row.deletedAt == null }
    val knownTables = localSnapshot.rowsByTable.keys + remoteSnapshot.rowsByTable.keys

    do {
        var changed = false
        activeChanges.forEach { change ->
            val key = change.entityKey()
            if (key in blockedKeys) return@forEach
            val isBlocked = change.row.dependencyKeys().any { dependency ->
                dependency.table in knownTables && (dependency in blockedKeys || dependency !in winners)
            }
            if (isBlocked) {
                blockedKeys += key
                changed = true
            }
        }
    } while (changed)

    return filterNot { change ->
        change.row.deletedAt == null && change.entityKey() in blockedKeys
    }
}

private fun buildWinnerRows(
    localSnapshot: MirrorLocalSnapshot,
    remoteSnapshot: MirrorRemoteSnapshot,
): Map<MirrorEntityKey, MirrorSyncRow> {
    val localRows = localSnapshot.rowsByTable.toEntityMap()
    val remoteRows = remoteSnapshot.rowsByTable.toEntityMap()
    return (localRows.keys + remoteRows.keys).associateWith { key ->
        val local = localRows[key]
        val remote = remoteRows[key]
        when {
            local == null -> requireNotNull(remote)
            remote == null -> local
            remote.versionAt() > local.versionAt() -> remote
            else -> local
        }
    }
}

private fun Map<MirrorSyncTable, List<MirrorSyncRow>>.toEntityMap(): Map<MirrorEntityKey, MirrorSyncRow> =
    entries.flatMap { (table, rows) ->
        rows.map { row -> MirrorEntityKey(table, row.syncId) to row }
    }.toMap()
