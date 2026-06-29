package ru.pavlig43.nocombro.mobile.experiments

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate

/**
 * UI-модель mobile-эксперимента.
 */
data class MobileExperiment(
    val id: Int,
    val syncId: String,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

/**
 * UI-модель записи журнала эксперимента.
 */
data class MobileExperimentEntry(
    val id: Int,
    val syncId: String,
    val experimentId: Int,
    val entryDate: LocalDate,
    val content: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

/**
 * UI-модель напоминания по эксперименту.
 */
data class MobileExperimentReminder(
    val id: Int,
    val syncId: String,
    val experimentId: Int,
    val text: String,
    val reminderDateTime: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

/**
 * Состояние mobile-экрана экспериментов.
 */
data class ExperimentsMobileState(
    val experiments: List<MobileExperiment> = emptyList(),
    val selectedExperiment: MobileExperiment? = null,
    val entries: List<MobileExperimentEntry> = emptyList(),
    val selectedEntry: MobileExperimentEntry? = null,
    val reminders: List<MobileExperimentReminder> = emptyList(),
    val showArchived: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
)

/**
 * Snapshot локальных данных для отправки во внешний sync transport.
 */
data class ExperimentSyncSnapshot(
    val experiments: List<ExperimentSyncRow>,
    val entries: List<ExperimentEntrySyncRow>,
    val reminders: List<ExperimentReminderSyncRow>,
)

/**
 * Sync-строка эксперимента.
 */
data class ExperimentSyncRow(
    val syncId: String,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

/**
 * Sync-строка записи журнала эксперимента.
 */
data class ExperimentEntrySyncRow(
    val syncId: String,
    val experimentSyncId: String,
    val entryDate: LocalDate,
    val content: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

/**
 * Sync-строка напоминания по эксперименту.
 */
data class ExperimentReminderSyncRow(
    val syncId: String,
    val experimentSyncId: String,
    val text: String,
    val reminderDateTime: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

/**
 * Статус sync-операции mobile-экрана.
 */
sealed interface SyncStatus {
    /**
     * Sync не запускался.
     */
    data object Idle : SyncStatus

    /**
     * Sync выполняется.
     */
    data object Running : SyncStatus

    /**
     * Sync завершился ошибкой.
     */
    data class Failed(val message: String) : SyncStatus

    /**
     * Sync завершился успешно.
     */
    data class Synced(val at: LocalDateTime) : SyncStatus
}
