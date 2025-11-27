package ru.pavlig43.declarationform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.declarationform.api.IDeclarationDependencies
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType

private fun baseModule(dependencies: IDeclarationDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
    single<DataBaseTransaction> { dependencies.transaction }
}
internal fun createDeclarationFormModule(dependencies: IDeclarationDependencies) = listOf(
    baseModule(dependencies),
    declarationFormModule
)