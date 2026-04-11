package ru.pavlig43.database.data.files.remote

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

/**
 * Runtime-конфиг для S3-compatible object storage.
 *
 * Основной сценарий для desktop-приложения: читать конфиг из `%APPDATA%\Nocombro\s3.properties`,
 * чтобы хранить его рядом с остальными локальными runtime-файлами приложения.
 *
 * Дополнительно остаётся fallback на env и JVM properties, чтобы не ломать ручные dev-сценарии.
 * По умолчанию конфиг подобран под `Yandex Object Storage`, но сама модель остаётся
 * совместимой с любым S3-провайдером.
 */
data class S3RemoteFileStorageConfig(
    val bucket: String,
    val region: String,
    val endpoint: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val keyPrefix: String,
) {
    companion object {
        /**
         * Собирает конфиг из `%APPDATA%\Nocombro\s3.properties`,
         * а если файла нет - из env/JVM properties.
         *
         * Возвращает `null`, если обязательный минимум не задан. Это сигнал для DI,
         * что нужно использовать `NoopRemoteFileStorageGateway`.
         */
        fun fromEnvironment(): S3RemoteFileStorageConfig? {
            val fileProperties = loadWindowsAppDataProperties()

            val bucket = readSetting(
                fileProperties = fileProperties,
                fileKey = "bucket",
                envName = "NOCOMBRO_REMOTE_FILES_BUCKET",
                propertyName = "nocombro.remoteFiles.bucket",
            )?.trim()?.takeIf(String::isNotBlank) ?: return null

            val accessKeyId = readSetting(
                fileProperties = fileProperties,
                fileKey = "accessKey",
                envName = "NOCOMBRO_REMOTE_FILES_ACCESS_KEY",
                propertyName = "nocombro.remoteFiles.accessKey",
            )?.trim()?.takeIf(String::isNotBlank) ?: return null

            val secretAccessKey = readSetting(
                fileProperties = fileProperties,
                fileKey = "secretKey",
                envName = "NOCOMBRO_REMOTE_FILES_SECRET_KEY",
                propertyName = "nocombro.remoteFiles.secretKey",
            )?.trim()?.takeIf(String::isNotBlank) ?: return null

            return S3RemoteFileStorageConfig(
                bucket = bucket,
                region = readSetting(
                    fileProperties = fileProperties,
                    fileKey = "region",
                    envName = "NOCOMBRO_REMOTE_FILES_REGION",
                    propertyName = "nocombro.remoteFiles.region",
                )?.trim()?.takeIf(String::isNotBlank) ?: "ru-central1",
                endpoint = readSetting(
                    fileProperties = fileProperties,
                    fileKey = "endpoint",
                    envName = "NOCOMBRO_REMOTE_FILES_ENDPOINT",
                    propertyName = "nocombro.remoteFiles.endpoint",
                )?.trim()?.takeIf(String::isNotBlank) ?: "https://storage.yandexcloud.net",
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                keyPrefix = readSetting(
                    fileProperties = fileProperties,
                    fileKey = "prefix",
                    envName = "NOCOMBRO_REMOTE_FILES_PREFIX",
                    propertyName = "nocombro.remoteFiles.prefix",
                )?.trim()?.trim('/') ?: "nocombro",
            )
        }

        /**
         * Читает properties-файл из `%APPDATA%\Nocombro`, если он есть.
         */
        private fun loadWindowsAppDataProperties(): Properties? {
            val appData = System.getenv("APPDATA")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return null

            val candidate = Paths.get(appData, "Nocombro", "s3.properties")
            if (!Files.exists(candidate)) {
                return null
            }

            return Properties().apply {
                Files.newInputStream(candidate).use(::load)
            }
        }

        /**
         * Унифицированное чтение сначала из `s3.properties`, затем из env, затем из JVM properties.
         */
        private fun readSetting(
            fileProperties: Properties?,
            fileKey: String,
            envName: String,
            propertyName: String,
        ): String? {
            return fileProperties?.getProperty(fileKey)
                ?: System.getenv(envName)
                ?: System.getProperty(propertyName)
        }
    }
}
