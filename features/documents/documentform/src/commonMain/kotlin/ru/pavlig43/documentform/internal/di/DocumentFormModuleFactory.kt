package ru.pavlig43.documentform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.dao.DocumentFilesDao

private fun baseModule(dependencies: IDocumentFormDependencies) = module {
    single<DataBaseTransaction> { dependencies.transaction }
    single<DocumentDao> { dependencies.documentDao }
    single<DocumentFilesDao> { dependencies.documentFilesDao }
}
internal fun createDocumentFormModule(dependencies: IDocumentFormDependencies) = listOf(
    baseModule(dependencies),
    documentFormModule
)

