package ru.pavlig43.database.data.batch

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.transact.Transact

@Entity(
    tableName = "batch_movement",
    foreignKeys = [
        ForeignKey(
            entity = BatchBD::class,
            parentColumns = ["id"],
            childColumns = ["batch_id"],
        ),
        ForeignKey(
            entity = Transact::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = CASCADE
        )
    ]
)
data class BatchMovement(
    @ColumnInfo("batch_id", index = true)
    val batchId: Int,

    @ColumnInfo("movement_type")
    val movementType: MovementType,

    @ColumnInfo("count")
    val count: Int,

    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
) : CollectionObject
