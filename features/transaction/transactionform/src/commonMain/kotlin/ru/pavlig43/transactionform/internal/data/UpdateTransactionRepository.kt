package ru.pavlig43.transactionform.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transaction.ProductTransactionIn
import ru.pavlig43.database.data.transaction.ProductTransactionOut
import ru.pavlig43.form.api.data.IUpdateRepository

internal class UpdateTransactionRepository(
    db: NocombroDatabase
) : IUpdateRepository<ProductTransactionIn, ProductTransactionOut> {
    private val dao = db.productTransactionDao
    override suspend fun getInit(id: Int): RequestResult<ProductTransactionOut> {
        return dbSafeCall("UpdateTransactionRepository") { dao.getTransactionWithProducts(id) }
    }

    override suspend fun update(changeSet: ChangeSet<ProductTransactionIn>) {
        if (changeSet.old == changeSet.new) return
        dao.updateTransaction(changeSet.new,changeSet.new.products)
    }
}