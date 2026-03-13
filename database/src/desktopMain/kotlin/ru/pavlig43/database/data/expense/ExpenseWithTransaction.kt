package ru.pavlig43.database.data.expense

import androidx.room.Embedded
import androidx.room.Relation
import ru.pavlig43.database.data.transact.Transact

data class ExpenseWithTransaction(
    @Embedded
    val expense: ExpenseBD,

    @Relation(
        entity = Transact::class,
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val transaction: Transact?
)
