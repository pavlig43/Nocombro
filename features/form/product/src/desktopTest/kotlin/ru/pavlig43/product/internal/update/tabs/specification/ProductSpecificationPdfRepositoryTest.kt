package ru.pavlig43.product.internal.update.tabs.specification

import io.kotest.matchers.shouldBe
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.PRODUCT_SPECIFICATION_FILE_NAME
import ru.pavlig43.database.data.files.remote.RemoteFileRef
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.files.remote.RemoteStorageObject
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import java.nio.file.Files

class ProductSpecificationPdfRepositoryTest : DesktopMainDispatcherFunSpec({

    test("generates PDF and Room row without calling S3") {
        withEmptyTestDatabase { db ->
            val outputDirectory = Files.createTempDirectory("nocombro-product-pdf").toFile()
            val outputFile = outputDirectory.resolve(PRODUCT_SPECIFICATION_FILE_NAME)
            val gateway = FailingUploadGateway()
            try {
                val repository = ProductSpecificationPdfRepository(
                    fileDao = db.fileDao,
                    remoteFileStorageGateway = gateway,
                    pdfGenerator = ProductSpecificationPdfGenerator(),
                    managedLocalFilePath = { outputFile.absolutePath },
                )

                val result = repository.generateAndSave(
                    productName = "Тестовый продукт",
                    specification = ProductSpecification(
                        productId = 77,
                        dosage = "1 капсула",
                        composition = "Тестовый состав",
                    ),
                )

                result.isSuccess shouldBe true
                outputFile.isFile shouldBe true
                assertPdfHasNoImages(outputFile)
                gateway.uploadCalls shouldBe 0
                val saved = db.fileDao.getFileByOwnerAndDisplayName(
                    ownerId = 77,
                    ownerFileType = OwnerType.PRODUCT,
                    displayName = PRODUCT_SPECIFICATION_FILE_NAME,
                )
                saved?.path shouldBe outputFile.absolutePath
                saved?.remoteObjectKey?.isNotBlank() shouldBe true
                saved?.remoteStorageProvider shouldBe gateway.providerId
            } finally {
                outputDirectory.deleteRecursively()
            }
        }
    }

    test("generates multi-page PDF without images or blank trailing page") {
        val outputDirectory = Files.createTempDirectory("nocombro-product-pdf-multipage").toFile()
        val outputFile = outputDirectory.resolve(PRODUCT_SPECIFICATION_FILE_NAME)
        try {
            ProductSpecificationPdfGenerator().generate(
                outputPath = outputFile.absolutePath,
                productName = "Многостраничный тестовый продукт",
                specification = ProductSpecification(
                    productId = 78,
                    composition = List(180) { "Подробное описание состава продукта" }.joinToString(" "),
                ),
            )

            Loader.loadPDF(outputFile).use { document ->
                (document.numberOfPages > 1) shouldBe true
                document.pages.forEachIndexed { index, _ ->
                    val pageNumber = index + 1
                    val pageText = PDFTextStripper().apply {
                        startPage = pageNumber
                        endPage = pageNumber
                    }.getText(document)
                    pageText.isNotBlank() shouldBe true
                }
            }
            assertPdfHasNoImages(outputFile)
        } finally {
            outputDirectory.deleteRecursively()
        }
    }
})

private fun assertPdfHasNoImages(outputFile: java.io.File) {
    Loader.loadPDF(outputFile).use { document ->
        var imageCount = 0
        document.pages.forEach { page ->
            page.resources.xObjectNames.forEach { name ->
                if (page.resources.getXObject(name) is PDImageXObject) {
                    imageCount++
                }
            }
        }
        imageCount shouldBe 0
    }
}

private class FailingUploadGateway : RemoteFileStorageGateway {
    var uploadCalls = 0
    override val providerId: String = "fake"
    override fun isConfigured(): Boolean = true

    override suspend fun upload(objectKey: String, localPath: String): Result<RemoteFileRef> {
        uploadCalls++
        return Result.failure(IllegalStateException("S3 must not be called"))
    }

    override suspend fun download(objectKey: String, localPath: String) = Result.success(Unit)
    override suspend fun listObjects() = Result.success(emptyList<RemoteStorageObject>())
    override suspend fun delete(objectKey: String) = Result.success(Unit)
}
