package ru.pavlig43.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pavlig43.datastore.SettingsRepository
import ru.pavlig43.datastore.createDataStoreDesktop

internal fun platformDataStore(): Module = module {
    single<DataStore<Preferences>> { createDataStoreDesktop() }
}

fun getSettingsRepository(): Module {
    return module {
        includes(platformDataStore(),module { singleOf(::SettingsRepository) })

    }
}