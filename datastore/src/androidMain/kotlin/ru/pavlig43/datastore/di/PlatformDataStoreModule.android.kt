package ru.pavlig43.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.datastore.createDataStoreAndroid

internal actual fun platformDataStore() = module {
    single<DataStore<Preferences>> { createDataStoreAndroid(context = get()) }
}