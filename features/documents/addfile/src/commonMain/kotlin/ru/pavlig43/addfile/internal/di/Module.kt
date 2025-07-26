package ru.pavlig43.addfile.internal.di

import org.koin.dsl.module
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.addfile.internal.data.InitFilesPathRepository
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository

val addFileModule = module {
    single<IInitDataRepository<List<DocumentFilePath>,List<AddedFile>>> { InitFilesPathRepository(get()) }
}

