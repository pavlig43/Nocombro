package ru.pavlig43.database.data.batch

import androidx.room.Embedded
import androidx.room.Relation
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product

/**
 * DTO партии с вычисленным остатком из движений (BatchMovement).
 *
 * Остаток (balance) вычисляется как сумма INCOMING - сумма OUTGOING.
 *
 * @property batch Партия
 * @property product Связанный продукт
 * @property declaration Связанная декларация
 * @property balance Вычисленный остаток (положительный = наличие, отрицательный = перерасход)
 */
data class BatchWithBalanceOut(
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

    val balance: Int
)
