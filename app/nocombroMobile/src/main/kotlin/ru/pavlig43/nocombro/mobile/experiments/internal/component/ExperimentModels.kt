package ru.pavlig43.nocombro.mobile.experiments.internal.component
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
    val createdAt: LocalDateTime,
    val content: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)

/**
 * Локальный файл записи эксперимента.
 */
data class MobileExperimentEntryFile(
    val id: Int,
    val syncId: String,
    val entryId: Int,
    val displayName: String,
    val localPath: String,
    val objectKey: String,
    val mimeType: String?,
    val sizeBytes: Long?,
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
