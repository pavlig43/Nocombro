package ru.pavlig43.database.data.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val FILE_TABLE_NAME = "file"

@Entity(
    tableName = FILE_TABLE_NAME,
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class FileBD(
    @ColumnInfo("owner_id")
    val ownerId: Int,

    @ColumnInfo("owner_type")
    val ownerFileType: OwnerType,

    @ColumnInfo("display_name")
    val displayName: String,

    @ColumnInfo("path")
    val path: String,

    @ColumnInfo("remote_object_key")
    val remoteObjectKey: String? = null,

    @ColumnInfo("remote_storage_provider")
    val remoteStorageProvider: String? = null,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
): CollectionObject
