package ru.pavlig43.documentform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.documentform.api.IDocumentFormDependencies

private fun baseModule(dependencies: IDocumentFormDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
    single<DataBaseTransaction> { dependencies.transaction }
}
internal fun createDocumentFormModule(dependencies: IDocumentFormDependencies) = listOf(
    baseModule(dependencies),
    documentFormModule
)

