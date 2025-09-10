package ru.pavlig43.database.data.document.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.document.DocumentFile

@Dao
interface DocumentFilesDao {
    @Query("SELECT * FROM document_file WHERE document_id = :documentId")
    suspend fun getFiles(documentId: Int):List<DocumentFile>

    @Upsert
    suspend fun upsertDocumentFiles(files:List<DocumentFile>)

    @Query("DELETE FROM document_file WHERE id in(:ids)")
    suspend fun deleteFiles(ids:List<Int>)

}