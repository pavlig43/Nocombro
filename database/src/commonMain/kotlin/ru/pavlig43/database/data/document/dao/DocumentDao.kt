package ru.pavlig43.database.data.document.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.common.NotificationDTO


@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(document: Document):Long

    @Update
    suspend fun updateDocument(document: Document)

    @Query("DELETE FROM document WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<Int>)

    @Query("SELECT * from document WHERE id = :id")
    suspend fun getDocument(id: Int):Document

    @Query("SELECT * from document ORDER BY created_at ASC")
    fun observeAllDocument(): Flow<List<Document>>

    @Query("SELECT * from document WHERE type IN (:types) ORDER BY created_at DESC")
    fun observeDocumentsByTypes(types: List<DocumentType>): Flow<List<Document>>

    @Query("""
        SELECT CASE
            WHEN (SELECT display_name FROM document WHERE id =:id) =:name THEN TRUE
            ELSE NOT EXISTS (SELECT 1 FROM document WHERE display_name = :name AND id != :id)
        END
    """)
    suspend fun isNameChangeAllowed(id: Int,name: String):Boolean

    @Query("SELECT COUNT(*) > 0 FROM document WHERE display_name =:name")
    suspend fun isNameExist(name: String):Boolean

    @Query("SELECT id,display_name AS name FROM document WHERE id NOT IN (SELECT document_id FROM document_file)")
    fun observeOnDocumentWithoutFiles():Flow<List<NotificationDTO>>

}
