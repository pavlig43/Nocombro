package ru.pavlig43.document.internal.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.document.api.IDocumentDependencies

internal fun createModule(documentDependencies: IDocumentDependencies): List<Module> = listOf(
    documentModule,
    module {
        single<DocumentDao>{ documentDependencies.documentDao }
    }
)