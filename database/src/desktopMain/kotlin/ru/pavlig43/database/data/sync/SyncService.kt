package ru.pavlig43.database.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadSummary
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
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
     * а `remoteChangesCount` - число remote winners для следующего pull.
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
    suspend fun syncOnce(): SyncRunResult {
        val context = mirrorReconciliationService.prepareSync().getOrElse { throwable ->
            return failureWithoutRefresh(throwable.message ?: "Mirror sync preparation failed")
        }
        val mirrorRun = mirrorReconciliationService.executePreparedSync(context).getOrElse { throwable ->
            return failureWithoutRefresh(throwable.message ?: "Mirror sync failed")
        }

        syncStateRepository.updateLastPushAt(mirrorRun.completedAt)
        syncStateRepository.updateLastPullAt(mirrorRun.completedAt)
        val status = SyncStatusSnapshot(
            pendingLocalChangesCount = 0,
            remoteChangesCount = 0,
            hasRemoteChanges = false,
            remoteSyncConfigured = true,
            lastStatusCheckAt = mirrorRun.completedAt,
            lastSyncAt = mirrorRun.completedAt,
            lastPullAt = mirrorRun.completedAt,
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

    private suspend fun downloadMissingFilesAfterMirrorPull(): Result<RemoteFileBatchDownloadSummary?> {
        val repository = remoteFileBatchDownloadRepository
            ?: return Result.success(null)
        if (!repository.isConfigured()) {
            return Result.success(null)
        }
        return repository.downloadMissingLocalCopies()
    }

    private suspend fun failureWithoutRefresh(message: String): SyncRunResult {
        val current = _status.value
        val syncState = syncStateRepository.getSyncState()
        return SyncRunResult.failure(
            message = message,
            status = current ?: SyncStatusSnapshot(
                pendingLocalChangesCount = 0,
                remoteChangesCount = 0,
                hasRemoteChanges = false,
                remoteSyncConfigured = false,
                lastStatusCheckAt = defaultUpdatedAt(),
                lastSyncAt = syncState?.lastPushAt,
                lastPullAt = syncState?.lastPullAt,
            ),
        )
    }
}

private object SyncFacadeStageLog {
    private val logger = java.util.logging.Logger.getLogger("MirrorSync")
    fun completed(stage: String, milliseconds: Long) {
        logger.info("Mirror sync stage=$stage durationMs=$milliseconds")
    }
}

/** Иммутабельный snapshot состояния синхронизации для UI. */
data class SyncStatusSnapshot(
    val pendingLocalChangesCount: Int,
    val remoteChangesCount: Int,
    val hasRemoteChanges: Boolean,
    val remoteSyncConfigured: Boolean,
    val lastStatusCheckAt: LocalDateTime,
    val lastSyncAt: LocalDateTime?,
    val lastPullAt: LocalDateTime?,
    val remoteError: String? = null,
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
                lastStatusCheckAt = defaultUpdatedAt(),
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
