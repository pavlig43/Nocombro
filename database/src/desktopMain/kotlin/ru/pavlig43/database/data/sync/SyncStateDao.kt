package ru.pavlig43.database.data.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SyncStateDao {
    /**
     * Создает или обновляет состояние синхронизации текущей локальной базы.
     */
    @Upsert
    suspend fun upsertSyncState(syncState: SyncStateEntity)

    /**
     * Возвращает текущее состояние синхронизации для указанного scope.
     */
    @Query("SELECT * FROM sync_state WHERE scope = :scope")
    suspend fun getSyncState(scope: String = DEFAULT_SYNC_SCOPE): SyncStateEntity?

}
