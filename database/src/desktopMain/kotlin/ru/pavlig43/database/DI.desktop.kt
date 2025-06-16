package ru.pavlig43.database

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformDataBaseModule(): Module = module {
    single<NocombroDatabase> { getNocombroDatabase(getDataBaseBuilder()) }

}

