package ru.pavlig43.database.data.files.remote

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.nio.file.Files

/** Проверяет нормализацию S3-префикса и отказ для опасных ключей файлов. */
class S3RemoteFileStorageGatewayTest : FunSpec({

    val gateway = S3RemoteFileStorageGateway(
        S3RemoteFileStorageConfig(
            bucket = "bucket",
            region = "region",
            endpoint = "https://example.invalid",
            accessKeyId = "access-key",
            secretAccessKey = "secret-key",
            keyPrefix = "nocombro",
        )
    )

    test("converts physical S3 key to logical database key") {
        gateway.toLogicalObjectKey("nocombro/files/product/file.pdf") shouldBe
            "files/product/file.pdf"
    }

    test("does not strip a similar key outside the configured prefix") {
        gateway.toLogicalObjectKey("nocombro-backup/files/product/file.pdf") shouldBe
            "nocombro-backup/files/product/file.pdf"
    }

    test("converts the prefix marker to an empty logical key") {
        gateway.toLogicalObjectKey("nocombro") shouldBe ""
    }

    test("upload result stores a logical object key") {
        gateway.normalizeObjectKey("nocombro/files/product/file.pdf") shouldBe
            "files/product/file.pdf"
    }

    test("ordinary backslashes are normalized") {
        gateway.normalizeObjectKey("files\\product\\file.pdf") shouldBe
            "files/product/file.pdf"
    }

    test("rejects unsafe logical keys") {
        listOf(
            "",
            "../secret",
            "..\\secret",
            "/absolute",
            "C:/absolute",
            "C:relative.txt",
            "files/./secret",
        )
            .forEach { key ->
                shouldThrow<IllegalArgumentException> { gateway.normalizeObjectKey(key) }
            }
    }

    test("invalid download key creates no local directory") {
        val root = Files.createTempDirectory("invalid-s3-key")
        val target = root.resolve("not-created/report.pdf")

        gateway.download("C:relative.txt", target.toString()).isFailure shouldBe true

        Files.exists(target.parent) shouldBe false
    }
})
