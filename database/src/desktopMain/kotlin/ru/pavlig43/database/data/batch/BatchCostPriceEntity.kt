package ru.pavlig43.database.data.batch

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "batch_cost_price",
    foreignKeys = [
        ForeignKey(
            entity = BatchBD::class,
            parentColumns = ["id"],
            childColumns = ["batch_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BatchCostPriceEntity(
    @PrimaryKey
    @ColumnInfo(name = "batch_id")
    val batchId: Int,

    /**
     * Цена за целую единицу(шт или кг) коп/кг
     * Почему не коп/грамм? при расчёте себестоимости партии например: при 100 кг и 50 рублях доставки теряется доставка,
     * так как при распределении на 100 кг получается меньше 0.5 копеек на 1 грамм
     */
    @ColumnInfo(name = "cost_price_per_unit")
    val costPricePerUnit: Int
)