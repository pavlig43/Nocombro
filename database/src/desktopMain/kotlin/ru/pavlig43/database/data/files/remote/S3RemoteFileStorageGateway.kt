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

/**
 * Реальная реализация [RemoteFileStorageGateway] через S3-compatible API.
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

    override suspend fun upload(
        objectKey: String,
        localPath: String,
    ): Result<RemoteFileRef> {
        return runCatching {
            val path = Path.of(localPath)
            val contentType = Files.probeContentType(path) ?: "application/octet-stream"
            val finalObjectKey = buildObjectKey(objectKey)
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
                objectKey = finalObjectKey,
            )
        }
    }

    override suspend fun download(
        objectKey: String,
        localPath: String,
    ): Result<Unit> {
        return runCatching {
            val path = Path.of(localPath)
            path.parent?.let(Files::createDirectories)
            withClient { client ->
                client.getObject(
                    GetObjectRequest.builder()
                        .bucket(config.bucket)
                        .key(buildObjectKey(objectKey))
                        .build(),
                    ResponseTransformer.toFile(path)
                )
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
                                    RemoteStorageObject(
                                        objectKey = key,
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

    override suspend fun delete(
        objectKey: String,
    ): Result<Unit> {
        return runCatching {
            withClient { client ->
                client.deleteObject(
                    DeleteObjectRequest.builder()
                        .bucket(config.bucket)
                        .key(buildObjectKey(objectKey))
                        .build()
                )
            }
        }
    }

    private fun buildObjectKey(
        objectKey: String,
    ): String {
        val cleanObjectKey = objectKey.trimStart('/')
        val cleanPrefix = config.keyPrefix.trim('/')

        return if (cleanPrefix.isBlank()) {
            cleanObjectKey
        } else if (cleanObjectKey == cleanPrefix || cleanObjectKey.startsWith("$cleanPrefix/")) {
            cleanObjectKey
        } else {
            "$cleanPrefix/$cleanObjectKey"
        }
    }

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
