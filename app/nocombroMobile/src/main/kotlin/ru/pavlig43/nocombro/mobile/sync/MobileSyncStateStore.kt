package ru.pavlig43.nocombro.mobile.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime

/**
 * Хранит часовой лимит фоновой проверки и последний показанный статус Android.
 */
interface MobileSyncStateStore {
    suspend fun tryRecordBackgroundAttempt(
        attemptAtEpochMillis: Long,
        minIntervalMillis: Long,
    ): Boolean

    suspend fun recordManualAttempt(attemptAtEpochMillis: Long)

    suspend fun loadCachedStatus(): MobileCachedSyncStatus?

    suspend fun saveStatus(
        status: MobileSyncStatus,
        error: String?,
    )
}

class DataStoreMobileSyncStateStore(
    private val dataStore: DataStore<Preferences>,
) : MobileSyncStateStore {
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

    override suspend fun loadCachedStatus(): MobileCachedSyncStatus? {
        val preferences = dataStore.data.first()
        if (preferences[HAS_CACHED_STATUS] != true) return null
        val checkedAt = preferences[CHECKED_AT]
            ?.let { value -> runCatching { LocalDateTime.parse(value) }.getOrNull() }
            ?: return null
        return MobileCachedSyncStatus(
            configured = preferences[CONFIGURED] ?: false,
            localChanges = preferences[LOCAL_CHANGES] ?: 0,
            remoteChanges = preferences[REMOTE_CHANGES] ?: 0,
            checkedAt = checkedAt,
            error = preferences[ERROR],
            hasConflicts = preferences[HAS_CONFLICTS] ?: false,
        )
    }

    override suspend fun saveStatus(
        status: MobileSyncStatus,
        error: String?,
    ) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[HAS_CACHED_STATUS] = true
                this[CONFIGURED] = status.configured
                this[LOCAL_CHANGES] = status.localChanges
                this[REMOTE_CHANGES] = status.remoteChanges
                this[CHECKED_AT] = status.checkedAt.toString()
                this[HAS_CONFLICTS] = status.conflicts.isNotEmpty()
                if (error == null) {
                    remove(ERROR)
                } else {
                    this[ERROR] = error
                }
            }
        }
    }
}

data class MobileCachedSyncStatus(
    val configured: Boolean,
    val localChanges: Int,
    val remoteChanges: Int,
    val checkedAt: LocalDateTime,
    val error: String?,
    val hasConflicts: Boolean,
)

private val LAST_REMOTE_STATUS_ATTEMPT_AT = longPreferencesKey(
    "last_remote_status_attempt_at_epoch_millis"
)
private val HAS_CACHED_STATUS = booleanPreferencesKey("has_cached_sync_status")
private val CONFIGURED = booleanPreferencesKey("cached_sync_configured")
private val LOCAL_CHANGES = intPreferencesKey("cached_sync_local_changes")
private val REMOTE_CHANGES = intPreferencesKey("cached_sync_remote_changes")
private val CHECKED_AT = stringPreferencesKey("cached_sync_checked_at")
private val ERROR = stringPreferencesKey("cached_sync_error")
private val HAS_CONFLICTS = booleanPreferencesKey("cached_sync_has_conflicts")
