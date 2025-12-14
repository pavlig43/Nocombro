package ru.pavlig43.itemlist.statik.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.statik.internal.component.DeclarationListRepository
import ru.pavlig43.itemlist.statik.internal.component.DocumentListRepository
import ru.pavlig43.itemlist.statik.internal.component.ProductListRepository
import ru.pavlig43.itemlist.statik.internal.component.TransactionListRepository
import ru.pavlig43.itemlist.statik.internal.component.VendorListRepository

internal fun moduleFactory(dependencies: ItemStaticListDependencies) = listOf(
    module{
        single<NocombroDatabase> { dependencies.db }
        factoryOf(::DocumentListRepository)
        factoryOf(::DeclarationListRepository)
        factoryOf(::ProductListRepository)
        factoryOf(::VendorListRepository)
        factoryOf(::TransactionListRepository)
    }
)