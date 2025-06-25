package ru.pavlig43.datastore

import java.io.File

fun createDataStoreDesktop() = createDataStore {
    val file = File(System.getProperty("java.io.tmpdir"), DATASTORE_FILE_NAME)
    file.absolutePath
}