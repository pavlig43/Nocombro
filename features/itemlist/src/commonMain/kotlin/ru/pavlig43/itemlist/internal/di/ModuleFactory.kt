package ru.pavlig43.itemlist.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.api.component.refactoring.ItemListDependencies
import ru.pavlig43.itemlist.api.data.DeclarationListRepository
import ru.pavlig43.itemlist.api.data.DocumentListRepository
import ru.pavlig43.itemlist.api.data.ProductListRepository
import ru.pavlig43.itemlist.api.data.VendorListRepository

internal fun moduleFactory(dependencies: ItemListDependencies) = listOf(
    module{
        single<NocombroDatabase> { dependencies.db }
        factoryOf(::DocumentListRepository)
        factoryOf(::DeclarationListRepository)
        factoryOf(::ProductListRepository)
        factoryOf(::VendorListRepository)
    }
)