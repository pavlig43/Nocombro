package ru.pavlig43.database.data.batch

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

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

    ],
    indices = [Index(value = ["sync_id"], unique = true)]
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

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
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
    val declaration: Declaration,
    @Relation(
        entity = BatchCostPriceEntity::class,
        parentColumn = "id",
        entityColumn = "batch_id"
    )
    val costPrice: BatchCostPriceEntity?  // null если нет себестоимости
)
