package ru.pavlig43.database.data.sync.mirror

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Room-доступ к долговременному журналу tombstone. */
@Dao
interface MirrorDeletionJournalDao {
    /** Возвращает все tombstone, которые должны участвовать в local snapshot. */
    @Query("SELECT * FROM $MIRROR_DELETION_JOURNAL_TABLE_NAME")
    suspend fun getAll(): List<MirrorDeletionJournalEntity>

    /**
     * Добавляет или обновляет tombstone по паре `entity_table + sync_id`.
     *
     * Повторный remote pull и повторный capture одного удаления поэтому идемпотентны.
     */
    @Upsert
    suspend fun upsert(entries: List<MirrorDeletionJournalEntity>)
}
