package ru.pavlig43.database.data.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

/**
 * Таблица с техническим состоянием синхронизации текущей локальной базы.
 *
 * Здесь хранится не бизнес-данные, а служебная информация: кто мы (`deviceId`),
 * когда последний раз делали pull/push и на каком удаленном курсоре остановились.
 */
const val SYNC_STATE_TABLE_NAME = "sync_state"

/**
 * Базовый scope синхронизации для текущего приложения.
 *
 * Сейчас используется одна общая запись состояния, но поле `scope` оставляет запас
 * на будущее, если появятся отдельные контексты синхронизации.
 */
const val DEFAULT_SYNC_SCOPE = "default"

@Entity(tableName = SYNC_STATE_TABLE_NAME)
data class SyncStateEntity(
    /**
     * Идентификатор контекста синхронизации.
     */
    @PrimaryKey
    @ColumnInfo("scope")
    val scope: String = DEFAULT_SYNC_SCOPE,

    /**
     * Постоянный идентификатор текущей установки приложения.
     */
    @ColumnInfo("device_id")
    val deviceId: String,

    /**
     * Время последнего успешного получения изменений с сервера.
     */
    @ColumnInfo("last_pull_at")
    val lastPullAt: LocalDateTime? = null,

    /**
     * Время последней успешной отправки изменений на сервер.
     */
    @ColumnInfo("last_push_at")
    val lastPushAt: LocalDateTime? = null,

    /**
     * Последний курсор удаленной синхронизации, если сервер его использует.
     */
    @ColumnInfo("last_remote_cursor")
    val lastRemoteCursor: String? = null,
)
