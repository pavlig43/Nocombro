package ru.pavlig43.database.data.batch

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
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
            onDelete = ForeignKey.Companion.RESTRICT
        ),
        ForeignKey(
            entity = Declaration::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
            onDelete = ForeignKey.RESTRICT
        )

    ]
)
data class Batch(
    @PrimaryKey val id: Int,
    val productId: Int,
    val dateBorn: LocalDate,
    val declarationId: Int,
)