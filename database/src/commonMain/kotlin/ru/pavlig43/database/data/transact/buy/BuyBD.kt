package ru.pavlig43.database.data.transact.buy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.transact.Transact

const val BUY_TABLE_NAME = "buy"

/**
 * Сущность для хранения информации о покупке в базе данных.
 *
 * Связывает транзакцию с движением партии, добавляя цену и комментарий.
 *
 * @property transactionId Идентификатор транзакции (внешний ключ к [Transact])
 * @property movementId Идентификатор движения партии (внешний ключ к [BatchMovement])
 * @property price Цена покупки в копейках
 * @property comment Комментарий к покупке
 * @property id Уникальный идентификатор записи
 */
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
            childColumns = ["movement_id"]
        )
    ]
)
data class BuyBDIn(
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

/**
 * Выходное DTO для отображения информации о покупке.
 *
 * Содержит полную информацию о покупке с denormalized данными
 * о связанной партии, продукте и декларации.
 *
 * @property transactionId Идентификатор транзакции
 * @property count Количество товаров
 * @property batchId Идентификатор партии
 * @property movementId Идентификатор движения партии
 * @property productId Идентификатор продукта
 * @property productName Название продукта
 * @property declarationId Идентификатор декларации
 * @property declarationName Название декларации
 * @property vendorName Название поставщика
 * @property dateBorn Дата создания партии
 * @property price Цена в копейках
 * @property comment Комментарий к покупке
 * @property id Уникальный идентификатор
 */
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
