package ru.pavlig43.database.data.batch

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.transact.Transact

const val BATCH_MOVEMENT_TABLE_NAME = "batch_movement"

@Entity(
    tableName = BATCH_MOVEMENT_TABLE_NAME,
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
    ],
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class BatchMovement(
    @ColumnInfo("batch_id", index = true)
    val batchId: Int,

    @ColumnInfo("movement_type")
    val movementType: MovementType,

    @ColumnInfo("count")
    val count: Long,

    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : CollectionObject
