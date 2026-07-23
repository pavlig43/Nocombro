package ru.pavlig43.database.data.files

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert


/**
 * Room DAO для локальных метаданных файлов.
 */
@Dao
interface FileDao {
    @Query("SELECT * FROM file")
    suspend fun getAllFiles(): List<FileBD>

    @Query("SELECT * FROM file WHERE owner_id = :ownerId AND owner_type = :ownerFileType AND deleted_at IS NULL")
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

    /**
     * Возвращает пути активных файлов без tombstone-строк.
     */
    @Query("SELECT path FROM file WHERE deleted_at IS NULL")
    suspend fun getActivePaths(): List<String>

    /**
     * Возвращает ключи объектов у строк, где есть S3-метаданные.
     */
    @Query("SELECT remote_object_key FROM file WHERE remote_object_key IS NOT NULL")
    suspend fun getAllRemoteObjectKeys(): List<String>

    @Query("SELECT * FROM file WHERE sync_id = :syncId")
    suspend fun getFileBySyncId(syncId: String): FileBD?

    @Upsert
    suspend fun upsertFiles(files:List<FileBD>)

    @Query("DELETE FROM file WHERE id in(:ids)")
    suspend fun deleteFiles(ids:List<Int>)

    @Query("DELETE FROM file WHERE sync_id IN (:syncIds)")
    suspend fun deleteFilesBySyncIds(syncIds: List<String>)

}
