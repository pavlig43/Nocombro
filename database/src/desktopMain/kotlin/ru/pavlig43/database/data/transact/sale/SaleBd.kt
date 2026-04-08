package ru.pavlig43.database.data.transact.sale

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.vendor.Vendor

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
        ),
        ForeignKey(
            entity = Vendor::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class SaleBDIn(
    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @ColumnInfo("movement_id", index = true)
    val movementId: Int,

    @ColumnInfo("price")
    val price: Long, // Цена в копейках

    @ColumnInfo("comment")
    val comment: String,

    @ColumnInfo("client_id", index = true)
    val clientId: Int,

    @ColumnInfo("nds_percent")
    val ndsPercent: Int = 0,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
,
    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : CollectionObject

/**
 * Выходное DTO для отображения информации о продаже.
 *
 * Содержит полную информацию о продаже с denormalized данными
 * о связанной партии, продукте и клиенте.
 *
 * @property transactionId Идентификатор транзакции
 * @property count Количество товаров
 * @property batchId Идентификатор партии
 * @property movementId Идентификатор движения партии
 * @property productId Идентификатор продукта
 * @property productName Название продукта
 * @property vendorName Название поставщика (из партии)
 * @property dateBorn Дата создания партии
 * @property clientName Имя клиента
 * @property clientId Идентификатор клиента
 * @property price Цена в копейках
 * @property comment Комментарий к продаже
 * @property id Уникальный идентификатор
 */
data class SaleBDOut(
    val transactionId: Int,
    val count: Long,
    val batchId: Int,
    val movementId: Int,
    val productId: Int,
    val productName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val clientName: String,
    val clientId: Int,
    val price: Long,
    val comment: String,
    val ndsPercent: Int = 0,
    override val id: Int
) : CollectionObject
