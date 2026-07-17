package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import kotlin.time.TimeSource

/**
 * Координирует snapshot-based reconciliation между Room и remote mirror.
 *
 * Сервис отделяет orchestration от транспорта и локального применения: snapshot
 * строит [MirrorLocalSnapshotRepository], победителей выбирает
 * [MirrorReconciliationPlanner], remote I/O выполняет [MirrorSyncRemoteGateway],
 * а pull применяет [MirrorLocalApplyRepository].
 *
 * Операции push и pull намеренно однонаправленные. Push записывает только локальных
 * победителей и не применяет обнаруженные remote changes; pull делает обратное.
 */
@Suppress("TooManyFunctions")
class MirrorReconciliationService(
    private val localSnapshotRepository: MirrorLocalSnapshotRepository,
    private val remoteGateway: MirrorSyncRemoteGateway,
    private val planner: MirrorReconciliationPlanner,
    private val localApplyRepository: MirrorLocalApplyRepository,
) {
    /** Возвращает низкоуровневый статус remote gateway без загрузки snapshot. */
    suspend fun getStatus(): MirrorRemoteStatus = remoteGateway.getStatus()

    /** Возвращает локальный статус настроек без подключения и проверки таблиц. */
    suspend fun getConfigurationStatus(): MirrorRemoteStatus = remoteGateway.getConfigurationStatus()

    /**
     * Загружает согласованный набор входных данных для полного цикла синхронизации.
     *
     * Метод проверяет локальные настройки, читает Room и YDB, затем строит план,
     * но ещё ничего не записывает. Полученный контекст можно передать в
     * [executePreparedSync].
     */
    suspend fun prepareSync(): Result<MirrorPreparedSyncContext> = runCatching {
        val configuration = remoteGateway.getConfigurationStatus()
        require(configuration.configured) { configuration.error ?: "Mirror sync is not configured" }
        require(configuration.error == null) { configuration.error ?: "Mirror remote is unavailable" }

        val local = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val remoteMark = TimeSource.Monotonic.markNow()
        val remote = remoteGateway.loadRemoteSnapshot().getOrElse { throw stageFailure("remote snapshot", it) }
        SyncStageLog.completed("remote snapshot", remoteMark.elapsedNow().inWholeMilliseconds)
        val plannerMark = TimeSource.Monotonic.markNow()
        val plan = planner.plan(local, remote)
        SyncStageLog.completed("planner", plannerMark.elapsedNow().inWholeMilliseconds)
        MirrorPreparedSyncContext(configuration, local, remote, plan)
    }

    /**
     * Выполняет подготовленный push, применяет remote winners и считает остаток.
     *
     * Отклонённый push вызывает не более одного повторного чтения YDB. Для pull
     * берётся план после этой проверки, поэтому конкурентно обновлённая remote
     * строка не теряется и может быть применена к Room в том же цикле.
     */
    suspend fun executePreparedSync(context: MirrorPreparedSyncContext): Result<MirrorReconciliationRun> = runCatching {
        val pushMark = TimeSource.Monotonic.markNow()
        val pushOutcome = pushWithSingleRemoteRefresh(
            initialLocal = context.localSnapshot,
            initialRemote = context.remoteSnapshot,
            initialPlan = context.plan,
        )
        SyncStageLog.completed("push", pushMark.elapsedNow().inWholeMilliseconds)

        val applyMark = TimeSource.Monotonic.markNow()
        localApplyRepository.apply(pushOutcome.effectivePlan.pullChanges)
        SyncStageLog.completed("Room apply", applyMark.elapsedNow().inWholeMilliseconds)
        val refreshedLocal = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val remainingPlan = planner.plan(refreshedLocal, pushOutcome.effectiveRemote)
        MirrorReconciliationRun(
            configured = true,
            completedAt = pushOutcome.completedAt,
            pushedChanges = pushOutcome.acceptedChanges.size,
            pulledChanges = pushOutcome.effectivePlan.pullChanges.size,
            remainingPushChanges = remainingPlan.pushChanges.size,
            remainingPullChanges = remainingPlan.pullChanges.size,
            conflicts = remainingPlan.conflicts,
        )
    }

    /**
     * Загружает локальный и удаленный snapshot и строит план без изменения данных.
     */
    suspend fun buildPreview(): Result<MirrorReconciliationPreview> = runCatching {
        val status = remoteGateway.getConfigurationStatus()
        require(status.configured) { status.error ?: "Mirror sync is not configured" }
        require(status.error == null) { status.error ?: "Mirror remote is unavailable" }

        val local = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val remote = remoteGateway.loadRemoteSnapshot().getOrThrow()
        MirrorReconciliationPreview(
            localSnapshot = local,
            remoteSnapshot = remote,
            plan = planner.plan(local, remote),
        )
    }

    /**
     * Загружает оба snapshot и подсчитывает расхождения без изменения данных.
     *
     * Если transport не настроен или cheap configuration check уже содержит
     * ошибку, тяжелая загрузка snapshot не выполняется. Ошибки сравнения
     * преобразуются в status.error.
     */
    suspend fun getSyncStatus(): MirrorSyncStatus {
        val configuration = remoteGateway.getConfigurationStatus()
        if ((!configuration.configured) || (configuration.error != null)) {
            return MirrorSyncStatus(status = configuration)
        }

        return runCatching {
            val local = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
            val remote = remoteGateway.loadRemoteSnapshot().getOrThrow()
            val plan = planner.plan(local, remote)
            MirrorSyncStatus(
                status = configuration.copy(
                    availableTables = remote.rowsByTable.keys.mapTo(mutableSetOf()) { it.tableName },
                    checkedAt = remote.loadedAt,
                    error = null,
                ),
                pushChangesCount = plan.pushChanges.size,
                pullChangesCount = plan.pullChanges.size,
                conflicts = plan.conflicts,
            )
        }.getOrElse { throwable ->
            MirrorSyncStatus(
                status = configuration.copy(
                    error = throwable.message ?: "Mirror status snapshot failed",
                ),
            )
        }
    }

    /**
     * Отправляет в remote только локальные строки, победившие по версии.
     *
     * Успешный результат также сообщает число remote winners, замеченных во время
     * сравнения, но не применяет их локально.
     */
    suspend fun pushLocalWinners(): Result<MirrorReconciliationRun> = runCatching {
        val status = remoteGateway.getConfigurationStatus()
        if (!status.configured) return@runCatching MirrorReconciliationRun.skipped(status.checkedAt)
        require(status.error == null) { status.error ?: "Mirror remote is unavailable" }

        val local = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val remote = remoteGateway.loadRemoteSnapshot().getOrThrow()
        val plan = planner.plan(local, remote)
        val pushOutcome = pushWithSingleRemoteRefresh(local, remote, plan)
        MirrorReconciliationRun(
            configured = true,
            completedAt = pushOutcome.completedAt,
            pushedChanges = pushOutcome.acceptedChanges.size,
            pulledChanges = pushOutcome.effectivePlan.pullChanges.size,
            remainingPushChanges = pushOutcome.effectivePlan.pushChanges.size,
            remainingPullChanges = pushOutcome.effectivePlan.pullChanges.size,
            conflicts = pushOutcome.effectivePlan.conflicts,
        )
    }

    /**
     * После первого отказа перечитывает обе стороны и пересчитывает план один раз.
     *
     * Если локальная версия всё ещё побеждает, выполняется одна повторная условная
     * запись. Второй отказ завершает операцию ошибкой с таблицей и `sync_id`, но без
     * пользовательского содержимого.
     *
     * @return принятые строки, фактический remote-снимок и план после push.
     */
    @Suppress("ReturnCount", "ThrowsCount")
    private suspend fun pushWithSingleRemoteRefresh(
        initialLocal: MirrorLocalSnapshot,
        initialRemote: MirrorRemoteSnapshot,
        initialPlan: MirrorReconciliationPlan,
    ): MirrorPushOutcome {
        if (initialPlan.pushChanges.isEmpty()) {
            return MirrorPushOutcome(
                acceptedChanges = emptyList(),
                effectiveRemote = initialRemote,
                effectivePlan = initialPlan,
                completedAt = initialRemote.loadedAt,
            )
        }

        val firstResult = remoteGateway.pushMirrorState(initialPlan.pushChanges)
            .getOrElse { throw stageFailure("push", it) }
        val acceptedChanges = firstResult.acceptedOrLegacy(initialPlan.pushChanges).toMutableList()
        if (firstResult.rejectedChanges.isEmpty()) {
            val effectiveRemote = initialRemote.withAppliedChanges(acceptedChanges)
            return MirrorPushOutcome(
                acceptedChanges = acceptedChanges,
                effectiveRemote = effectiveRemote,
                effectivePlan = planner.plan(initialLocal, effectiveRemote),
                completedAt = firstResult.pushedAt,
            )
        }

        val refreshedRemote = remoteGateway.loadRemoteSnapshot()
            .getOrElse { throw stageFailure("remote snapshot after rejected push", it) }
        val refreshedLocal = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val refreshedPlan = planner.plan(refreshedLocal, refreshedRemote)
        if (refreshedPlan.pushChanges.isEmpty()) {
            return MirrorPushOutcome(
                acceptedChanges = acceptedChanges,
                effectiveRemote = refreshedRemote,
                effectivePlan = refreshedPlan,
                completedAt = refreshedRemote.loadedAt,
            )
        }

        val retryResult = remoteGateway.pushMirrorState(refreshedPlan.pushChanges)
            .getOrElse { throw stageFailure("push retry", it) }
        if (retryResult.rejectedChanges.isNotEmpty()) {
            throw IllegalStateException(retryResult.rejectedChanges.secondRejectionMessage())
        }
        val retryAccepted = retryResult.acceptedOrLegacy(refreshedPlan.pushChanges)
        acceptedChanges += retryAccepted
        val effectiveRemote = refreshedRemote.withAppliedChanges(retryAccepted)
        return MirrorPushOutcome(
            acceptedChanges = acceptedChanges,
            effectiveRemote = effectiveRemote,
            effectivePlan = planner.plan(refreshedLocal, effectiveRemote),
            completedAt = retryResult.pushedAt,
        )
    }

    /**
     * Применяет к Room только удаленные строки, победившие по версии.
     *
     * Изменения применяются одной локальной транзакцией с учетом зависимостей и
     * предварительным сохранением remote tombstone в deletion journal.
     */
    suspend fun pullRemoteWinners(): Result<MirrorReconciliationRun> = runCatching {
        val status = remoteGateway.getConfigurationStatus()
        if (!status.configured) return@runCatching MirrorReconciliationRun.skipped(status.checkedAt)
        require(status.error == null) { status.error ?: "Mirror remote is unavailable" }

        val local = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val remote = remoteGateway.loadRemoteSnapshot().getOrThrow()
        val plan = planner.plan(local, remote)
        localApplyRepository.apply(plan.pullChanges)
        MirrorReconciliationRun(
            configured = true,
            completedAt = remote.loadedAt,
            pushedChanges = 0,
            pulledChanges = plan.pullChanges.size,
            conflicts = plan.conflicts,
        )
    }

    /**
     * Делает локальную базу эталоном для disaster recovery remote mirror.
     *
     * Все локальные строки отправляются как есть. Активные remote-строки, которых
     * нет локально, не удаляются физически, а превращаются в tombstone с единым
     * временем [MirrorRemoteRebuildResult.rebuiltAt], строго более новым любой
     * удаляемой строки. Любой отказ условной записи останавливает пересборку: более
     * свежие remote-данные нельзя молча считать заменёнными.
     */
    suspend fun rebuildRemoteFromLocal(): Result<MirrorRemoteRebuildResult> = runCatching {
        val status = remoteGateway.getConfigurationStatus()
        require(status.configured) { status.error ?: "Mirror sync is not configured" }
        require(status.error == null) { status.error ?: "Mirror remote is unavailable" }

        val local = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
        val remote = remoteGateway.loadRemoteSnapshot().getOrThrow()
        val localChanges = MirrorSyncTable.mirroredBusinessTables.flatMap { table ->
            local.rowsByTable[table].orEmpty().map { row -> MirrorPushEntityChange(table, row) }
        }
        val rowsToTombstone = MirrorSyncTable.mirroredBusinessTables.flatMap { table ->
            val localIds = local.rowsByTable[table].orEmpty().mapTo(mutableSetOf(), MirrorSyncRow::syncId)
            remote.rowsByTable[table].orEmpty()
                .filter { it.syncId !in localIds && it.deletedAt == null }
                .map { row -> MirrorPushEntityChange(table, row) }
        }
        val tombstonedAt = defaultUpdatedAt(rowsToTombstone.maxOfOrNull { it.row.versionAt() })
        val tombstones = rowsToTombstone.map { change ->
            change.copy(row = change.row.markDeleted(tombstonedAt))
        }
        val pushResult = remoteGateway.pushMirrorState(localChanges + tombstones).getOrThrow()
        check(pushResult.rejectedChanges.isEmpty()) {
            "Remote rebuild rejected ${pushResult.rejectedChanges.size} newer rows"
        }

        MirrorRemoteRebuildResult(
            rebuiltAt = tombstonedAt,
            pushedRows = localChanges.size,
            tombstonedRows = tombstones.size,
        )
    }

    /**
     * Перечитывает конфликт и сохраняет выбранное пользователем содержимое.
     *
     * Выбор получает версию строго новее обеих сторон. Локальная строка меняется
     * через compare-and-set; если Room уже изменился, возвращается [MirrorConflictResolutionResult.Stale].
     * Затем та же строка условно пишется в YDB. Повторный отказ возвращает свежий
     * конфликт и не выдаётся за успешное разрешение.
     *
     * @param conflict пара строк, которую видел пользователь.
     * @param winner сторона, чьё содержимое нужно сохранить.
     */
    suspend fun resolveConflict(
        conflict: MirrorVersionConflict,
        winner: MirrorConflictWinner,
    ): Result<MirrorConflictResolutionResult> = runCatching {
        val current = loadConflictRows(conflict.table, conflict.localRow.syncId)
            ?: return@runCatching MirrorConflictResolutionResult.Stale
        if (!current.hasSameRows(conflict)) {
            return@runCatching MirrorConflictResolutionResult.Stale
        }

        val newestVersion = maxOf(current.localRow.versionAt(), current.remoteRow.versionAt())
        val selected = when (winner) {
            MirrorConflictWinner.LOCAL -> current.localRow
            MirrorConflictWinner.REMOTE -> current.remoteRow
        }.withSyncVersion(defaultUpdatedAt(newestVersion))
        val change = MirrorPushEntityChange(current.table, selected)
        val localApplied = localApplyRepository.applyIfCurrentMatches(
            expected = MirrorPushEntityChange(current.table, current.localRow),
            change = change,
        )
        if (!localApplied) {
            return@runCatching MirrorConflictResolutionResult.Stale
        }

        val push = remoteGateway.pushMirrorState(listOf(change)).getOrThrow()
        if (push.rejectedChanges.isEmpty()) {
            return@runCatching MirrorConflictResolutionResult.Resolved
        }

        val rejected = loadConflictRows(current.table, current.localRow.syncId)
            ?: error(
                "Conflict rows disappeared after rejected YDB write: " +
                    "table=${current.table.tableName}, sync_id=${current.localRow.syncId}"
            )
        MirrorConflictResolutionResult.Rejected(rejected)
    }

    /**
     * Загружает актуальную пару спорных строк из одной таблицы.
     *
     * @return конфликт либо `null`, если одна сторона исчезла или строки уже равны.
     */
    @Suppress("ReturnCount", "UnreachableCode")
    private suspend fun loadConflictRows(
        table: MirrorSyncTable,
        syncId: String,
    ): MirrorVersionConflict? {
        val local = localSnapshotRepository.loadSnapshot(listOf(table))
            .rowsByTable[table].orEmpty()
            .firstOrNull { it.syncId == syncId }
            ?: return null
        val remote = remoteGateway.loadRemoteSnapshot(listOf(table))
            .getOrElse { throw stageFailure("remote conflict snapshot", it) }
            .rowsByTable[table].orEmpty()
            .firstOrNull { it.syncId == syncId }
            ?: return null
        if (local.hasSameSyncContent(remote)) return null
        return MirrorVersionConflict(table, local, remote)
    }
}

/**
 * Неизменяемые входные данные, собранные перед записью полного sync-цикла.
 */
data class MirrorPreparedSyncContext(
    val configuration: MirrorRemoteStatus,
    val localSnapshot: MirrorLocalSnapshot,
    val remoteSnapshot: MirrorRemoteSnapshot,
    val plan: MirrorReconciliationPlan,
)

/** Результат push после возможного одного повторного чтения remote. */
private data class MirrorPushOutcome(
    val acceptedChanges: List<MirrorPushEntityChange>,
    val effectiveRemote: MirrorRemoteSnapshot,
    val effectivePlan: MirrorReconciliationPlan,
    val completedAt: LocalDateTime,
)

private fun stageFailure(stage: String, throwable: Throwable): IllegalStateException =
    IllegalStateException("Mirror $stage failed: ${throwable.message ?: throwable::class.simpleName}", throwable)

/** Строит безопасный текст второго отказа без пользовательских полей строки. */
private fun List<MirrorPushRejection>.secondRejectionMessage(): String {
    val rows = joinToString { rejection ->
        "table=${rejection.change.table.tableName}, sync_id=${rejection.change.row.syncId}"
    }
    return "Mirror push was rejected after one remote refresh: $rows"
}

private object SyncStageLog {
    private val logger = java.util.logging.Logger.getLogger("MirrorSync")
    fun completed(stage: String, milliseconds: Long) {
        logger.fine("Mirror sync stage=$stage durationMs=$milliseconds")
    }
}

/** Сторона, выбранная пользователем как источник содержимого конфликта. */
enum class MirrorConflictWinner { LOCAL, REMOTE }

/** Итог попытки разрешить конфликт с защитой от конкурентных правок. */
sealed interface MirrorConflictResolutionResult {
    /** Обе стороны приняли выбранное содержимое с новой версией. */
    data object Resolved : MirrorConflictResolutionResult
    /** Исходные строки успели измениться или исчезнуть до записи. */
    data object Stale : MirrorConflictResolutionResult
    /** YDB отклонила запись; [conflict] содержит заново прочитанную пару. */
    data class Rejected(val conflict: MirrorVersionConflict) : MirrorConflictResolutionResult
}

/**
 * Проверяет, что конфликт всё ещё описывает те же локальную и удалённую строки.
 *
 * Сравнивается синхронизируемое содержимое, а не только `sync_id`: это защищает
 * разрешение конфликта от записи поверх данных, изменившихся после показа пользователю.
 */
private fun MirrorVersionConflict.hasSameRows(other: MirrorVersionConflict): Boolean =
    table == other.table &&
        localRow.hasSameSyncContent(other.localRow) &&
        remoteRow.hasSameSyncContent(other.remoteRow)

/**
 * Итог одной операции сверки Room и remote mirror.
 *
 * [conflicts] не входят в счётчики push/pull: равные версии с разным содержимым
 * требуют отдельного выбора пользователя.
 */
data class MirrorReconciliationRun(
    val configured: Boolean,
    val completedAt: LocalDateTime,
    val pushedChanges: Int,
    val pulledChanges: Int,
    val remainingPushChanges: Int = 0,
    val remainingPullChanges: Int = 0,
    val conflicts: List<MirrorVersionConflict> = emptyList(),
) {
    companion object {
        /** Создает успешный no-op результат для установки без remote-конфигурации. */
        fun skipped(at: LocalDateTime) = MirrorReconciliationRun(
            configured = false,
            completedAt = at,
            pushedChanges = 0,
            pulledChanges = 0,
        )
    }
}

private fun MirrorRemoteSnapshot.withAppliedChanges(
    changes: List<MirrorPushEntityChange>,
): MirrorRemoteSnapshot {
    if (changes.isEmpty()) return this

    val changesByTable = changes.groupBy(MirrorPushEntityChange::table)
    return copy(
        rowsByTable = rowsByTable + changesByTable.mapValues { (table, tableChanges) ->
            val rowsBySyncId = rowsByTable[table].orEmpty()
                .associateByTo(linkedMapOf(), MirrorSyncRow::syncId)
            tableChanges.forEach { change -> rowsBySyncId[change.row.syncId] = change.row }
            rowsBySyncId.values.toList()
        },
    )
}

/** Статистика полной пересборки remote mirror из локального snapshot. */
data class MirrorRemoteRebuildResult(
    val rebuiltAt: LocalDateTime,
    val pushedRows: Int,
    val tombstonedRows: Int,
)

/** Read-only результат сравнения Room и remote mirror. */
data class MirrorReconciliationPreview(
    val localSnapshot: MirrorLocalSnapshot,
    val remoteSnapshot: MirrorRemoteSnapshot,
    val plan: MirrorReconciliationPlan,
)

/**
 * Статус gateway, число победителей на каждой стороне и ручные конфликты.
 */
data class MirrorSyncStatus(
    val status: MirrorRemoteStatus,
    val pushChangesCount: Int = 0,
    val pullChangesCount: Int = 0,
    val conflicts: List<MirrorVersionConflict> = emptyList(),
) {
    /** Есть ли хотя бы одна remote-версия, которую следует применить локально. */
    val hasRemoteChanges: Boolean
        get() = pullChangesCount > 0
}

/**
 * Поддерживает старые тестовые gateway, которые не заполняют списки результата.
 *
 * Пустые списки при отсутствии отказов трактуются как принятие всего запроса.
 */
private fun MirrorPushResult.acceptedOrLegacy(
    requested: List<MirrorPushEntityChange>,
): List<MirrorPushEntityChange> {
    return if (acceptedChanges.isEmpty() && rejectedChanges.isEmpty()) requested else acceptedChanges
}

/**
 * Создает typed tombstone, сохраняя исходный payload и меняя только sync metadata.
 */
@Suppress("CyclomaticComplexMethod")
internal fun MirrorSyncRow.markDeleted(at: LocalDateTime): MirrorSyncRow = when (this) {
    is VendorMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is DocumentMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is DeclarationMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ProductMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ProductSpecificationMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is SafetyStockMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is CompositionMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ProductDeclarationMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is BatchMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is BatchCostPriceMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is BatchMovementMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is BuyMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is SaleMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ReminderMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ExpenseMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ExperimentMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ExperimentEntryMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is ExperimentReminderMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is TransactionMirrorRow -> copy(updatedAt = at, deletedAt = at)
    is FileMirrorRow -> copy(updatedAt = at, deletedAt = at)
}

/**
 * Меняет логическую версию, сохраняя содержимое и состояние удаления строки.
 *
 * Активная строка получает новый `updatedAt`. Tombstone сохраняет признак удаления
 * и получает одинаковые новые `updatedAt` и `deletedAt` через [markDeleted].
 */
@Suppress("CyclomaticComplexMethod")
internal fun MirrorSyncRow.withSyncVersion(at: LocalDateTime): MirrorSyncRow {
    if (deletedAt != null) return markDeleted(at)
    return when (this) {
        is VendorMirrorRow -> copy(updatedAt = at)
        is DocumentMirrorRow -> copy(updatedAt = at)
        is DeclarationMirrorRow -> copy(updatedAt = at)
        is ProductMirrorRow -> copy(updatedAt = at)
        is ProductSpecificationMirrorRow -> copy(updatedAt = at)
        is SafetyStockMirrorRow -> copy(updatedAt = at)
        is CompositionMirrorRow -> copy(updatedAt = at)
        is ProductDeclarationMirrorRow -> copy(updatedAt = at)
        is BatchMirrorRow -> copy(updatedAt = at)
        is BatchCostPriceMirrorRow -> copy(updatedAt = at)
        is BatchMovementMirrorRow -> copy(updatedAt = at)
        is BuyMirrorRow -> copy(updatedAt = at)
        is SaleMirrorRow -> copy(updatedAt = at)
        is ReminderMirrorRow -> copy(updatedAt = at)
        is ExpenseMirrorRow -> copy(updatedAt = at)
        is ExperimentMirrorRow -> copy(updatedAt = at)
        is ExperimentEntryMirrorRow -> copy(updatedAt = at)
        is ExperimentReminderMirrorRow -> copy(updatedAt = at)
        is TransactionMirrorRow -> copy(updatedAt = at)
        is FileMirrorRow -> copy(updatedAt = at)
    }
}
