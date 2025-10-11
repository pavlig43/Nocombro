package ru.pavlig43.database.data.vendor

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.FileData

@Entity(
    tableName = "vendor_file",
    foreignKeys = [ForeignKey(
        entity = Vendor::class,
        parentColumns = ["id"],
        childColumns = ["vendor_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class VendorFile(

    @ColumnInfo("vendor_id")
    val vendorId: Int,

    @ColumnInfo("path")
    override val path: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
): FileData