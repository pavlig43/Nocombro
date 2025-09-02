package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.FileData

@Entity(
    tableName = "product_file",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["product_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class ProductFile(

    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("path")
    override val path: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
): FileData