package ru.pavlig43.database.data.document.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.DocumentWithFiles

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocument(document: Document):Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocumentPaths(filePaths:List<DocumentFilePath>)

    @Query("DELETE FROM document WHERE id IN (:ids)")
    suspend fun deleteDocuments(ids: List<Int>)

    @Query("SELECT * from document WHERE id = :id")
    suspend fun getDocumentWithFiles(id: Int): DocumentWithFiles

    @Query("SELECT * from document ORDER BY created_at ASC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * from document WHERE type IN (:types) ORDER BY created_at ASC")
    fun getDocumentsByTypes(types: List<DocumentType>): Flow<List<Document>>


    @Query("SELECT COUNT(*) from document WHERE display_name = :name")
    suspend fun isExistName(name: String): Int

    @Transaction
    suspend fun insertDocumentWithWithFiles(documentWithFiles: DocumentWithFiles){
        val documentId = insertDocument(documentWithFiles.document).toInt()
        insertDocumentPaths(documentWithFiles.files.map { it.copy(documentId = documentId) })

    }

}