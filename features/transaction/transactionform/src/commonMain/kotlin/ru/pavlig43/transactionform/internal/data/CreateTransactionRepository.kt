package ru.pavlig43.transactionform.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.database.data.transaction.ProductTransactionIn
import ru.pavlig43.database.data.transaction.TransactionRow


internal class CreateTransactionRepository(
    private val db:NocombroDatabase,
) {
    suspend fun create(transaction: ProductTransaction,rows:List<TransactionRow>): RequestResult<Int> {
        val dao = db.productTransactionDao
        return dbSafeCall("CreateTransactionRepository"){
            dao.createTransactionWithRow(ProductTransactionIn(transaction,rows))
        }

    }
}