package ru.pavlig43.testkit

import java.nio.file.Path
import kotlin.io.path.Path

const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

fun realDataDatabasePathOrNull(): Path? {
    return System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)
}
