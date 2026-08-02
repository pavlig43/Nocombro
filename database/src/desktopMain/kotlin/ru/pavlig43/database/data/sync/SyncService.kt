package ru.pavlig43.database.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadSummary
import ru.pavlig43.database.data.sync.mirror.MirrorConflictResolutionResult
import ru.pavlig43.database.data.sync.mirror.MirrorConflictWinner
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationRun
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteStatus
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.database.data.sync.mirror.isYdbResourceExhausted
import java.io.File
import kotlin.time.TimeSource

/**
 * Прикладной слой синхронизации для интерфейса и корневых компонентов.
 *
 * Сервис последовательно координирует сверку зеркал, сохраняет даты успешной
 * отправки и получения, а после получения восстанавливает отсутствующие локальные файлы.
 * Он не содержит алгоритма сравнения строк: это ответственность
 * [MirrorReconciliationService].
 *
 * [status] публикует последний явно рассчитанный снимок. Он изначально равен
 * `null` и обновляется локальной загрузкой, проверкой или ручной операцией.
 */
class SyncService(
    private val syncStateRepository: SyncStateRepository,
    private val mirrorReconciliationService: MirrorReconciliationService,
    private val remoteFileBatchDownloadRepository: RemoteFileBatchDownloadRepository? = null,
    private val syncAnalysisReportWriter: SyncAnalysisReportWriter = SyncAnalysisReportWriter(),
) {
    private val _status = MutableStateFlow<SyncStatusSnapshot?>(null)
    val status: StateFlow<SyncStatusSnapshot?> = _status.asStateFlow()

    /** Читает только локальное состояние и последний отчёт, не обращаясь к YDB. */
    suspend fun getLocalStatus(): SyncStatusSnapshot {
        val syncState = syncStateRepository.getSyncState()
        val configuration = runCatching {
            mirrorReconciliationService.getConfigurationStatus()
        }.getOrNull()
        val current = _status.value
        val reportSnapshotAt = syncAnalysisReportWriter.latestSnapshotAt()
        return SyncStatusSnapshot(
            pendingLocalChangesCount = current?.pendingLocalChangesCount ?: 0,
            remoteChangesCount = current?.remoteChangesCount ?: 0,
            hasRemoteChanges = current?.hasRemoteChanges ?: false,
            remoteSyncConfigured = configuration?.configured
                ?: current?.remoteSyncConfigured
                ?: false,
            lastStatusCheckAt = reportSnapshotAt ?: current?.lastStatusCheckAt,
            lastSyncAt = syncState?.lastPushAt,
            lastPullAt = syncState?.lastPullAt,
            conflicts = current?.conflicts.orEmpty(),
            reportSnapshotAt = reportSnapshotAt,
        ).also { snapshot ->
            _status.value = snapshot
        }
    }

    /**
     * Пересчитывает расхождения Room/YDB и публикует актуальное состояние интерфейса.
     *
     * `pendingLocalChangesCount` означает число локальных победителей для следующей отправки,
     * а `remoteChangesCount` — число удалённых победителей для следующего получения. Равные
     * версии с разным содержимым публикуются отдельно в [SyncStatusSnapshot.conflicts].
     */
    suspend fun getStatus(): SyncStatusSnapshot {
        val syncState = syncStateRepository.getSyncState()
        val mirrorStatus = mirrorReconciliationService.getSyncStatus()
        var reportSnapshotAt = syncAnalysisReportWriter.latestSnapshotAt()
        val reportError = mirrorStatus.preview
            ?.takeIf { mirrorStatus.status.error == null }
            ?.let { preview ->
                runCatching {
                    syncAnalysisReportWriter.write(preview)
                    reportSnapshotAt = preview.remoteSnapshot.loadedAt
                }.exceptionOrNull()
            }

        return SyncStatusSnapshot(
            pendingLocalChangesCount = mirrorStatus.pushChangesCount,
            remoteChangesCount = mirrorStatus.pullChangesCount,
            hasRemoteChanges = mirrorStatus.hasRemoteChanges,
            remoteSyncConfigured = mirrorStatus.status.configured,
            lastStatusCheckAt = mirrorStatus.status.checkedAt,
            lastSyncAt = syncState?.lastPushAt,
            lastPullAt = syncState?.lastPullAt,
            remoteError = mirrorStatus.status.error?.toSyncUserMessage()
                ?: reportError?.syncUserMessage("Не удалось сохранить отчёт синхронизации."),
            conflicts = mirrorStatus.conflicts,
            reportSnapshotAt = reportSnapshotAt,
        ).also { snapshot ->
            _status.value = snapshot
        }
    }

    /**
     * Возвращает последний отчёт с диска. Room и YDB не читаются.
     */
    fun createSyncAnalysisReport(): Result<File> {
        return syncAnalysisReportWriter.latestReport()
    }

    /**
     * Выполняет полный пользовательский цикл: сначала отправку, затем получение.
     *
     * Получение не запускается после ошибки отправки, чтобы не маскировать первичную причину.
     * Итог включает результат восстановления файлов, выполненного после получения.
     */
    @Suppress("ReturnCount")
    suspend fun syncOnce(): SyncRunResult {
        val context = mirrorReconciliationService.prepareSync().getOrElse { throwable ->
            val configuration = runCatching {
                mirrorReconciliationService.getConfigurationStatus()
            }.getOrNull()
            return failureWithoutRefresh(
                message = throwable.syncUserMessage("Не удалось подготовить синхронизацию с YDB."),
                configuration = configuration,
            )
        }
        val mirrorRun = mirrorReconciliationService.executePreparedSync(context).getOrElse { throwable ->
            return failureWithoutRefresh(
                message = throwable.syncUserMessage("Не удалось синхронизировать данные с YDB."),
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
            reportSnapshotAt = syncAnalysisReportWriter.latestSnapshotAt(),
        ).also { _status.value = it }

        val recoveryMark = TimeSource.Monotonic.markNow()
        val filesDownloadSummary = downloadMissingFilesAfterMirrorPull().getOrElse { throwable ->
            SyncFacadeStageLog.completed("S3 recovery", recoveryMark.elapsedNow().inWholeMilliseconds)
            return SyncRunResult.failure(
                message = throwable.syncUserMessage("Не удалось восстановить файлы из S3."),
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
     * Отсутствующая удалённая конфигурация считается явной ошибкой операции, а не
     * успешным завершением без действий.
     */
    @Suppress("ReturnCount")
    suspend fun pushOnce(): SyncRunResult {
        val mirrorPush = mirrorReconciliationService.pushLocalWinners().fold(
            onSuccess = { it },
            onFailure = { throwable ->
                return failureWithoutRefresh(
                    message = throwable.syncUserMessage("Не удалось отправить данные в YDB."),
                    configuration = null,
                )
            }
        )
        if (!mirrorPush.configured) {
            return failureWithoutRefresh(
                message = "Синхронизация с YDB не настроена.",
                configuration = null,
            )
        }
        syncStateRepository.updateLastPushAt(mirrorPush.completedAt)
        val syncState = syncStateRepository.getSyncState()

        val status = statusFromRun(
            run = mirrorPush,
            lastPushAt = mirrorPush.completedAt,
            lastPullAt = syncState?.lastPullAt,
        )
        return SyncRunResult(
            status = status,
            lastSyncAt = mirrorPush.completedAt,
            lastPushAt = mirrorPush.completedAt,
            lastPullAt = status.lastPullAt,
        )
    }

    /**
     * Применяет удалённых победителей, сохраняет `lastPullAt` и восстанавливает файлы.
     *
     * Ошибка восстановления из S3 возвращается после успешного получения из зеркала: данные
     * Room уже применены, но интерфейс получает точную информацию о неполном восстановлении файлов.
     */
    @Suppress("ReturnCount")
    suspend fun pullOnce(): SyncRunResult {
        val mirrorPull = mirrorReconciliationService.pullRemoteWinners().fold(
            onSuccess = { it },
            onFailure = { throwable ->
                return failureWithoutRefresh(
                    message = throwable.syncUserMessage("Не удалось получить данные из YDB."),
                    configuration = null,
                )
            }
        )
        if (!mirrorPull.configured) {
            return failureWithoutRefresh(
                message = "Синхронизация с YDB не настроена.",
                configuration = null,
            )
        }
        syncStateRepository.updateLastPullAt(
            pulledAt = mirrorPull.completedAt,
        )
        val syncState = syncStateRepository.getSyncState()
        val status = statusFromRun(
            run = mirrorPull,
            lastPushAt = syncState?.lastPushAt,
            lastPullAt = mirrorPull.completedAt,
        )
        val filesDownloadSummary = downloadMissingFilesAfterMirrorPull().getOrElse { throwable ->
            return SyncRunResult.failure(
                message = throwable.syncUserMessage("Не удалось восстановить файлы из S3."),
                status = status,
            )
        }
        return SyncRunResult(
            status = status,
            lastSyncAt = syncState?.lastPushAt,
            lastPushAt = syncState?.lastPushAt,
            lastPullAt = mirrorPull.completedAt,
            filesDownloadSummary = filesDownloadSummary,
        )
    }

    private fun statusFromRun(
        run: MirrorReconciliationRun,
        lastPushAt: LocalDateTime?,
        lastPullAt: LocalDateTime?,
    ): SyncStatusSnapshot {
        val snapshot = SyncStatusSnapshot(
            pendingLocalChangesCount = run.remainingPushChanges,
            remoteChangesCount = run.remainingPullChanges,
            hasRemoteChanges = run.remainingPullChanges > 0,
            remoteSyncConfigured = run.configured,
            lastStatusCheckAt = run.completedAt,
            lastSyncAt = lastPushAt,
            lastPullAt = lastPullAt,
            conflicts = run.conflicts,
            reportSnapshotAt = syncAnalysisReportWriter.latestSnapshotAt(),
        )
        _status.value = snapshot
        return snapshot
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
            lastStatusCheckAt = configuration?.checkedAt ?: syncAnalysisReportWriter.latestSnapshotAt(),
            lastSyncAt = syncState?.lastPushAt,
            lastPullAt = syncState?.lastPullAt,
            reportSnapshotAt = syncAnalysisReportWriter.latestSnapshotAt(),
        )).copy(
            remoteSyncConfigured = configuration?.configured ?: current?.remoteSyncConfigured ?: false,
            lastStatusCheckAt = configuration?.checkedAt ?: current?.lastStatusCheckAt,
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
     * обновляется фактическими строками, чтобы интерфейс не показывал старые данные.
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

private fun Throwable.syncUserMessage(fallback: String): String {
    if (isYdbResourceExhausted()) {
        return "YDB временно отклонила запрос из-за нехватки ресурсов. Повторите позже."
    }
    return message?.takeIf(String::isNotBlank) ?: fallback
}

private fun String.toSyncUserMessage(): String {
    return if (contains("RESOURCE_EXHAUSTED", ignoreCase = true) || contains("401020")) {
        "YDB временно отклонила запрос из-за нехватки ресурсов. Повторите позже."
    } else {
        this
    }
}

private object SyncFacadeStageLog {
    private val logger = java.util.logging.Logger.getLogger("MirrorSync")
    fun completed(stage: String, milliseconds: Long) {
        logger.fine("Mirror sync stage=$stage durationMs=$milliseconds")
    }
}

/**
 * Неизменяемый снимок состояния синхронизации для интерфейса и Doctor.
 *
 * [conflicts] содержит только пары с равной версией и разным переносимым
 * содержимым; такие строки не входят в счётчики отправки и получения.
 */
data class SyncStatusSnapshot(
    val pendingLocalChangesCount: Int,
    val remoteChangesCount: Int,
    val hasRemoteChanges: Boolean,
    val remoteSyncConfigured: Boolean,
    val lastStatusCheckAt: LocalDateTime?,
    val lastSyncAt: LocalDateTime?,
    val lastPullAt: LocalDateTime?,
    val remoteError: String? = null,
    val conflicts: List<MirrorVersionConflict> = emptyList(),
    val reportSnapshotAt: LocalDateTime? = null,
)

/**
 * Итог одной команды синхронизации.
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
        /** Создаёт единообразный результат ошибки с переданным или безопасным статусом. */
        fun failure(
            message: String,
            status: SyncStatusSnapshot? = null,
        ): SyncRunResult {
            val fallbackStatus = status ?: SyncStatusSnapshot(
                pendingLocalChangesCount = 0,
                remoteChangesCount = 0,
                hasRemoteChanges = false,
                remoteSyncConfigured = false,
                lastStatusCheckAt = null,
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
