package ru.pavlig43.database.data.sync

/**
 * Статус элемента локальной очереди синхронизации.
 */
enum class SyncQueueStatus {
    PENDING,
    IN_PROGRESS,
    FAILED,
}
