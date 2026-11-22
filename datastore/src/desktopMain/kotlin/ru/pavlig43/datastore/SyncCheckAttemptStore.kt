package ru.pavlig43.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Хранит время последней попытки и результат последней успешной сверки.
 *
 * Отметка записывается до сетевого запроса. Поэтому падение или перезапуск
 * приложения не запускают новую фоновую серию запросов раньше срока.
 */
interface SyncCheckAttemptStore {
    /**
     * Записывает [attemptAtEpochMillis], только если фоновая проверка уже разрешена.
     *
     * @return true, если вызывающий код должен начать удалённую проверку.
     */
    suspend fun tryRecordBackgroundAttempt(
        attemptAtEpochMillis: Long,
        minIntervalMillis: Long,
    ): Boolean

    /** Записывает попытку ручной операции без часового лимита. */
    suspend fun recordManualAttempt(attemptAtEpochMillis: Long)

    suspend fun loadCachedStatus(): CachedSyncStatus?

    suspend fun saveStatus(status: CachedSyncStatus)
}

data class CachedSyncStatus(
    val checkedAt: String,
    val pendingLocalChangesCount: Int,
    val remoteChangesCount: Int,
    val hasRemoteChanges: Boolean,
    val conflictsCount: Int,
)

class DataStoreSyncCheckAttemptStore(
    private val dataStore: DataStore<Preferences>,
) : SyncCheckAttemptStore {
    override suspend fun tryRecordBackgroundAttempt(
        attemptAtEpochMillis: Long,
        minIntervalMillis: Long,
    ): Boolean {
        var accepted = false
        dataStore.updateData { preferences ->
            val previousAttempt = preferences[LAST_REMOTE_STATUS_ATTEMPT_AT]
            val isDue = previousAttempt == null ||
                attemptAtEpochMillis - previousAttempt >= minIntervalMillis
            if (!isDue) return@updateData preferences

            accepted = true
            preferences.toMutablePreferences().apply {
                this[LAST_REMOTE_STATUS_ATTEMPT_AT] = attemptAtEpochMillis
            }
        }
        return accepted
    }

    override suspend fun recordManualAttempt(attemptAtEpochMillis: Long) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[LAST_REMOTE_STATUS_ATTEMPT_AT] = attemptAtEpochMillis
            }
        }
    }

    override suspend fun loadCachedStatus(): CachedSyncStatus? {
        val preferences = dataStore.data.first()
        return CachedSyncStatus(
            checkedAt = preferences[CHECKED_AT] ?: return null,
            pendingLocalChangesCount = preferences[PENDING_LOCAL_CHANGES] ?: return null,
            remoteChangesCount = preferences[REMOTE_CHANGES] ?: return null,
            hasRemoteChanges = preferences[HAS_REMOTE_CHANGES] ?: return null,
            conflictsCount = preferences[CONFLICTS] ?: return null,
        )
    }

    override suspend fun saveStatus(status: CachedSyncStatus) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[CHECKED_AT] = status.checkedAt
                this[PENDING_LOCAL_CHANGES] = status.pendingLocalChangesCount
                this[REMOTE_CHANGES] = status.remoteChangesCount
                this[HAS_REMOTE_CHANGES] = status.hasRemoteChanges
                this[CONFLICTS] = status.conflictsCount
            }
        }
    }
}

private val LAST_REMOTE_STATUS_ATTEMPT_AT = longPreferencesKey(
    "last_remote_status_attempt_at_epoch_millis"
)

private val CHECKED_AT = stringPreferencesKey("sync_status_checked_at")
private val PENDING_LOCAL_CHANGES = intPreferencesKey("sync_status_local_changes")
private val REMOTE_CHANGES = intPreferencesKey("sync_status_remote_changes")
private val HAS_REMOTE_CHANGES = booleanPreferencesKey("sync_status_has_remote_changes")
private val CONFLICTS = intPreferencesKey("sync_status_conflicts")
