package ru.pavlig43.database.data.vendor

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val VENDOR_TABLE_NAME = "vendor"

@Entity(
    tableName = VENDOR_TABLE_NAME,
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class Vendor(

    @ColumnInfo("display_name")
    val displayName: String = "",

    val comment:String ="",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,

    ) : SingleItem
