package ru.pavlig43.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.datastore.createDataStoreDesktop

internal actual fun platformDataStore(): Module = module {
    single<DataStore<Preferences>> { createDataStoreDesktop() }
}