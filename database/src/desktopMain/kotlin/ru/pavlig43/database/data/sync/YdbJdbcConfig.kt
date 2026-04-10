package ru.pavlig43.database.data.sync

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class YdbJdbcConfig(
    val jdbcUrl: String,
    val authToken: String?,
    val serviceAccountFile: String?,
    val tablePath: String,
    val reminderSourceTablePath: String,
) {
    companion object {
        fun fromEnvironment(): YdbJdbcConfig? {
            val jdbcUrl = readSetting("NOCOMBRO_YDB_JDBC_URL", "nocombro.ydb.jdbcUrl")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return null

            val tablePath = readSetting("NOCOMBRO_YDB_SYNC_TABLE", "nocombro.ydb.syncTable")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: "sync_push_log"

            val reminderSourceTablePath = readSetting(
                "NOCOMBRO_YDB_REMINDER_SOURCE_TABLE",
                "nocombro.ydb.reminderSourceTable"
            )?.trim()
                ?.takeIf(String::isNotBlank)
                ?: "reminder_email_source"

            val serviceAccountFile = readSetting(
                "NOCOMBRO_YDB_SA_FILE",
                "nocombro.ydb.saFile"
            )?.trim()
                ?.takeIf(String::isNotBlank)
                ?: defaultWindowsServiceAccountFile()

            return YdbJdbcConfig(
                jdbcUrl = jdbcUrl,
                authToken = readSetting("NOCOMBRO_YDB_TOKEN", "nocombro.ydb.token")?.trim(),
                serviceAccountFile = serviceAccountFile,
                tablePath = tablePath,
                reminderSourceTablePath = reminderSourceTablePath,
            )
        }

        private fun defaultWindowsServiceAccountFile(): String? {
            val appData = System.getenv("APPDATA")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return null

            val candidate = Paths.get(appData, "Nocombro", "ydb-sa-key.json")
            return candidate
                .takeIf(Files::exists)
                ?.toJdbcFileReference()
        }

        private fun readSetting(
            envName: String,
            propertyName: String,
        ): String? {
            return System.getenv(envName)
                ?: System.getProperty(propertyName)
        }
    }
}

private fun Path.toJdbcFileReference(): String {
    return toAbsolutePath().normalize().toString()
}
