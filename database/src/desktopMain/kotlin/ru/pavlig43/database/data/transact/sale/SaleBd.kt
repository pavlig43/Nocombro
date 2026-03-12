package ru.pavlig43.database.data.transact.sale

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.batch.BatchMovement
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

    @ColumnInfo("client_id", index = true)
    val clientId: Int,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
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
    val count: Int,
    val batchId: Int,
    val movementId: Int,
    val productId: Int,
    val productName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val clientName: String,
    val clientId: Int,
    val price: Int,
    val comment: String,
    override val id: Int
) : CollectionObject