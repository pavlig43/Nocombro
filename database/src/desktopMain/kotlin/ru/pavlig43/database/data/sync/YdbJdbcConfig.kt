package ru.pavlig43.database.data.sync

data class YdbJdbcConfig(
    val jdbcUrl: String,
    val authToken: String?,
    val tablePath: String,
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

            return YdbJdbcConfig(
                jdbcUrl = jdbcUrl,
                authToken = readSetting("NOCOMBRO_YDB_TOKEN", "nocombro.ydb.token")?.trim(),
                tablePath = tablePath,
            )
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
