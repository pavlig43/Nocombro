package ru.pavlig43.database.data.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

@Entity(
    tableName = "file",
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class FileBD(
    @ColumnInfo("owner_id")
    val ownerId: Int,

    @ColumnInfo("owner_type")
    val ownerFileType: OwnerType,

    @ColumnInfo("path")
    val path: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
): CollectionObject
