package ru.pavlig43.database.data.sync.mirror

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Runtime-конфигурация JDBC-доступа к typed mirror tables в YDB.
 *
 * Авторизация выбирается в gateway: service account file имеет приоритет над
 * token. [tableRoot] может быть пустым и позволяет размещать mirror tables в
 * отдельном YDB-каталоге без изменения их канонических имен.
 */
data class YdbMirrorJdbcConfig(
    val jdbcUrl: String,
    val authToken: String?,
    val serviceAccountFile: String?,
    val tableRoot: String,
) {
    /**
     * Строит безопасный относительный путь таблицы внутри YDB database.
     *
     * Крайние `/` удаляются, чтобы конфигурации `mirror`, `/mirror/` и
     * ` mirror ` давали одинаковый результат.
     */
    fun tablePath(table: MirrorSyncTable): String {
        val normalizedRoot = tableRoot.trim().trim('/')
        return if (normalizedRoot.isEmpty()) {
            table.tableName
        } else {
            "$normalizedRoot/${table.tableName}"
        }
    }

    companion object {
        /**
         * Читает конфигурацию из environment variables или JVM properties.
         *
         * Environment имеет приоритет. Если JDBC URL отсутствует, используется
         * default database URL. На Windows service account автоматически ищется в
         * `%APPDATA%/Nocombro/ydb-sa-key.json`.
         */
        fun fromEnvironment(): YdbMirrorJdbcConfig {
            return fromSettings(::readSetting, ::defaultWindowsServiceAccountFile)
        }

        internal fun fromSettings(
            readSetting: (envName: String, propertyName: String) -> String?,
            defaultServiceAccountFile: () -> String?,
        ): YdbMirrorJdbcConfig {
            val jdbcUrl = readSetting("NOCOMBRO_YDB_JDBC_URL", "nocombro.ydb.jdbcUrl")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: DEFAULT_JDBC_URL
            val tableRoot = (
                readSetting("NOCOMBRO_YDB_MIRROR_ROOT", "nocombro.ydb.mirrorRoot")
                )
                ?.trim()
                .orEmpty()
            return YdbMirrorJdbcConfig(
                jdbcUrl = jdbcUrl,
                authToken = readSetting("NOCOMBRO_YDB_TOKEN", "nocombro.ydb.token")?.trim(),
                serviceAccountFile = readSetting(
                    "NOCOMBRO_YDB_SA_FILE",
                    "nocombro.ydb.saFile",
                )?.trim()?.takeIf(String::isNotBlank) ?: defaultServiceAccountFile(),
                tableRoot = tableRoot,
            )
        }

        private fun readSetting(envName: String, propertyName: String): String? {
            return System.getenv(envName) ?: System.getProperty(propertyName)
        }

        internal const val DEFAULT_JDBC_URL =
            "jdbc:ydb:grpcs://ydb.serverless.yandexcloud.net:2135/" +
                "?database=/ru-central1/b1g87p6oufggn8merjua/etn8eb6ujifrk8lp7b73"

        @Suppress("UnreachableCode")
        private fun defaultWindowsServiceAccountFile(): String? {
            val appData = System.getenv("APPDATA")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return null
            return Paths.get(appData, "Nocombro", "ydb-sa-key.json")
                .takeIf(Files::exists)
                ?.toAbsolutePath()
                ?.normalize()
                ?.toString()
        }
    }
}
