package ru.pavlig43.database.data.declaration

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.database.data.vendor.Vendor

const val DECLARATIONS_TABLE_NAME = "declaration"

@Entity(
    tableName = DECLARATIONS_TABLE_NAME,
    foreignKeys = [ForeignKey(
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
     val createdAt: Long,

    @ColumnInfo("vendor_id")
    val vendorId: Int,

    @ColumnInfo("vendor_name")
    val vendorName: String,

    @ColumnInfo("best_before")
    val bestBefore: Long,

    @ColumnInfo("observe_from_notification")
    val observeFromNotification:Boolean,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

) : GenericItem



