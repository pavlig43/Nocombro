package ru.pavlig43.datastore.di

import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pavlig43.datastore.SettingsRepository

 internal expect fun platformDataStore(): Module

fun getSettingsRepository(): Module {
    return module {
        includes(platformDataStore(),module { singleOf(::SettingsRepository) })

    }
}