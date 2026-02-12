package ru.pavlig43.database.data.declaration

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.vendor.Vendor

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
    ]
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

    ) : SingleItem {
    @Ignore
    val isActual = bestBefore > getCurrentLocalDate()
}



