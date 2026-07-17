package ru.pavlig43.database.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadSummary
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
import ru.pavlig43.database.data.sync.mirror.MirrorConflictResolutionResult
import ru.pavlig43.database.data.sync.mirror.MirrorConflictWinner
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteStatus
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.datetime.getCurrentLocalDateTime
import java.io.File
import kotlin.time.TimeSource

/**
 * Прикладной facade синхронизации для UI и root-компонентов.
 *
 * Сервис последовательно координирует mirror reconciliation, сохраняет даты
 * успешных push/pull и после pull восстанавливает отсутствующие локальные файлы.
 * Он не содержит алгоритма сравнения строк: это ответственность
 * [MirrorReconciliationService].
 *
 * [status] публикует последний явно рассчитанный snapshot. Он изначально равен
 * `null` и обновляется каждым успешным вызовом [getStatus].
 */
class SyncService(
    private val syncStateRepository: SyncStateRepository,
    private val mirrorReconciliationService: MirrorReconciliationService,
    private val remoteFileBatchDownloadRepository: RemoteFileBatchDownloadRepository? = null,
    private val syncAnalysisReportWriter: SyncAnalysisReportWriter = SyncAnalysisReportWriter(),
) {
    private val _status = MutableStateFlow<SyncStatusSnapshot?>(null)
    val status: StateFlow<SyncStatusSnapshot?> = _status.asStateFlow()

    /**
     * Пересчитывает расхождения Room/YDB и публикует актуальный UI status.
     *
     * `pendingLocalChangesCount` означает число local winners для следующего push,
     * а `remoteChangesCount` — число remote winners для следующего pull. Равные
     * версии с разным содержимым публикуются отдельно в [SyncStatusSnapshot.conflicts].
     */
    suspend fun getStatus(): SyncStatusSnapshot {
        val syncState = syncStateRepository.getSyncState()
        val mirrorStatus = mirrorReconciliationService.getSyncStatus()

        return SyncStatusSnapshot(
            pendingLocalChangesCount = mirrorStatus.pushChangesCount,
            remoteChangesCount = mirrorStatus.pullChangesCount,
            hasRemoteChanges = mirrorStatus.hasRemoteChanges,
            remoteSyncConfigured = mirrorStatus.status.configured,
            lastStatusCheckAt = mirrorStatus.status.checkedAt,
            lastSyncAt = syncState?.lastPushAt,
            lastPullAt = syncState?.lastPullAt,
            remoteError = mirrorStatus.status.error,
            conflicts = mirrorStatus.conflicts,
        ).also { snapshot ->
            _status.value = snapshot
        }
    }

    /**
     * Сравнивает Room/YDB и сохраняет Markdown-отчёт, не изменяя sync state.
     */
    suspend fun createSyncAnalysisReport(): Result<File> {
        return mirrorReconciliationService.buildPreview()
            .mapCatching(syncAnalysisReportWriter::write)
    }

    /**
     * Выполняет полный пользовательский цикл: сначала push, затем pull.
     *
     * Pull не запускается после ошибки push, чтобы не маскировать первичную причину.
     * Итог включает результат восстановления файлов, выполненного после pull.
     */
    @Suppress("ReturnCount")
    suspend fun syncOnce(): SyncRunResult {
        val context = mirrorReconciliationService.prepareSync().getOrElse { throwable ->
            val configuration = runCatching {
                mirrorReconciliationService.getConfigurationStatus()
            }.getOrNull()
            return failureWithoutRefresh(
                message = throwable.message ?: "Mirror sync preparation failed",
                configuration = configuration,
            )
        }
        val mirrorRun = mirrorReconciliationService.executePreparedSync(context).getOrElse { throwable ->
            return failureWithoutRefresh(
                message = throwable.message ?: "Mirror sync failed",
                configuration = context.configuration,
            )
        }

        syncStateRepository.updateLastPushAt(mirrorRun.completedAt)
        syncStateRepository.updateLastPullAt(mirrorRun.completedAt)
        val status = SyncStatusSnapshot(
            pendingLocalChangesCount = mirrorRun.remainingPushChanges,
            remoteChangesCount = mirrorRun.remainingPullChanges,
            hasRemoteChanges = mirrorRun.remainingPullChanges > 0,
            remoteSyncConfigured = context.configuration.configured,
            lastStatusCheckAt = mirrorRun.completedAt,
            lastSyncAt = mirrorRun.completedAt,
            lastPullAt = mirrorRun.completedAt,
            conflicts = mirrorRun.conflicts,
        ).also { _status.value = it }

        val recoveryMark = TimeSource.Monotonic.markNow()
        val filesDownloadSummary = downloadMissingFilesAfterMirrorPull().getOrElse { throwable ->
            SyncFacadeStageLog.completed("S3 recovery", recoveryMark.elapsedNow().inWholeMilliseconds)
            return SyncRunResult.failure(
                message = throwable.message ?: "File recovery after mirror pull failed",
                status = status,
            )
        }
        SyncFacadeStageLog.completed("S3 recovery", recoveryMark.elapsedNow().inWholeMilliseconds)
        return SyncRunResult(
            status = status,
            lastSyncAt = mirrorRun.completedAt,
            lastPushAt = mirrorRun.completedAt,
            lastPullAt = mirrorRun.completedAt,
            filesDownloadSummary = filesDownloadSummary,
        )
    }

    /**
     * Отправляет локальных победителей и сохраняет `lastPushAt`.
     *
     * Отсутствующая remote-конфигурация считается явной ошибкой операции, а не
     * успешным no-op.
     */
    @Suppress("ReturnCount")
    suspend fun pushOnce(): SyncRunResult {
        val mirrorPush = mirrorReconciliationService.pushLocalWinners().fold(
            onSuccess = { it },
            onFailure = { throwable ->
                return SyncRunResult.failure(
                    message = throwable.message ?: "Mirror push failed",
                    status = getStatus(),
                )
            }
        )
        if (!mirrorPush.configured) {
            return SyncRunResult.failure(
                message = "Mirror sync is not configured",
                status = getStatus(),
            )
        }
        syncStateRepository.updateLastPushAt(mirrorPush.completedAt)

        val status = getStatus()
        return SyncRunResult(
            status = status,
            lastSyncAt = mirrorPush.completedAt,
            lastPushAt = mirrorPush.completedAt,
            lastPullAt = status.lastPullAt,
        )
    }

    /**
     * Применяет remote winners, сохраняет `lastPullAt` и восстанавливает файлы.
     *
     * Ошибка S3-восстановления возвращается после успешного mirror pull: данные
     * Room уже применены, но UI получает точную информацию о неполном file recovery.
     */
    @Suppress("ReturnCount")
    suspend fun pullOnce(): SyncRunResult {
        val mirrorPull = mirrorReconciliationService.pullRemoteWinners().fold(
            onSuccess = { it },
            onFailure = { throwable ->
                return SyncRunResult.failure(
                    message = throwable.message ?: "Mirror pull failed",
                    status = getStatus(),
                )
            }
        )
        if (!mirrorPull.configured) {
            return SyncRunResult.failure(
                message = "Mirror sync is not configured",
                status = getStatus(),
            )
        }
        syncStateRepository.updateLastPullAt(
            pulledAt = mirrorPull.completedAt,
        )
        val status = getStatus()
        val filesDownloadSummary = downloadMissingFilesAfterMirrorPull().getOrElse { throwable ->
            return SyncRunResult.failure(
                message = throwable.message ?: "File recovery after mirror pull failed",
                status = status,
            )
        }
        return SyncRunResult(
            status = status,
            lastSyncAt = status.lastSyncAt,
            lastPushAt = status.lastSyncAt,
            lastPullAt = mirrorPull.completedAt,
            filesDownloadSummary = filesDownloadSummary,
        )
    }

    @Suppress("ReturnCount")
    private suspend fun downloadMissingFilesAfterMirrorPull(): Result<RemoteFileBatchDownloadSummary?> {
        val repository = remoteFileBatchDownloadRepository
            ?: return Result.success(null)
        if (!repository.isConfigured()) {
            return Result.success(null)
        }
        return repository.downloadMissingLocalCopies()
    }

    @Suppress("UnreachableCode")
    private suspend fun failureWithoutRefresh(
        message: String,
        configuration: MirrorRemoteStatus?,
    ): SyncRunResult {
        val current = _status.value
        val syncState = syncStateRepository.getSyncState()
        val status = (current ?: SyncStatusSnapshot(
            pendingLocalChangesCount = 0,
            remoteChangesCount = 0,
            hasRemoteChanges = false,
            remoteSyncConfigured = configuration?.configured ?: false,
            lastStatusCheckAt = configuration?.checkedAt ?: getCurrentLocalDateTime(),
            lastSyncAt = syncState?.lastPushAt,
            lastPullAt = syncState?.lastPullAt,
        )).copy(
            remoteSyncConfigured = configuration?.configured ?: current?.remoteSyncConfigured ?: false,
            lastStatusCheckAt = configuration?.checkedAt ?: current?.lastStatusCheckAt ?: getCurrentLocalDateTime(),
            remoteError = message,
        )
        _status.value = status
        return SyncRunResult.failure(
            message = message,
            status = status,
        )
    }

    /**
     * Разрешает выбранный в Doctor конфликт и публикует свежий статус.
     *
     * Низкоуровневый сервис перечитывает обе стороны перед записью. Устаревший
     * выбор и повторный отказ YDB возвращаются как ошибка, а список конфликтов
     * обновляется фактическими строками, чтобы UI не показывал старые данные.
     *
     * @param conflict снимок конфликта, который видел пользователь.
     * @param winner сторона, чьё содержимое нужно сохранить.
     * @return новый статус либо ошибка устаревшего или отклонённого выбора.
     */
    suspend fun resolveConflict(
        conflict: MirrorVersionConflict,
        winner: MirrorConflictWinner,
    ): Result<SyncStatusSnapshot> {
        val resolution = mirrorReconciliationService.resolveConflict(conflict, winner)
            .getOrElse { throwable ->
                runCatching { getStatus() }
                return Result.failure(throwable)
            }
        return when (resolution) {
            MirrorConflictResolutionResult.Resolved -> runCatching { getStatus() }
            MirrorConflictResolutionResult.Stale -> {
                runCatching { getStatus() }
                Result.failure(
                    IllegalStateException(
                        "Строка изменилась. Обновите список конфликтов и повторите выбор."
                    )
                )
            }
            is MirrorConflictResolutionResult.Rejected -> {
                val refreshedStatus = getStatus()
                val rejectedConflict = resolution.conflict
                val conflicts = refreshedStatus.conflicts
                    .filterNot { current ->
                        current.table == rejectedConflict.table &&
                            current.localRow.syncId == rejectedConflict.localRow.syncId
                    } + rejectedConflict
                _status.value = refreshedStatus.copy(conflicts = conflicts)
                Result.failure(
                    IllegalStateException(
                        "YDB отклонила выбранную версию. Конфликт перечитан; выберите строку ещё раз."
                    )
                )
            }
        }
    }
}

private object SyncFacadeStageLog {
    private val logger = java.util.logging.Logger.getLogger("MirrorSync")
    fun completed(stage: String, milliseconds: Long) {
        logger.fine("Mirror sync stage=$stage durationMs=$milliseconds")
    }
}

/**
 * Неизменяемый снимок состояния синхронизации для UI и Doctor.
 *
 * [conflicts] содержит только пары с равной версией и разным переносимым
 * содержимым; такие строки не входят в счётчики push и pull.
 */
data class SyncStatusSnapshot(
    val pendingLocalChangesCount: Int,
    val remoteChangesCount: Int,
    val hasRemoteChanges: Boolean,
    val remoteSyncConfigured: Boolean,
    val lastStatusCheckAt: LocalDateTime,
    val lastSyncAt: LocalDateTime?,
    val lastPullAt: LocalDateTime?,
    val remoteError: String? = null,
    val conflicts: List<MirrorVersionConflict> = emptyList(),
)

/**
 * Итог одной sync-команды.
 *
 * Поля времени заполняются только для реально завершенных стадий. [error] не равен
 * `null`, если команда завершилась неуспешно, даже когда часть предыдущих стадий уже
 * успела выполниться.
 */
data class SyncRunResult(
    val status: SyncStatusSnapshot,
    val lastSyncAt: LocalDateTime? = null,
    val lastPushAt: LocalDateTime? = null,
    val lastPullAt: LocalDateTime? = null,
    val filesDownloadSummary: RemoteFileBatchDownloadSummary? = null,
    val error: String? = null,
) {
    companion object {
        /** Создает единообразный failure result с переданным или безопасным status. */
        fun failure(
            message: String,
            status: SyncStatusSnapshot? = null,
        ): SyncRunResult {
            val fallbackStatus = status ?: SyncStatusSnapshot(
                pendingLocalChangesCount = 0,
                remoteChangesCount = 0,
                hasRemoteChanges = false,
                remoteSyncConfigured = false,
                lastStatusCheckAt = getCurrentLocalDateTime(),
                lastSyncAt = null,
                lastPullAt = null,
                remoteError = null,
            )
            return SyncRunResult(
                status = fallbackStatus,
                error = message,
            )
        }
    }
}
