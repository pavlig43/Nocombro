package ru.pavlig43.database.data.transact.pf

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.transact.Transact

internal const val PF_TABLE_NAME = "pf"

/**
 * Сущность для хранения информации о полуфабрикате (Product Frame) для OPZS транзакции.
 *
 * OPZS (Отчёт производства за смену) создаёт BatchMovement для компонентов (OUTGOING)
 * и полуфабриката (INCOMING).
 *
 * @property transactionId Идентификатор транзакции (внешний ключ к [Transact])
 * @property productId Идентификатор производимого продукта
 * @property productName Название продукта (denormalized)
 * @property declarationId Идентификатор декларации качества
 * @property declarationName Название декларации (denormalized)
 * @property count Количество произведённого полуфабриката
 * @property id Уникальный идентификатор записи
 */
@Entity(
    tableName = PF_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Transact::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PfBD(
    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("product_name")
    val productName: String,

    @ColumnInfo("declaration_id")
    val declarationId: Int,

    @ColumnInfo("declaration_name")
    val declarationName: String,

    @ColumnInfo("count")
    val count: Int,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
) : SingleItem
