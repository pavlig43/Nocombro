package ru.pavlig43.database.data.transact.sale

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.transact.Transact

const val SALE_TABLE_NAME = "sale"


@Entity(
    tableName = SALE_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Transact::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BatchMovement::class,
            parentColumns = ["id"],
            childColumns = ["movement_id"]
        )
    ]
)
data class SaleBDIn(
    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @ColumnInfo("movement_id", index = true)
    val movementId: Int,

    @ColumnInfo("price")
    val price: Int, // Цена в копейках

    @ColumnInfo("comment")
    val comment: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
) : CollectionObject