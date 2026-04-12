package ru.pavlig43.database.data.files

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert


@Dao
interface FileDao {
    @Query("SELECT * FROM file WHERE owner_id = :ownerId AND owner_type =:ownerFileType")
    suspend fun getFiles(ownerId: Int,ownerFileType: OwnerType):List<FileBD>

    @Query(
        "SELECT * FROM file WHERE owner_id = :ownerId AND owner_type = :ownerFileType AND display_name = :displayName LIMIT 1"
    )
    suspend fun getFileByOwnerAndDisplayName(
        ownerId: Int,
        ownerFileType: OwnerType,
        displayName: String,
    ): FileBD?

    @Query("SELECT path FROM file")
    suspend fun getAllPaths(): List<String>

    @Query("SELECT * FROM file WHERE sync_id = :syncId")
    suspend fun getFileBySyncId(syncId: String): FileBD?

    @Upsert
    suspend fun upsertFiles(files:List<FileBD>)

    @Query("DELETE FROM file WHERE id in(:ids)")
    suspend fun deleteFiles(ids:List<Int>)

}
