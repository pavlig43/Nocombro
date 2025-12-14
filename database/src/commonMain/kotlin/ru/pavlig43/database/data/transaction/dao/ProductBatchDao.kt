package ru.pavlig43.database.data.transaction.dao

import androidx.room.Embedded
import androidx.room.Relation
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transaction.TransactionProductBDIn
import ru.pavlig43.database.data.transaction.TransactionProductBDOut

internal data class InternalProductBatchTransactionRow(

    @Embedded
    val productBatchTransaction: TransactionProductBDIn,

    @Relation(parentColumn = "product_id", entityColumn = "id")
    val product: Product,

    @Relation(parentColumn = "declaration_id", entityColumn = "id")
    val declaration: Declaration,
)

private fun InternalProductBatchTransactionRow.toBDOut(): TransactionProductBDOut {
    return TransactionProductBDOut(
        productId = product.id,
        productName = product.displayName,
        declarationId = declaration.id,
        declarationName = declaration.displayName,
        vendorName = declaration.vendorName,
        dateBorn = productBatchTransaction.dateBorn,
        batch = productBatchTransaction.batch,
        id = productBatchTransaction.id
    )
}