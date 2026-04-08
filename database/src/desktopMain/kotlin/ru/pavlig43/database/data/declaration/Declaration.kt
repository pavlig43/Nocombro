package ru.pavlig43.database.data.declaration

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.getCurrentLocalDate

const val DECLARATIONS_TABLE_NAME = "declaration"

@Entity(
    tableName = DECLARATIONS_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Vendor::class,
            parentColumns = ["id"],
            childColumns = ["vendor_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class Declaration(

    @ColumnInfo("display_name")
    val displayName: String,

    @ColumnInfo("created_at")
    val createdAt: LocalDate,

    @ColumnInfo("vendor_id", index = true)
    val vendorId: Int,

    @ColumnInfo("vendor_name")
    val vendorName: String,

    @ColumnInfo("born_date")
    val bornDate: LocalDate,

    @ColumnInfo("best_before")
    val bestBefore: LocalDate,

    @ColumnInfo("observe_from_notification")
    val observeFromNotification: Boolean,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,

    ) : SingleItem {
    @Ignore
    val isActual = bestBefore > getCurrentLocalDate()
}



