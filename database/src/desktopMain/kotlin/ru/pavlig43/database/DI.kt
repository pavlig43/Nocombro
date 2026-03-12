package ru.pavlig43.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

fun platformDataBaseModule(): Module = module {
    single<NocombroDatabase> { getNocombroDatabase() }

}

fun getNocombroDatabase(): NocombroDatabase {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "nocombro.db")

    val builder =  Room.databaseBuilder<NocombroDatabase>(
        name = dbFile.absolutePath,
    )
    val database = builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    CoroutineScope(Dispatchers.IO).launch {
        seedDatabase(database)
    }
    return database
}
