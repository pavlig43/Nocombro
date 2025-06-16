package ru.pavlig43.database.data.document.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType

@Dao
interface DocumentDao {


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: Document)

    @Update
    suspend fun update(document: Document):Int

    @Query("DELETE FROM document WHERE id IN (:ids)")
    suspend fun deleteItems(ids: List<Int>):Int

    @Query("SELECT * from document WHERE id = :id")
    suspend fun getDocument(id: Int): Document

    @Query("SELECT * from document ORDER BY createdAt ASC")
    fun getAllDocument(): Flow<List<Document>>

    @Query("SELECT * from document WHERE type IN (:types)  ORDER BY createdAt ASC")
    fun getDocumentsByTypes(types: List<DocumentType>): Flow<List<Document>>

    @Query("SELECT * from document WHERE type = :type  ORDER BY createdAt ASC")
    fun getDocumentsByType(type: String): Flow<List<Document>>

    @Query("SELECT * from document WHERE type = :type  ORDER BY createdAt ASC")
    fun getDocumentsByType(type: DocumentType): Flow<List<Document>>

    @Query("SELECT COUNT(*) from document WHERE display_name = :name")
    suspend fun validName(name:String):Int


}