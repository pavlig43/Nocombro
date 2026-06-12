package ru.pavlig43.database.data.sync.mirror

/**
 * Результат двустороннего сравнения локального и удаленного snapshot.
 *
 * Списки взаимоисключающие для одной пары `table + sync_id`: строка либо должна
 * победить удаленно через [pushChanges], либо локально через [pullChanges], либо
 * уже согласована и отсутствует в обоих списках.
 */
data class MirrorReconciliationPlan(
    val pushChanges: List<MirrorPushEntityChange>,
    val pullChanges: List<MirrorPushEntityChange>,
)

/**
 * Детерминированно выбирает победившую версию каждой mirror-строки.
 *
 * Identity строки задается парой [MirrorSyncTable] и [MirrorSyncRow.syncId].
 * Версией считается более позднее из `updatedAt` и `deletedAt`. При равных версиях
 * действие не создается: planner не пытается разрешать конфликт по payload.
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
                }
            }
        }

        return MirrorReconciliationPlan(
            pushChanges = pushChanges,
            pullChanges = pullChanges,
        )
    }
}

/** Возвращает логическую версию строки с учетом более позднего tombstone. */
internal fun MirrorSyncRow.versionAt() = deletedAt?.takeIf { it > updatedAt } ?: updatedAt
