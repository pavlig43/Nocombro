package ru.pavlig43.database.data.document.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.DocumentWithFiles

@Suppress("TooManyFunctions")
@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocument(document: Document):Long

    @Query("DELETE FROM document WHERE id IN (:ids)")
    suspend fun deleteDocuments(ids: List<Int>)

    @Update
    suspend fun updateDocument(document: Document)

    @Query("SELECT * from document WHERE id = :id")
    suspend fun getDocument(id: Int):Document

    @Query("SELECT * from document ORDER BY created_at ASC")
    fun observeAllDocuments(): Flow<List<Document>>

    @Query("SELECT * from document WHERE type IN (:types) ORDER BY created_at DESC")
    fun observeDocumentsByTypes(types: List<DocumentType>): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocumentPaths(filePaths:List<DocumentFilePath>)

    @Query("DELETE  FROM document_file_path WHERE id in(:deletedIds)")
    suspend fun deleteFilePaths(deletedIds:List<Int>)

    @Query("SELECT * FROM document_file_path WHERE document_id = :documentId")
    suspend fun getFilePaths(documentId: Int):List<DocumentFilePath>

    @Transaction
    suspend fun updateDocumentWithFiles(new:DocumentWithFiles,old:DocumentWithFiles){
        val deletedIds = old.files.filter { it !in new.files }.map { it.id }
        deleteFilePaths(deletedIds)
        val newFilePaths = new.files.filter { it.id == 0 }.map { it.copy(documentId = new.id) }
        insertDocumentPaths(newFilePaths)
        updateDocument(new.document)


    }

    @Transaction
    suspend fun insertDocumentWithWithFiles(documentWithFiles: DocumentWithFiles){
        val documentId = insertDocument(documentWithFiles.document).toInt()
        insertDocumentPaths(documentWithFiles.files.map { it.copy(documentId = documentId) })

    }
    @Query("""
        SELECT CASE
            WHEN (SELECT display_name FROM document WHERE id =:id) THEN TRUE
            ELSE NOT EXISTS (SELECT 1 FROM document WHERE display_name = :name AND id != :id)
        END
    """)
    suspend fun isNameChangeAllowed(id: Int,name: String):Boolean

    @Query("SELECT COUNT(*) > 0 FROM document WHERE display_name =:name")
    suspend fun isNameExist(name: String):Boolean

}