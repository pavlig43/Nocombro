package ru.pavlig43.nocombro.mobile.sync

import android.content.Context
import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Настройки удалённой синхронизации Android-клиента.
 *
 * Gradle собирает их из локального файла секретов и кладёт в assets APK.
 * В рантайме код читает этот объект и на его основе открывает доступ к YDB
 * и S3. Секреты не логируются и не попадают в тексты ошибок.
 */
@Serializable
data class MobileRemoteConfig(
    val ydb: MobileYdbConfig,
    val s3: MobileS3Config,
)

/**
 * Настройки доступа к YDB mirror-таблицам.
 *
 * [jdbcUrl] указывает на базу YDB. [tableRoot] задаёт папку, где лежат
 * mirror-таблицы. Для входа нужен либо готовый IAM [token], либо ключ
 * сервисного аккаунта в [saJsonBase64], из которого Android сам получает
 * IAM-токен.
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
 * Настройки S3-совместимого хранилища для файлов экспериментов.
 *
 * В Room и YDB хранится логический ключ файла без [keyPrefix]. Префикс нужен
 * только при физическом обращении к бакету. Это не даёт телефону повторно
 * добавлять префикс после получения данных с другого устройства.
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
     * Строит физический ключ объекта в bucket.
     *
     * Метод принимает и логический ключ, и ключ с префиксом. На выходе префикс
     * будет ровно один раз.
     */
    fun remoteKey(localObjectKey: String): String {
        val prefix = normalizedPrefix()
        val key = normalizeObjectKey(localObjectKey)
        return when {
            prefix.isEmpty() -> key
            key.isEmpty() -> prefix
            else -> "$prefix/$key"
        }
    }

    /**
     * Возвращает логический ключ без [keyPrefix].
     *
     * Этот вид хранится в Room и YDB. Если старые данные уже содержат префикс,
     * он снимается при чтении.
     */
    fun normalizeObjectKey(objectKey: String): String {
        val key = objectKey.trim().trim('/')
        val prefix = normalizedPrefix()
        return when {
            prefix.isEmpty() -> key
            key == prefix -> ""
            key.startsWith("$prefix/") -> key.removePrefix("$prefix/")
            else -> key
        }
    }

    private fun normalizedPrefix(): String = keyPrefix.trim().trim('/')
}

/**
 * Загружает настройки синхронизации из Android assets и декодирует секреты
 * для времени работы приложения.
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
     * Декодирует Base64 service account JSON для поставщика IAM-токена.
     */
    fun decodeServiceAccountJson(config: MobileYdbConfig): String? {
        val encoded = config.saJsonBase64?.takeIf(String::isNotBlank) ?: return null
        return Base64.getDecoder().decode(encoded).toString(Charsets.UTF_8)
    }

    private companion object {
        const val CONFIG_ASSET_NAME = "mobile_sync_config.json"
    }
}
