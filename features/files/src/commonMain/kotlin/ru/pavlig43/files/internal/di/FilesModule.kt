package ru.pavlig43.files.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.internal.data.FilesRepository

internal fun filesModule(dependencies: FilesDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        singleOf(::FilesRepository)

    }
)