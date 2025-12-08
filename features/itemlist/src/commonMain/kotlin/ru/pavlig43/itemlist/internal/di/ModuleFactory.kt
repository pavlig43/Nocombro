package ru.pavlig43.itemlist.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.itemlist.internal.component.DeclarationListRepository
import ru.pavlig43.itemlist.internal.component.DocumentListRepository
import ru.pavlig43.itemlist.internal.component.ProductListRepository
import ru.pavlig43.itemlist.internal.component.TransactionListRepository
import ru.pavlig43.itemlist.internal.component.VendorListRepository

internal fun moduleFactory(dependencies: ItemListDependencies) = listOf(
    module{
        single<NocombroDatabase> { dependencies.db }
        factoryOf(::DocumentListRepository)
        factoryOf(::DeclarationListRepository)
        factoryOf(::ProductListRepository)
        factoryOf(::VendorListRepository)
        factoryOf(::TransactionListRepository)
    }
)