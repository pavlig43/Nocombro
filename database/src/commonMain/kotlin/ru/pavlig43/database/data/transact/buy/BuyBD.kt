package ru.pavlig43.database.data.transact.buy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.transact.Transact

const val BUY_TABLE_NAME = "buy"

@Entity(
    tableName = BUY_TABLE_NAME,
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
            childColumns = ["movement_id"],
            onDelete = ForeignKey.RESTRICT
        )

    ]
)
data class BuyBDIn(
    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @ColumnInfo("movement_id")
    val movementId: Int,

    @ColumnInfo("price")
    val price: Int, // Цена в копейках

    @ColumnInfo("comment")
    val comment: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
) : CollectionObject






data class BuyBDOut(
    val transactionId: Int,
    val count: Int,
    val batchId: Int,
    val movementId: Int,
    val productId: Int,
    val productName: String,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val price: Int,
    val comment: String,
    override val id: Int
) : CollectionObject
