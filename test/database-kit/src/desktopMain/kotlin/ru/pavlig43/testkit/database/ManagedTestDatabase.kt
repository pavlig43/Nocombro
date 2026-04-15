package ru.pavlig43.testkit.database

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.testkit.deleteTempPath
import java.nio.file.Path

class ManagedTestDatabase(
    val database: NocombroDatabase,
    private val rootPath: Path,
) : AutoCloseable {

    override fun close() {
        database.close()
        deleteTempPath(rootPath)
    }
}
