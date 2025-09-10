package ru.pavlig43.productform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.data.ItemListRepository
import ru.pavlig43.productform.api.IProductFormDependencies

private fun baseModule(dependencies: IProductFormDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
    single<DataBaseTransaction> { dependencies.transaction }
    single<ItemListRepository<Document, DocumentType>> {dependencies.documentListRepository  }

}
internal fun createProductFormModule(dependencies: IProductFormDependencies) = listOf(
    baseModule(dependencies),
    productFormModule
)


