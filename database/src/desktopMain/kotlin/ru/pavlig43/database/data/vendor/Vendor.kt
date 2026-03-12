package ru.pavlig43.database.data.vendor

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.pavlig43.core.model.SingleItem

const val VENDOR_TABLE_NAME = "vendor"

@Entity(VENDOR_TABLE_NAME)
data class Vendor(

    @ColumnInfo("display_name")
    val displayName: String = "",

    val comment:String ="",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    ) : SingleItem
