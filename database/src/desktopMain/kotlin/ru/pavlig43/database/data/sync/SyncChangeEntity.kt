package ru.pavlig43.database.data.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

/**
 * Таблица локальной очереди изменений, которые нужно отправить в удаленную базу.
 *
 * Здесь не хранится полный снимок сущности: только информация о том, какую запись и какого типа
 * нужно синхронизировать, в каком она статусе и были ли ошибки отправки.
 */
const val SYNC_CHANGE_TABLE_NAME = "sync_change"
const val SYNC_CHANGE_ENTITY_TABLE_COLUMN = "entity_table"
const val SYNC_CHANGE_ENTITY_LOCAL_ID_COLUMN = "entity_local_id"
const val SYNC_CHANGE_TYPE_COLUMN = "change_type"
const val SYNC_CHANGE_STATUS_COLUMN = "status"
const val SYNC_CHANGE_CREATED_AT_COLUMN = "created_at"
const val SYNC_CHANGE_UPDATED_AT_COLUMN = "updated_at"
const val SYNC_CHANGE_ATTEMPT_COUNT_COLUMN = "attempt_count"
const val SYNC_CHANGE_LAST_ERROR_COLUMN = "last_error"

@Entity(
    tableName = SYNC_CHANGE_TABLE_NAME,
    indices = [
        Index(value = [SYNC_CHANGE_STATUS_COLUMN, SYNC_CHANGE_CREATED_AT_COLUMN]),
        Index(value = [SYNC_CHANGE_ENTITY_TABLE_COLUMN, SYNC_CHANGE_ENTITY_LOCAL_ID_COLUMN]),
    ]
)
data class SyncChangeEntity(
    /**
     * Имя локальной таблицы, в которой лежит измененная запись.
     */
    @ColumnInfo(SYNC_CHANGE_ENTITY_TABLE_COLUMN)
    val entityTable: String,

    /**
     * Локальный идентификатор записи в текстовом виде.
     *
     * Храним строку, чтобы очередь одинаково работала и для `Int id`, и для будущих строковых ключей.
     */
    @ColumnInfo(SYNC_CHANGE_ENTITY_LOCAL_ID_COLUMN)
    val entityLocalId: String,

    /**
     * Тип изменения: создание/обновление или удаление.
     */
    @ColumnInfo(SYNC_CHANGE_TYPE_COLUMN)
    val changeType: SyncChangeType,

    /**
     * Текущий статус элемента очереди синхронизации.
     */
    @ColumnInfo(SYNC_CHANGE_STATUS_COLUMN)
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,

    /**
     * Время постановки изменения в очередь.
     */
    @ColumnInfo(SYNC_CHANGE_CREATED_AT_COLUMN)
    val createdAt: LocalDateTime,

    /**
     * Время последнего изменения записи очереди.
     */
    @ColumnInfo(SYNC_CHANGE_UPDATED_AT_COLUMN)
    val updatedAt: LocalDateTime,

    /**
     * Сколько раз уже пытались отправить это изменение.
     */
    @ColumnInfo(SYNC_CHANGE_ATTEMPT_COUNT_COLUMN, defaultValue = "0")
    val attemptCount: Int = 0,

    /**
     * Текст последней ошибки отправки, если она была.
     */
    @ColumnInfo(SYNC_CHANGE_LAST_ERROR_COLUMN)
    val lastError: String? = null,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)
