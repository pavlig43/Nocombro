package ru.pavlig43.testkit.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.MIGRATION_1_2
import ru.pavlig43.database.MIGRATION_2_3
import ru.pavlig43.database.MIGRATION_3_4
import ru.pavlig43.database.MIGRATION_4_5
import ru.pavlig43.database.MIGRATION_5_6
import ru.pavlig43.database.MIGRATION_6_7
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.seedDatabase
import ru.pavlig43.testkit.createTempPath
import java.nio.file.Files
import java.nio.file.Path

fun createManagedTestDatabase(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    rootPath: Path = createTempPath(),
): ManagedTestDatabase {
    val databasePath = rootPath.resolve("nocombro-test.db")
    val database = Room.databaseBuilder<NocombroDatabase>(
        name = databasePath.toString(),
    )
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .addMigrations(MIGRATION_4_5)
        .addMigrations(MIGRATION_5_6)
        .addMigrations(MIGRATION_6_7)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .build()

    return ManagedTestDatabase(
        database = database,
        rootPath = rootPath,
    )
}

suspend fun createManagedCopiedDatabase(
    sourceDatabasePath: Path,
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    rootPath: Path = createTempPath(),
): ManagedTestDatabase {
    require(Files.exists(sourceDatabasePath)) {
        "Database file does not exist: $sourceDatabasePath"
    }

    val targetDatabasePath = rootPath.resolve(sourceDatabasePath.fileName.toString())
    Files.copy(sourceDatabasePath, targetDatabasePath)
    copyIfExists(sourceDatabasePath.resolveSibling("${sourceDatabasePath.fileName}-wal"), targetDatabasePath.resolveSibling("${targetDatabasePath.fileName}-wal"))
    copyIfExists(sourceDatabasePath.resolveSibling("${sourceDatabasePath.fileName}-shm"), targetDatabasePath.resolveSibling("${targetDatabasePath.fileName}-shm"))

    val database = Room.databaseBuilder<NocombroDatabase>(
        name = targetDatabasePath.toString(),
    )
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .addMigrations(MIGRATION_4_5)
        .addMigrations(MIGRATION_5_6)
        .addMigrations(MIGRATION_6_7)
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

suspend fun <T> withCopiedTestDatabase(
    sourceDatabasePath: Path,
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend (NocombroDatabase) -> T,
): T {
    val managed = createManagedCopiedDatabase(
        sourceDatabasePath = sourceDatabasePath,
        queryDispatcher = queryDispatcher,
    )
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

private fun copyIfExists(source: Path, target: Path) {
    if (Files.exists(source)) {
        Files.copy(source, target)
    }
}
