package ru.pavlig43.addfile.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.addfile.internal.data.InitFilesPathRepository
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository

val addFileModule = module {
    singleOf(::InitFilesPathRepository) bind IInitDataRepository::class
}