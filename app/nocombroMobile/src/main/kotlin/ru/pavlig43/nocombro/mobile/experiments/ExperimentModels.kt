package ru.pavlig43.nocombro.mobile.experiments

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class MobileExperiment(
    val id: Int,
    val syncId: String,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

data class MobileExperimentEntry(
    val id: Int,
    val syncId: String,
    val experimentId: Int,
    val entryDate: LocalDate,
    val content: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

data class MobileExperimentReminder(
    val id: Int,
    val syncId: String,
    val experimentId: Int,
    val text: String,
    val reminderDateTime: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

data class ExperimentsMobileState(
    val experiments: List<MobileExperiment> = emptyList(),
    val selectedExperiment: MobileExperiment? = null,
    val entries: List<MobileExperimentEntry> = emptyList(),
    val selectedEntry: MobileExperimentEntry? = null,
    val reminders: List<MobileExperimentReminder> = emptyList(),
    val showArchived: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
)

data class ExperimentSyncSnapshot(
    val experiments: List<ExperimentSyncRow>,
    val entries: List<ExperimentEntrySyncRow>,
    val reminders: List<ExperimentReminderSyncRow>,
)

data class ExperimentSyncRow(
    val syncId: String,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

data class ExperimentEntrySyncRow(
    val syncId: String,
    val experimentSyncId: String,
    val entryDate: LocalDate,
    val content: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

data class ExperimentReminderSyncRow(
    val syncId: String,
    val experimentSyncId: String,
    val text: String,
    val reminderDateTime: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Running : SyncStatus
    data class Failed(val message: String) : SyncStatus
    data class Synced(val at: LocalDateTime) : SyncStatus
}

fun currentDate(): LocalDate = Clock.System.now()
    .toLocalDateTime(TimeZone.currentSystemDefault())
    .date

fun currentDateTime(): LocalDateTime = Clock.System.now()
    .toLocalDateTime(TimeZone.currentSystemDefault())
