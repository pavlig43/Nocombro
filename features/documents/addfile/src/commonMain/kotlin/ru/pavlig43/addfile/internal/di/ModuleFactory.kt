package ru.pavlig43.addfile.internal.di

import org.koin.dsl.module
import ru.pavlig43.addfile.api.IAddFileDependencies
import ru.pavlig43.database.data.document.dao.DocumentDao

internal fun createAddFileModuleFactory(dependencies: IAddFileDependencies) = listOf(
    module {
        single<DocumentDao> { dependencies.documentDao }
    },
    addFileModule
)