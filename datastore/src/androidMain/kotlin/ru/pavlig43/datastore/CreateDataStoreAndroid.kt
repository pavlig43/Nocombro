package ru.pavlig43.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createDataStoreAndroid(context: Context): DataStore<Preferences> = createDataStore {
    context.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath
}