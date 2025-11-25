package ru.pavlig43.transactionform.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.database.data.transaction.ProductTransactionIn
import ru.pavlig43.database.data.transaction.ProductTransactionOut
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.transactionform.internal.data.CreateTransactionRepository
import ru.pavlig43.transactionform.internal.data.UpdateTransactionRepository

internal val transactionFormModule = module {

    single<CreateTransactionRepository> { CreateTransactionRepository(get()) }
    single<IUpdateRepository<ProductTransactionIn, ProductTransactionOut>> { UpdateTransactionRepository(get()) }
}




