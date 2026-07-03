plugins {
    alias(libs.plugins.pavlig43.androidapp)
    alias(libs.plugins.kotlinx.serialization)
}

import groovy.json.JsonOutput
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Генерирует Android asset с YDB/S3-настройками из локального secrets-файла.
 */
abstract class GenerateMobileSyncConfigTask : DefaultTask() {
    @get:Input
    abstract val secretsFilePath: Property<String>

    @get:OutputDirectory
    val outputDir = project.objects.directoryProperty()

    /**
     * Проверяет набор ключей и пишет `mobile_sync_config.json` в generated assets.
     */
    @TaskAction
    fun generate() {
        val secretsFile = File(secretsFilePath.get())

        if (!secretsFile.isFile) {
            throw GradleException("Mobile sync secrets file not found. Set NOCOMBRO_MOBILE_SYNC_SECRETS_FILE.")
        }

        val properties = Properties().apply {
            secretsFile.inputStream().use(::load)
        }
        val required = listOf(
            "ydb.jdbcUrl",
            "ydb.mirrorRoot",
            "s3.endpoint",
            "s3.bucket",
            "s3.region",
            "s3.accessKeyId",
            "s3.secretAccessKey",
            "s3.keyPrefix",
        )
        val missing = required.filter { key -> properties.getProperty(key).isNullOrBlank() }
        val hasYdbAuth = !properties.getProperty("ydb.saJsonBase64").isNullOrBlank() ||
            !properties.getProperty("ydb.token").isNullOrBlank()
        if (missing.isNotEmpty() || !hasYdbAuth) {
            throw GradleException("Mobile sync secrets file is missing required keys.")
        }

        val json = JsonOutput.toJson(
            linkedMapOf(
                "ydb" to linkedMapOf(
                    "jdbcUrl" to properties.getProperty("ydb.jdbcUrl"),
                    "saJsonBase64" to properties.getProperty("ydb.saJsonBase64"),
                    "token" to properties.getProperty("ydb.token"),
                    "mirrorRoot" to properties.getProperty("ydb.mirrorRoot"),
                ),
                "s3" to linkedMapOf(
                    "endpoint" to properties.getProperty("s3.endpoint"),
                    "bucket" to properties.getProperty("s3.bucket"),
                    "region" to properties.getProperty("s3.region"),
                    "accessKeyId" to properties.getProperty("s3.accessKeyId"),
                    "secretAccessKey" to properties.getProperty("s3.secretAccessKey"),
                    "keyPrefix" to properties.getProperty("s3.keyPrefix"),
                ),
            )
        )

        val assetsDir = outputDir.get().asFile
        assetsDir.mkdirs()
        assetsDir.resolve("mobile_sync_config.json").writeText(JsonOutput.prettyPrint(json))
    }
}

android {
    namespace = "ru.pavlig43.nocombro.mobile"

    defaultConfig {
        applicationId = "ru.pavlig43.nocombro.mobile"
    }

    sourceSets {
        getByName("main").assets.directories.add(
            layout.buildDirectory.dir("generated/mobileSyncConfig/assets").get().asFile.path
        )
    }

}

val generateMobileSyncConfig = tasks.register<GenerateMobileSyncConfigTask>("generateMobileSyncConfig") {
    outputDir.set(layout.buildDirectory.dir("generated/mobileSyncConfig/assets"))
    secretsFilePath.set(
        providers.gradleProperty("nocombro.mobileSyncSecretsFile")
            .orElse(providers.environmentVariable("NOCOMBRO_MOBILE_SYNC_SECRETS_FILE"))
            .orElse("${System.getenv("APPDATA") ?: ""}/Nocombro/mobile-sync-secrets.properties")
    )
    outputs.upToDateWhen { false }
}

tasks.named("preBuild") {
    dependsOn(generateMobileSyncConfig)
}

dependencies {
    implementation(projects.core)
    implementation(projects.corekoin)
    implementation(projects.datetime)
    implementation(projects.theme)
    implementation(libs.filekit.dialogs.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ydb.jdbc)
    implementation(libs.aws.kotlin.s3)
    implementation(libs.conscrypt.android)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
