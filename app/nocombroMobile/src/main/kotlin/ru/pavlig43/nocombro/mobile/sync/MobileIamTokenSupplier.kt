package ru.pavlig43.nocombro.mobile.sync

import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.PSSParameterSpec
import java.time.Instant
import java.util.Base64
import java.util.function.Supplier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader

/**
 * Создаёт и кеширует IAM-токен Yandex Cloud для YDB JDBC `tokenProvider`.
 *
 * На desktop YDB SDK умеет читать ключ сервисного аккаунта сам. На Android этот
 * путь недоступен, поэтому здесь сделан небольшой поставщик токена:
 * - читает JSON-ключ сервисного аккаунта;
 * - собирает JWT с `kid`, `iss`, `aud`, `iat` и `exp`;
 * - подписывает JWT приватным RSA-ключом через PS256;
 * - меняет JWT на IAM-токен в Yandex IAM API;
 * - кеширует IAM-токен до истечения срока.
 */
internal class MobileIamTokenSupplier(
    serviceAccountJson: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : Supplier<String> {
    private val serviceAccountKey = json.decodeFromString<MobileServiceAccountKey>(serviceAccountJson)
    private val privateKey = serviceAccountKey.privateKey.toRsaPrivateKey()
    private val lock = Any()

    @Volatile
    private var cachedToken: CachedIamToken? = null

    /**
     * Возвращает свежий IAM-токен или запрашивает новый.
     *
     * YDB JDBC может вызвать `tokenProvider` из разных потоков. `lock` не даёт
     * сделать несколько одинаковых запросов в IAM API, когда токен истёк.
     */
    override fun get(): String {
        cachedToken?.takeIf(CachedIamToken::isFresh)?.let { return it.value }
        return synchronized(lock) {
            cachedToken?.takeIf(CachedIamToken::isFresh)?.value ?: requestIamToken().also {
                cachedToken = it
            }.value
        }
    }

    /**
     * Меняет подписанный JWT сервисного аккаунта на IAM-токен.
     *
     * IAM API возвращает сам токен и время истечения. Оба значения нужны: токен
     * отдаётся JDBC-драйверу, а срок нужен для раннего обновления до ошибки YDB.
     */
    private fun requestIamToken(): CachedIamToken {
        val jwt = createJwt()
        val connection = (URL(IAM_TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = REQUEST_TIMEOUT_MILLIS
            readTimeout = REQUEST_TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val request = json.encodeToString(JsonObject(mapOf("jwt" to JsonPrimitive(jwt))))
        connection.outputStream.use { output ->
            output.write(request.toByteArray(Charsets.UTF_8))
        }
        val response = if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IOException("IAM token request failed: HTTP ${connection.responseCode} $error")
        }
        val root = json.parseToJsonElement(response).jsonObject
        return CachedIamToken(
            value = root.getValue("iamToken").jsonPrimitive.content,
            expiresAt = Instant.parse(root.getValue("expiresAt").jsonPrimitive.content),
        )
    }

    /**
     * Собирает JWT с PS256-подписью для Yandex IAM.
     *
     * `aud` должен совпадать с URL IAM API. `kid` берётся из ключа сервисного
     * аккаунта, чтобы Yandex Cloud понял, каким публичным ключом проверять подпись.
     */
    private fun createJwt(): String {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(JWT_TTL_SECONDS)
        val header = json.encodeToString(
            JsonObject(
                mapOf(
                    "alg" to JsonPrimitive("PS256"),
                    "typ" to JsonPrimitive("JWT"),
                    "kid" to JsonPrimitive(serviceAccountKey.id),
                )
            )
        )
        val payload = json.encodeToString(
            JsonObject(
                mapOf(
                    "iss" to JsonPrimitive(serviceAccountKey.serviceAccountId),
                    "aud" to JsonPrimitive(IAM_TOKEN_URL),
                    "iat" to JsonPrimitive(now.epochSecond),
                    "exp" to JsonPrimitive(expiresAt.epochSecond),
                )
            )
        )
        val unsignedJwt = "${header.base64Url()}.${payload.base64Url()}"
        val signature = newPssSignature().run {
            initSign(privateKey)
            update(unsignedJwt.toByteArray(Charsets.UTF_8))
            sign()
        }
        return "$unsignedJwt.${signature.base64Url()}"
    }

    /**
     * Создаёт RSASSA-PSS подпись с параметрами PS256.
     *
     * BouncyCastle ставится в `NocombroMobileApplication`. Без явного провайдера
     * Android может не найти нужный алгоритм или взять несовместимую реализацию.
     */
    private fun newPssSignature(): Signature {
        return Signature.getInstance("RSASSA-PSS", BouncyCastleProvider.PROVIDER_NAME).apply {
            setParameter(PSS_SPEC)
        }
    }

    /**
     * IAM-токен вместе со временем истечения.
     */
    private data class CachedIamToken(
        val value: String,
        val expiresAt: Instant,
    ) {
        /**
         * Проверяет, что токен не истечёт в ближайшие несколько минут.
         */
        fun isFresh(): Boolean = Instant.now().plusSeconds(TOKEN_REFRESH_SKEW_SECONDS).isBefore(expiresAt)
    }

    private companion object {
        const val IAM_TOKEN_URL = "https://iam.api.cloud.yandex.net/iam/v1/tokens"
        const val JWT_TTL_SECONDS = 3600L
        const val TOKEN_REFRESH_SKEW_SECONDS = 300L
        const val REQUEST_TIMEOUT_MILLIS = 20_000
        val PSS_SPEC = PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1)
    }
}

/**
 * Минимум полей ключа сервисного аккаунта, нужных для подписи JWT.
 */
@Serializable
private data class MobileServiceAccountKey(
    val id: String,
    @SerialName("service_account_id")
    val serviceAccountId: String,
    @SerialName("private_key")
    val privateKey: String,
)

/**
 * Преобразует PEM-ключ сервисного аккаунта в RSA private key.
 */
private fun String.toRsaPrivateKey(): PrivateKey {
    val pem = PemReader(StringReader(this)).use { reader ->
        reader.readPemObject()
    }
    val keySpec = PKCS8EncodedKeySpec(pem.content)
    return KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME).generatePrivate(keySpec)
}

/**
 * Кодирует строку в Base64 URL без padding для JWT header/payload.
 */
private fun String.base64Url(): String = toByteArray(Charsets.UTF_8).base64Url()

/**
 * Кодирует байты в Base64 URL без padding для JWT signature.
 */
private fun ByteArray.base64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)
