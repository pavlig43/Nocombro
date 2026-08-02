package ru.pavlig43.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * Хранит время последней попытки удалённой проверки статуса.
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
}

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
}

private val LAST_REMOTE_STATUS_ATTEMPT_AT = longPreferencesKey(
    "last_remote_status_attempt_at_epoch_millis"
)
