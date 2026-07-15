package ru.pavlig43.database.data.files.remote

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.core.sync.ResponseTransformer
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import ru.pavlig43.database.data.files.normalizeLogicalFileKey

/**
 * Реальная реализация [RemoteFileStorageGateway] через S3-совместимый API.
 *
 * В текущем проекте она используется как базовый способ складывать бинарные вложения
 * в `Yandex Object Storage`, но остаётся достаточно общей для других S3-провайдеров.
 *
 * Локальная БД хранит только метаданные файла и `objectKey`, а сам бинарный контент
 * живёт в bucket.
 */
class S3RemoteFileStorageGateway(
    private val config: S3RemoteFileStorageConfig,
) : RemoteFileStorageGateway {

    override val providerId: String = "s3"

    override fun isConfigured(): Boolean = true

    /**
     * Загружает локальный файл по проверенному логическому ключу.
     *
     * Префикс бакета добавляется ровно один раз, а наружу возвращается ключ без
     * префикса, пригодный для хранения в Room и YDB.
     */
    override suspend fun upload(
        objectKey: String,
        localPath: String,
    ): Result<RemoteFileRef> {
        return runCatching {
            val finalObjectKey = buildObjectKey(objectKey)
            val path = Path.of(localPath)
            val contentType = Files.probeContentType(path) ?: "application/octet-stream"
            withClient { client ->
                client.putObject(
                    PutObjectRequest.builder()
                        .bucket(config.bucket)
                        .key(finalObjectKey)
                        .contentType(contentType)
                        .build(),
                    RequestBody.fromFile(path)
                )
            }
            RemoteFileRef(
                providerId = providerId,
                objectKey = toLogicalObjectKey(finalObjectKey),
            )
        }
    }

    /**
     * Скачивает объект во временный файл и атомарно заменяет локальную цель.
     *
     * Ключ проверяется до создания каталога. При сетевом или файловом сбое `.part-*`
     * удаляется, а уже существующая целевая копия остаётся целой.
     */
    override suspend fun download(
        objectKey: String,
        localPath: String,
    ): Result<Unit> {
        return runCatching {
            val remoteObjectKey = buildObjectKey(objectKey)
            val path = Path.of(localPath)
            path.parent?.let(Files::createDirectories)
            val partPath = path.resolveSibling("${path.fileName}.part-${UUID.randomUUID()}")
            try {
                withClient { client ->
                    client.getObject(
                        GetObjectRequest.builder()
                            .bucket(config.bucket)
                            .key(remoteObjectKey)
                            .build(),
                        ResponseTransformer.toFile(partPath)
                    )
                }
                Files.move(partPath, path, ATOMIC_MOVE, REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(partPath)
            }
        }
    }

    override suspend fun listObjects(): Result<List<RemoteStorageObject>> {
        return runCatching {
            withClient { client ->
                buildList {
                    var continuationToken: String? = null
                    do {
                        val response = client.listObjectsV2(
                            ListObjectsV2Request.builder()
                                .bucket(config.bucket)
                                .prefix(config.keyPrefix.takeIf { it.isNotBlank() })
                                .continuationToken(continuationToken)
                                .build()
                        )
                        response.contents()
                            .mapNotNull { item ->
                                item.key()?.let { key ->
                                    val logicalObjectKey = toLogicalObjectKey(key)
                                        .takeIf(String::isNotBlank)
                                        ?: return@let null
                                    RemoteStorageObject(
                                        objectKey = logicalObjectKey,
                                        sizeBytes = item.size(),
                                    )
                                }
                            }
                            .forEach(::add)
                        continuationToken = response.nextContinuationToken()
                    } while (response.isTruncated)
                }
            }
        }
    }

    /** Удаляет объект после проверки ключа и однократного добавления префикса. */
    override suspend fun delete(
        objectKey: String,
    ): Result<Unit> {
        return runCatching {
            val remoteObjectKey = buildObjectKey(objectKey)
            withClient { client ->
                client.deleteObject(
                    DeleteObjectRequest.builder()
                        .bucket(config.bucket)
                        .key(remoteObjectKey)
                        .build()
                )
            }
        }
    }

    /**
     * Преобразует логический ключ в физический ключ бакета.
     *
     * Метод принимает как чистый ключ, так и старую запись с уже добавленным
     * префиксом; в результате префикс присутствует ровно один раз.
     */
    private fun buildObjectKey(
        objectKey: String,
    ): String {
        val cleanObjectKey = normalizeObjectKey(objectKey)
        require(cleanObjectKey.isNotBlank()) { "File key is empty" }
        val cleanPrefix = config.keyPrefix.trim('/').takeIf(String::isNotBlank)
            ?.let(::normalizeLogicalFileKey)
            .orEmpty()

        return if (cleanPrefix.isBlank()) {
            cleanObjectKey
        } else if (cleanObjectKey == cleanPrefix || cleanObjectKey.startsWith("$cleanPrefix/")) {
            cleanObjectKey
        } else {
            "$cleanPrefix/$cleanObjectKey"
        }
    }

    /**
     * Снимает настроенный префикс с физического S3-ключа.
     *
     * @return проверенный логический ключ или пустую строку для самого префикса.
     */
    internal fun toLogicalObjectKey(
        objectKey: String,
    ): String {
        val cleanObjectKey = normalizeLogicalFileKey(objectKey)
        val cleanPrefix = config.keyPrefix.trim('/').takeIf(String::isNotBlank)
            ?.let(::normalizeLogicalFileKey)
            .orEmpty()

        return when {
            cleanPrefix.isBlank() -> cleanObjectKey
            cleanObjectKey == cleanPrefix -> ""
            cleanObjectKey.startsWith("$cleanPrefix/") ->
                normalizeLogicalFileKey(cleanObjectKey.removePrefix("$cleanPrefix/"))
            else -> normalizeLogicalFileKey(cleanObjectKey)
        }
    }

    override fun normalizeObjectKey(objectKey: String): String =
        toLogicalObjectKey(objectKey)

    private fun <T> withClient(
        block: (S3Client) -> T,
    ): T {
        val credentials = AwsBasicCredentials.create(
            config.accessKeyId,
            config.secretAccessKey,
        )
        val client = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(config.region))
            .endpointOverride(URI.create(config.endpoint))
            .forcePathStyle(true)
            .build()

        return client.use(block)
    }
}
