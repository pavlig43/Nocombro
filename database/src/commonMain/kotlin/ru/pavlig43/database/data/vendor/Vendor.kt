package ru.pavlig43.database.data.vendor

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.Item

const val VENDOR_TABLE_NAME = "vendor"

@Entity(VENDOR_TABLE_NAME)
data class Vendor(

    @ColumnInfo("display_name")
    override val displayName: String = "",

    override val type: VendorType,

    @ColumnInfo("created_at")
    override val createdAt: Long,

    override val comment:String ="",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    ) : Item
