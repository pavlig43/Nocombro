package ru.pavlig43.testkit.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import java.nio.file.Path

class DatabaseTestRuntime(
    private val managedDatabase: ManagedTestDatabase,
) : AutoCloseable {

    val database: NocombroDatabase = managedDatabase.database
    val transactionExecutor: NocombroTransactionExecutor by lazy { NocombroTransactionExecutor(database) }
    val filesDependencies: FilesDependencies by lazy {
        FilesDependencies(
            db = database,
            remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
        )
    }
    val immutableTableDependencies: ImmutableTableDependencies by lazy {
        ImmutableTableDependencies(database)
    }

    override fun close() {
        managedDatabase.close()
    }
}

suspend fun createEmptyDatabaseRuntime(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    rootPath: Path? = null,
): DatabaseTestRuntime {
    val managed = if (rootPath == null) {
        createManagedTestDatabase(queryDispatcher = queryDispatcher)
    } else {
        createManagedTestDatabase(queryDispatcher = queryDispatcher, rootPath = rootPath)
    }
    return DatabaseTestRuntime(managed)
}

suspend fun createSeededDatabaseRuntime(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    rootPath: Path? = null,
): DatabaseTestRuntime {
    val managed = if (rootPath == null) {
        createSeededManagedTestDatabase(queryDispatcher = queryDispatcher)
    } else {
        createSeededManagedTestDatabase(queryDispatcher = queryDispatcher, rootPath = rootPath)
    }
    return DatabaseTestRuntime(managed)
}

suspend fun createCopiedDatabaseRuntime(
    sourceDatabasePath: Path,
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    rootPath: Path? = null,
): DatabaseTestRuntime {
    val managed = if (rootPath == null) {
        createManagedCopiedDatabase(
            sourceDatabasePath = sourceDatabasePath,
            queryDispatcher = queryDispatcher,
        )
    } else {
        createManagedCopiedDatabase(
            sourceDatabasePath = sourceDatabasePath,
            queryDispatcher = queryDispatcher,
            rootPath = rootPath,
        )
    }
    return DatabaseTestRuntime(managed)
}

suspend fun <T> withSeededDatabaseRuntime(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend (DatabaseTestRuntime) -> T,
): T {
    val runtime = createSeededDatabaseRuntime(queryDispatcher = queryDispatcher)
    return try {
        block(runtime)
    } finally {
        runtime.close()
    }
}

suspend fun <T> withCopiedDatabaseRuntime(
    sourceDatabasePath: Path,
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend (DatabaseTestRuntime) -> T,
): T {
    val runtime = createCopiedDatabaseRuntime(
        sourceDatabasePath = sourceDatabasePath,
        queryDispatcher = queryDispatcher,
    )
    return try {
        block(runtime)
    } finally {
        runtime.close()
    }
}

suspend fun <T> withEmptyDatabaseRuntime(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend (DatabaseTestRuntime) -> T,
): T {
    val runtime = createEmptyDatabaseRuntime(queryDispatcher = queryDispatcher)
    return try {
        block(runtime)
    } finally {
        runtime.close()
    }
}
