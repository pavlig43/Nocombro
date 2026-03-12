package ru.pavlig43.database.data.batch

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product

@Entity(
    tableName = "batch",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Declaration::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
            onDelete = ForeignKey.RESTRICT
        )

    ]
)
data class BatchBD(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo("product_id", index = true)
    val productId: Int,

    @ColumnInfo("date_born")
    val dateBorn: LocalDate,

    @ColumnInfo("declaration_id", index = true)
    val declarationId: Int,
)
data class BatchOut(
    @Embedded
    val batch: BatchBD,
    @Relation(
        entity = Product::class,
        parentColumn = "product_id",
        entityColumn = "id"
    )
    val product: Product,
    @Relation(
        entity = Declaration::class,
        parentColumn = "declaration_id",
        entityColumn = "id"
    )
    val declaration: Declaration
)