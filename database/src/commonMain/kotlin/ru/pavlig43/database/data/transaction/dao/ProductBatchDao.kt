package ru.pavlig43.database.data.transaction.dao

//internal data class InternalProductBatchTransactionRow(
//
//    @Embedded
//    val productBatchTransaction: TransactionProductBDIn,
//
//    @Relation(parentColumn = "product_id", entityColumn = "id")
//    val product: Product,
//
//    @Relation(parentColumn = "declaration_id", entityColumn = "id")
//    val declaration: Declaration,
//)

//private fun InternalProductBatchTransactionRow.toBDOut(): TransactionProductBDOut {
//    return TransactionProductBDOut(
//        productId = product.id,
//        productName = product.displayName,
//        declarationId = declaration.id,
//        declarationName = declaration.displayName,
//        vendorName = declaration.vendorName,
//        dateBorn = productBatchTransaction.dateBorn,
//        batch = productBatchTransaction.batch,
//        id = productBatchTransaction.id
//    )
//}