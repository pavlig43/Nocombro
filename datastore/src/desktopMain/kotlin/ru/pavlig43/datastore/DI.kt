package ru.pavlig43.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

fun getSettingsRepository(): Module {
    return module {
        single<DataStore<Preferences>> { createDataStoreDesktop() }
        single { SettingsRepository(get()) }
    }
}

private fun createDataStoreDesktop(): DataStore<Preferences> {
    val path = System.getProperty("java.io.tmpdir").toPath() / DATASTORE_FILE_NAME
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { path }
    )
}

private const val DATASTORE_FILE_NAME = "nocombro.preferences_pb"