package ru.pavlig43.database.data.declaration.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.common.isCanSaveWithName
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.declaration.Declaration

@Dao
interface DeclarationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(declaration: Declaration): Long

    @Update
    suspend fun updateDeclaration(declaration: Declaration)

    @Query("DELETE FROM $DECLARATIONS_TABLE_NAME WHERE id IN (:ids)")
    suspend fun deleteDeclarationsByIds(ids: Set<Int>)


    @Query("SELECT * FROM $DECLARATIONS_TABLE_NAME WHERE id = :id")
    suspend fun getDeclaration(id: Int): Declaration

    @Query(
        """
    SELECT * FROM $DECLARATIONS_TABLE_NAME d
    ORDER BY id DESC
"""
    )
    fun observeOnItems(): Flow<List<Declaration>>

    @Query(
        """
        SELECT CASE
            WHEN (SELECT display_name FROM $DECLARATIONS_TABLE_NAME WHERE id =:id) =:name THEN TRUE
            ELSE NOT EXISTS (SELECT 1 FROM $DECLARATIONS_TABLE_NAME WHERE display_name = :name AND id != :id)
        END
    """
    )
    suspend fun isNameAllowed(id: Int, name: String): Boolean

    suspend fun isCanSave(declaration: Declaration): Result<Unit> {
        return isCanSaveWithName(declaration.id,declaration.displayName,::isNameAllowed)
    }


    @Query(
        """
       SELECT id,display_name AS displayName
        FROM declaration WHERE id NOT IN (SELECT declaration_id FROM declaration_file) 
    """
    )
    fun observeOnDeclarationInWithoutFiles(): Flow<List<NotificationDTO>>

    @Query(
        """
       SELECT id,display_name AS displayName
        FROM declaration
        WHERE best_before < (strftime('%s', 'now') * 1000 + :delta) AND observe_from_notification = true 
         
    """
    )
    fun observeOnExpiredDeclaration(delta: Long): Flow<List<NotificationDTO>>



}