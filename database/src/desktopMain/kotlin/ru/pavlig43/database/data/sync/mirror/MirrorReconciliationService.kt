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
class MirrorReconciliationService(
    private val localSnapshotRepository: MirrorLocalSnapshotRepository,
    private val remoteGateway: MirrorSyncRemoteGateway,
    private val planner: MirrorReconciliationPlanner,
    private val localApplyRepository: MirrorLocalApplyRepository,
) {
    /** Возвращает низкоуровневый статус remote gateway без загрузки snapshot. */
    suspend fun getStatus(): MirrorRemoteStatus = remoteGateway.getStatus()

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

    suspend fun executePreparedSync(context: MirrorPreparedSyncContext): Result<MirrorReconciliationRun> = runCatching {
        val pushMark = TimeSource.Monotonic.markNow()
        if (context.plan.pushChanges.isNotEmpty()) {
            remoteGateway.pushMirrorState(context.plan.pushChanges)
                .getOrElse { throw stageFailure("push", it) }
        }
        SyncStageLog.completed("push", pushMark.elapsedNow().inWholeMilliseconds)

        val applyMark = TimeSource.Monotonic.markNow()
        localApplyRepository.apply(context.plan.pullChanges)
        SyncStageLog.completed("Room apply", applyMark.elapsedNow().inWholeMilliseconds)
        MirrorReconciliationRun(
            configured = true,
            completedAt = context.remoteSnapshot.loadedAt,
            pushedChanges = context.plan.pushChanges.size,
            pulledChanges = context.plan.pullChanges.size,
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
     * Если transport не настроен или status уже содержит ошибку, тяжелая загрузка
     * snapshot не выполняется. Ошибки сравнения преобразуются в status.error.
     */
    suspend fun getSyncStatus(): MirrorSyncStatus {
        val status = remoteGateway.getStatus()
        if (!status.configured || status.error != null) {
            return MirrorSyncStatus(status = status)
        }

        return runCatching {
            val local = localSnapshotRepository.loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
            val remote = remoteGateway.loadRemoteSnapshot().getOrThrow()
            val plan = planner.plan(local, remote)
            MirrorSyncStatus(
                status = status,
                pushChangesCount = plan.pushChanges.size,
                pullChangesCount = plan.pullChanges.size,
            )
        }.getOrElse { throwable ->
            MirrorSyncStatus(
                status = status.copy(
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
        if (plan.pushChanges.isNotEmpty()) {
            remoteGateway.pushMirrorState(plan.pushChanges).getOrThrow()
        }
        MirrorReconciliationRun(
            configured = true,
            completedAt = local.loadedAt,
            pushedChanges = plan.pushChanges.size,
            pulledChanges = plan.pullChanges.size,
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
        )
    }

    /**
     * Делает локальную базу эталоном для disaster recovery remote mirror.
     *
     * Все локальные строки отправляются как есть. Активные remote-строки, которых
     * нет локально, не удаляются физически, а превращаются в tombstone с единым
     * временем [MirrorRemoteRebuildResult.rebuiltAt].
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
        val tombstonedAt = defaultUpdatedAt()
        val tombstones = MirrorSyncTable.mirroredBusinessTables.flatMap { table ->
            val localIds = local.rowsByTable[table].orEmpty().mapTo(mutableSetOf(), MirrorSyncRow::syncId)
            remote.rowsByTable[table].orEmpty()
                .filter { it.syncId !in localIds && it.deletedAt == null }
                .map { row -> MirrorPushEntityChange(table, row.markDeleted(tombstonedAt)) }
        }
        remoteGateway.pushMirrorState(localChanges + tombstones).getOrThrow()

        MirrorRemoteRebuildResult(
            rebuiltAt = tombstonedAt,
            pushedRows = localChanges.size,
            tombstonedRows = tombstones.size,
        )
    }
}

data class MirrorPreparedSyncContext(
    val configuration: MirrorRemoteStatus,
    val localSnapshot: MirrorLocalSnapshot,
    val remoteSnapshot: MirrorRemoteSnapshot,
    val plan: MirrorReconciliationPlan,
)

private fun stageFailure(stage: String, throwable: Throwable): IllegalStateException =
    IllegalStateException("Mirror $stage failed: ${throwable.message ?: throwable::class.simpleName}", throwable)

private object SyncStageLog {
    private val logger = java.util.logging.Logger.getLogger("MirrorSync")
    fun completed(stage: String, milliseconds: Long) {
        logger.info("Mirror sync stage=$stage durationMs=$milliseconds")
    }
}

/** Итог одной однонаправленной reconciliation-операции. */
data class MirrorReconciliationRun(
    val configured: Boolean,
    val completedAt: LocalDateTime,
    val pushedChanges: Int,
    val pulledChanges: Int,
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

/** Статус gateway и количество победителей на каждой стороне. */
data class MirrorSyncStatus(
    val status: MirrorRemoteStatus,
    val pushChangesCount: Int = 0,
    val pullChangesCount: Int = 0,
) {
    /** Есть ли хотя бы одна remote-версия, которую следует применить локально. */
    val hasRemoteChanges: Boolean
        get() = pullChangesCount > 0
}

/**
 * Создает typed tombstone, сохраняя исходный payload и меняя только sync metadata.
 */
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
