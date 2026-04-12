package ru.pavlig43.product.internal.update.tabs.specification

import java.io.File
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.PRODUCT_SPECIFICATION_FILE_NAME
import ru.pavlig43.database.data.files.buildCanonicalFileKey
import ru.pavlig43.database.data.files.buildManagedLocalFilePath
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction
import ru.pavlig43.files.api.FilesDependencies

internal class ProductSpecificationPdfRepository(
    private val db: NocombroDatabase,
    private val syncQueueRepository: SyncQueueRepository,
    private val filesDependencies: FilesDependencies,
    private val pdfGenerator: ProductSpecificationPdfGenerator,
) {
    private val fileDao = db.fileDao
    private val remoteStorageGateway = filesDependencies.remoteFileStorageGateway

    suspend fun generateAndSave(
        productName: String,
        specification: ProductSpecification,
    ): Result<String> {
        return runCatching {
            require(productName.isNotBlank()) { "Заполни название продукта перед генерацией спецификации." }

            val existingFile = fileDao.getFileByOwnerAndDisplayName(
                ownerId = specification.productId,
                ownerFileType = OwnerType.PRODUCT,
                displayName = PRODUCT_SPECIFICATION_FILE_NAME,
            )
            val syncId = existingFile?.syncId ?: defaultSyncId()
            val canonicalFileKey = buildCanonicalFileKey(
                ownerType = OwnerType.PRODUCT,
                fileSyncId = syncId,
                originalName = PRODUCT_SPECIFICATION_FILE_NAME,
            )
            val localPath = buildManagedLocalFilePath(canonicalFileKey)

            pdfGenerator.generate(
                outputPath = localPath,
                productName = productName,
                specification = specification,
            )

            val remoteRef = if (remoteStorageGateway.isConfigured()) {
                remoteStorageGateway.upload(
                    objectKey = canonicalFileKey,
                    localPath = localPath,
                ).getOrThrow()
            } else {
                null
            }

            val updatedAt = defaultUpdatedAt()
            val file = FileBD(
                ownerId = specification.productId,
                ownerFileType = OwnerType.PRODUCT,
                displayName = PRODUCT_SPECIFICATION_FILE_NAME,
                path = localPath,
                remoteObjectKey = remoteRef?.objectKey,
                remoteStorageProvider = remoteRef?.providerId,
                id = existingFile?.id ?: 0,
                syncId = syncId,
                updatedAt = updatedAt,
                deletedAt = null,
            )

            db.inTransaction {
                fileDao.upsertFiles(listOf(file))
                syncQueueRepository.enqueueUpsert(
                    entityTable = FILE_TABLE_NAME,
                    entityLocalId = file.syncId,
                    createdAt = updatedAt,
                )
            }

            File(localPath).absolutePath
        }
    }
}
