package ru.pavlig43.database.data.files

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert


@Dao
abstract class FileDao {
    @Query("SELECT * FROM file WHERE owner_id = :ownerId AND owner_type =:ownerFileType")
    abstract suspend fun getFiles(ownerId: Int,ownerFileType: OwnerType):List<FileBD>

    @Upsert
    abstract suspend fun upsertFiles(files:List<FileBD>)

    @Query("DELETE FROM file WHERE id in(:ids)")
    abstract suspend fun deleteFiles(ids:List<Int>)





}