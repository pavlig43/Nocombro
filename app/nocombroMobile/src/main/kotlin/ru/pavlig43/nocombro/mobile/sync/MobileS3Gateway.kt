package ru.pavlig43.nocombro.mobile.sync

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.writeToFile
import aws.smithy.kotlin.runtime.net.url.Url
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Минимальный шлюз к объектному хранилищу для файлов Android-синхронизации.
 *
 * Входные ключи здесь логические: без S3-префикса. Реальная реализация сама
 * добавляет префикс перед сетевым запросом.
 */
interface MobileObjectStorageGateway {
    /** Загружает локальный файл по логическому ключу объекта. */
    suspend fun uploadFile(localPath: String, remoteKey: String): Result<Unit>

    /** Скачивает объект по логическому ключу объекта в локальный путь. */
    suspend fun downloadFile(remoteKey: String, localPath: String): Result<Unit>
}

/**
 * S3-совместимый шлюз для Android.
 *
 * Для скачивания используется AWS Kotlin SDK. Для загрузки оставлен ручной PUT
 * с AWS Signature V4: на Android этот путь стабильнее для Yandex Object Storage
 * и не требует настраивать тело запроса в SDK.
 */
class AwsKotlinMobileS3Gateway(
    private val config: MobileS3Config,
) : MobileObjectStorageGateway {
    override suspend fun uploadFile(localPath: String, remoteKey: String): Result<Unit> = runCatching {
        val file = File(localPath)
        require(file.isFile) { "Локальный файл не найден" }
        putObject(file, remoteKey)
    }

    override suspend fun downloadFile(remoteKey: String, localPath: String): Result<Unit> = runCatching {
        val target = File(localPath)
        target.parentFile?.mkdirs()
        client().use { s3 ->
            s3.getObject(
                GetObjectRequest {
                    bucket = config.bucket
                    key = config.remoteKey(remoteKey)
                }
            ) { response ->
                response.body?.writeToFile(target)
            }
        }
    }

    private suspend fun client(): S3Client {
        return S3Client.fromEnvironment {
            region = config.region
            endpointUrl = Url.parse(config.endpoint)
            forcePathStyle = true
            credentialsProvider = StaticCredentialsProvider(
                Credentials(
                    accessKeyId = config.accessKeyId,
                    secretAccessKey = config.secretAccessKey,
                )
            )
        }
    }

    private fun putObject(file: File, remoteKey: String) {
        val target = objectUrl(config.remoteKey(remoteKey))
        val payloadHash = file.sha256Hex()
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val amzDate = AMZ_DATE_FORMAT.format(now)
        val shortDate = SHORT_DATE_FORMAT.format(now)
        val credentialScope = "$shortDate/${config.region}/s3/aws4_request"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
        val canonicalHeaders = buildString {
            append("host:${target.hostHeader}\n")
            append("x-amz-content-sha256:$payloadHash\n")
            append("x-amz-date:$amzDate\n")
        }
        val canonicalRequest = listOf(
            "PUT",
            target.canonicalPath,
            "",
            canonicalHeaders,
            signedHeaders,
            payloadHash,
        ).joinToString("\n")
        val stringToSign = listOf(
            "AWS4-HMAC-SHA256",
            amzDate,
            credentialScope,
            canonicalRequest.sha256Hex(),
        ).joinToString("\n")
        val signature = signingKey(shortDate).hmacSha256(stringToSign).toHex()
        val authorization = buildString {
            append("AWS4-HMAC-SHA256 ")
            append("Credential=${config.accessKeyId}/$credentialScope, ")
            append("SignedHeaders=$signedHeaders, ")
            append("Signature=$signature")
        }
        val connection = (target.url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setFixedLengthStreamingMode(file.length())
            setRequestProperty("Content-Type", "application/octet-stream")
            setRequestProperty("Host", target.hostHeader)
            setRequestProperty("x-amz-content-sha256", payloadHash)
            setRequestProperty("x-amz-date", amzDate)
            setRequestProperty("Authorization", authorization)
        }
        try {
            file.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            val code = connection.responseCode
            if (code !in HTTP_OK_RANGE) {
                val message = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: connection.responseMessage
                    ?: "S3 upload failed"
                error("S3 upload failed: HTTP $code $message")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun objectUrl(remoteKey: String): S3ObjectUrl {
        val endpoint = URL(config.endpoint.trimEnd('/'))
        val basePath = endpoint.path.trim('/').takeIf(String::isNotBlank)
        val keySegments = remoteKey.trim('/').split('/').filter(String::isNotBlank)
        val pathSegments = buildList {
            basePath?.split('/')?.filter(String::isNotBlank)?.let(::addAll)
            add(config.bucket)
            addAll(keySegments)
        }
        val canonicalPath = pathSegments.joinToString(
            separator = "/",
            prefix = "/",
            transform = ::s3Encode,
        )
        val portSuffix = endpoint.port.takeIf { it != -1 && it != endpoint.defaultPort }
            ?.let { ":$it" }
            .orEmpty()
        val hostHeader = endpoint.host.lowercase(Locale.US) + portSuffix
        return S3ObjectUrl(
            url = URL(endpoint.protocol, endpoint.host, endpoint.port, canonicalPath),
            canonicalPath = canonicalPath,
            hostHeader = hostHeader,
        )
    }

    private fun signingKey(shortDate: String): ByteArray {
        val dateKey = "AWS4${config.secretAccessKey}".toByteArray(Charsets.UTF_8).hmacSha256(shortDate)
        val regionKey = dateKey.hmacSha256(config.region)
        val serviceKey = regionKey.hmacSha256("s3")
        return serviceKey.hmacSha256("aws4_request")
    }

    private data class S3ObjectUrl(
        val url: URL,
        val canonicalPath: String,
        val hostHeader: String,
    )

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 120_000
        val HTTP_OK_RANGE = 200..299
        val AMZ_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        val SHORT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

private fun s3Encode(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")
}

private fun File.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHex()
}

private fun String.sha256Hex(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .toHex()
}

private fun ByteArray.hmacSha256(value: String): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(this, "HmacSHA256"))
    return mac.doFinal(value.toByteArray(Charsets.UTF_8))
}

private fun ByteArray.toHex(): String {
    return joinToString(separator = "") { byte -> "%02x".format(byte) }
}
