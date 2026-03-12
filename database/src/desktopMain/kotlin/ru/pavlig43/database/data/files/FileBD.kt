package ru.pavlig43.database.data.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.pavlig43.core.model.CollectionObject

@Entity(
    tableName = "file"
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
): CollectionObject
