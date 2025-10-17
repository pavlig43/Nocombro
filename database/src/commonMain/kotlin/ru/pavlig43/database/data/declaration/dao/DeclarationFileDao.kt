package ru.pavlig43.database.data.declaration.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.declaration.DeclarationFile

@Dao
interface DeclarationFileDao {
    @Query("SELECT * FROM declaration_file WHERE declaration_id = :declarationId")
    suspend fun getFiles(declarationId: Int):List<DeclarationFile>

    @Upsert
    suspend fun upsertFiles(files:List<DeclarationFile>)

    @Query("DELETE FROM declaration_file WHERE id in(:ids)")
    suspend fun deleteFiles(ids:List<Int>)


}