package ru.pavlig43.vendor.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.data.ItemFilter
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType
import ru.pavlig43.vendor.api.IVendorFormDependencies

private fun baseModule(dependencies: IVendorFormDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
    single<DataBaseTransaction> { dependencies.transaction }
}

internal fun createVendorFormModule(dependencies: IVendorFormDependencies) = listOf(
    baseModule(dependencies),
    vendorFormModule
)