package ru.pavlig43.database.data.sync.mirror

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.datetime.LocalDateTime

const val MIRROR_DELETION_JOURNAL_TABLE_NAME = "mirror_deletion_journal"

/**
 * Сохраненный tombstone физически удаленной Room-строки.
 *
 * Журнал нужен потому, что после hard delete обычный snapshot больше не может
 * восстановить ни `sync_id`, ни payload строки. Первичный ключ
 * `entity_table + sync_id` позволяет обновлять tombstone той же сущности без
 * накопления дублей.
 *
 * @property entityTable имя таблицы из [MirrorSyncTable.tableName].
 * @property syncId стабильный межустановочный идентификатор удаленной строки.
 * @property rowJson сериализованный конкретный [MirrorSyncRow] с заполненным
 * `deletedAt`; payload сохраняется для typed push в YDB.
 * @property deletedAt время hard delete, продублированное отдельной колонкой для
 * диагностики и будущей retention-политики.
 */
@Entity(
    tableName = MIRROR_DELETION_JOURNAL_TABLE_NAME,
    primaryKeys = ["entity_table", "sync_id"],
)
data class MirrorDeletionJournalEntity(
    @ColumnInfo("entity_table")
    val entityTable: String,
    @ColumnInfo("sync_id")
    val syncId: String,
    @ColumnInfo("row_json")
    val rowJson: String,
    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime,
)
