package ru.pavlig43.documentform.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.addfile.api.IAddFileDependencies
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.database.data.document.dao.DocumentDao

private fun baseModule(dependencies: IDocumentFormDependencies) = module {
    single<DocumentDao> { dependencies.documentDao }
    singleOf(::AddFileDependencies) bind IAddFileDependencies::class
}
internal fun createDocumentFormModule(dependencies: IDocumentFormDependencies) = listOf(
    baseModule(dependencies),
    createDocumentFormModule
)

private class AddFileDependencies(override val documentDao: DocumentDao):IAddFileDependencies