package ru.pavlig43.database.data.sync

/**
 * Тип изменения, которое должно быть отражено в удаленной базе.
 */
enum class SyncChangeType {
    UPSERT,
    DELETE,
}
