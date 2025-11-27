package ru.pavlig43.rootnocombro.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.NocombroTransaction
import ru.pavlig43.rootnocombro.api.RootDependencies


internal fun getDatabaseModule(rootDependencies: RootDependencies) = listOf(
    module {
        single<NocombroDatabase> { rootDependencies.database }
        single<DataBaseTransaction> { NocombroTransaction(get()) }

    }
)
