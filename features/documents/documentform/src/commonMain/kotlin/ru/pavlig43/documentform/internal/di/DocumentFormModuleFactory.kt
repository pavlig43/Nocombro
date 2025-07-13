package ru.pavlig43.documentform.internal.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.database.data.document.dao.DocumentDao

internal fun createModule(dependencies: IDocumentFormDependencies): List<Module> = listOf(
    documentFormModule,
    module {
        single<DocumentDao>{ dependencies.documentDao }
    }
)