package ru.pavlig43.testkit.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.seedDatabase
import ru.pavlig43.testkit.createTempPath
import java.nio.file.Path

fun createManagedTestDatabase(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    rootPath: Path = createTempPath(),
): ManagedTestDatabase {
    val databasePath = rootPath.resolve("nocombro-test.db")
    val database = Room.databaseBuilder<NocombroDatabase>(
        name = databasePath.toString(),
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .build()

    return ManagedTestDatabase(
        database = database,
        rootPath = rootPath,
    )
}

suspend fun createSeededManagedTestDatabase(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    rootPath: Path = createTempPath(),
): ManagedTestDatabase {
    val managed = createManagedTestDatabase(
        queryDispatcher = queryDispatcher,
        rootPath = rootPath,
    )
    seedDatabase(managed.database)
    return managed
}

suspend fun <T> withEmptyTestDatabase(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend (NocombroDatabase) -> T,
): T {
    val managed = createManagedTestDatabase(queryDispatcher = queryDispatcher)
    return try {
        block(managed.database)
    } finally {
        managed.close()
    }
}

suspend fun <T> withSeededTestDatabase(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend (NocombroDatabase) -> T,
): T {
    val managed = createSeededManagedTestDatabase(queryDispatcher = queryDispatcher)
    return try {
        block(managed.database)
    } finally {
        managed.close()
    }
}
