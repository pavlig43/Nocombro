package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.rootnocombro.api.RootDependencies


internal fun getDatabaseModule(rootDependencies: RootDependencies) = listOf(
    module {
        single<NocombroDatabase> { rootDependencies.database }
        single<TransactionExecutor> { NocombroTransactionExecutor(get()) }

    }
)
