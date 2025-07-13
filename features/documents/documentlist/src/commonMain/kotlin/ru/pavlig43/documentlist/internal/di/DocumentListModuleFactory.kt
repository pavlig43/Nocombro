package ru.pavlig43.documentlist.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.documentlist.api.IDocumentLisDependencies

internal fun createModule(dependencies: IDocumentLisDependencies) = listOf(
    documentListModule,
    module {
        single<DocumentDao> { dependencies.documentDao }
    }
)