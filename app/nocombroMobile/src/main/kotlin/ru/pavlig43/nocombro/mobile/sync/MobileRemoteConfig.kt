package ru.pavlig43.nocombro.mobile.sync

import android.content.Context
import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Remote config Android sync, который Gradle кладёт в assets.
 */
@Serializable
data class MobileRemoteConfig(
    val ydb: MobileYdbConfig,
    val s3: MobileS3Config,
)

/**
 * Настройки YDB mirror для Android-клиента.
 */
@Serializable
data class MobileYdbConfig(
    val jdbcUrl: String,
    val saJsonBase64: String? = null,
    val token: String? = null,
    @SerialName("mirrorRoot")
    val tableRoot: String = "",
)

/**
 * Настройки S3-compatible хранилища для файлов экспериментов.
 */
@Serializable
data class MobileS3Config(
    val endpoint: String,
    val bucket: String,
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val keyPrefix: String = "",
) {
    /**
     * Добавляет configured prefix к object key без лишних `/`.
     */
    fun remoteKey(localObjectKey: String): String {
        val prefix = keyPrefix.trim().trim('/')
        val key = localObjectKey.trim().trim('/')
        return if (prefix.isEmpty()) key else "$prefix/$key"
    }
}

/**
 * Загружает sync config из Android assets и декодирует секреты для runtime.
 */
class MobileRemoteConfigRepository(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /**
     * Читает `mobile_sync_config.json` из assets.
     */
    fun load(): Result<MobileRemoteConfig> = runCatching {
        context.assets.open(CONFIG_ASSET_NAME).use { input ->
            json.decodeFromString<MobileRemoteConfig>(
                input.bufferedReader().use { it.readText() },
            )
        }
    }

    /**
     * Декодирует Base64 service account JSON для IAM token supplier.
     */
    fun decodeServiceAccountJson(config: MobileYdbConfig): String? {
        val encoded = config.saJsonBase64?.takeIf(String::isNotBlank) ?: return null
        return Base64.getDecoder().decode(encoded).toString(Charsets.UTF_8)
    }

    private companion object {
        const val CONFIG_ASSET_NAME = "mobile_sync_config.json"
    }
}
