package ru.pavlig43.database.data.document.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.common.IsCanUpsertResult
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.document.Document


@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(document: Document):Long

    @Update
    suspend fun updateDocument(document: Document)

    @Query("DELETE FROM document WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: Set<Int>)

    @Query("SELECT * from document WHERE id = :id")
    suspend fun getDocument(id: Int):Document


    @Query("""
    SELECT * FROM $DOCUMENT_TABLE_NAME
    ORDER BY id DESC
""")
    fun observeOnDocuments(): Flow<List<Document>>

    @Query("""
        SELECT CASE
            WHEN (SELECT display_name FROM document WHERE id =:id) =:name THEN TRUE
            ELSE NOT EXISTS (SELECT 1 FROM document WHERE display_name = :name AND id != :id)
        END
    """)
    suspend fun isNameAllowed(id: Int, name: String):Boolean

    suspend fun isCanSave(document: Document): IsCanUpsertResult{
        if (document.displayName.isBlank()) return IsCanUpsertResult.NameBlank()
        if (!isNameAllowed(document.id,document.displayName)) return IsCanUpsertResult.NameExists()
        return IsCanUpsertResult.Ok()
    }


    @Query("SELECT id,display_name AS displayName FROM document WHERE id NOT IN (SELECT document_id FROM document_file)")
    fun observeOnDocumentWithoutFiles():Flow<List<NotificationDTO>>

}
