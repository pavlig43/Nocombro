package ru.pavlig43.database.data.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.datetime.LocalDateTime

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

    /**
     * Атомарно создаёт состояние scope или меняет только отметку push.
     *
     * Значение `last_pull_at` уже существующей строки сохраняется.
     */
    @Query(
        """
        INSERT INTO sync_state(scope, last_push_at, last_pull_at)
        VALUES (:scope, :pushedAt, NULL)
        ON CONFLICT(scope) DO UPDATE SET last_push_at = excluded.last_push_at
        """
    )
    suspend fun upsertLastPushAt(scope: String, pushedAt: LocalDateTime)

    /**
     * Атомарно создаёт состояние scope или меняет только отметку pull.
     *
     * Значение `last_push_at` уже существующей строки сохраняется.
     */
    @Query(
        """
        INSERT INTO sync_state(scope, last_push_at, last_pull_at)
        VALUES (:scope, NULL, :pulledAt)
        ON CONFLICT(scope) DO UPDATE SET last_pull_at = excluded.last_pull_at
        """
    )
    suspend fun upsertLastPullAt(scope: String, pulledAt: LocalDateTime)

}
