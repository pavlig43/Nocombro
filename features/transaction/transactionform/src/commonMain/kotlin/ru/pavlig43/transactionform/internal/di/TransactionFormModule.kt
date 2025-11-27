package ru.pavlig43.transactionform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transaction.ProductTransactionIn
import ru.pavlig43.database.data.transaction.ProductTransactionOut
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.transactionform.api.TransactionDependencies
import ru.pavlig43.transactionform.internal.data.CreateTransactionRepository
import ru.pavlig43.transactionform.internal.data.UpdateTransactionRepository

internal fun createTransactionFormModule(dependencies: TransactionDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<CreateTransactionRepository> { CreateTransactionRepository(get()) }
        single<IUpdateRepository<ProductTransactionIn, ProductTransactionOut>> { UpdateTransactionRepository(get()) }
    }
)




