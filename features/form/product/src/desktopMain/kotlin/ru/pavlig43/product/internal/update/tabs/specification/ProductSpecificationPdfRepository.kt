package ru.pavlig43.product.internal.update.tabs.specification

import java.io.File
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.FileDao
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.PRODUCT_SPECIFICATION_FILE_NAME
import ru.pavlig43.database.data.files.buildCanonicalFileKey
import ru.pavlig43.database.data.files.buildManagedLocalFilePath
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

/**
 * Генерирует и сохраняет единственный системный PDF-файл спецификации продукта.
 *
 * Репозиторий собирает локальный PDF, при необходимости загружает его в remote
 * storage и обновляет запись в таблице `file` без создания дублей.
 */
internal class ProductSpecificationPdfRepository(
    private val fileDao: FileDao,
    private val remoteFileStorageGateway: RemoteFileStorageGateway,
    private val syncQueueRepository: SyncQueueRepository,
    private val pdfGenerator: ProductSpecificationPdfGenerator,
) {
    /**
     * Сохраняет `Спецификация.pdf` для продукта и возвращает локальный путь.
     *
     * Если файл уже существует, переиспользует его запись и `syncId`, чтобы
     * обновление было идемпотентным и корректно синхронизировалось.
     */
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

            val remoteRef = if (remoteFileStorageGateway.isConfigured()) {
                remoteFileStorageGateway.upload(
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

            fileDao.upsertFiles(listOf(file))
            syncQueueRepository.enqueueUpsert(
                entityTable = FILE_TABLE_NAME,
                entityLocalId = file.syncId,
                createdAt = updatedAt,
            )

            File(localPath).absolutePath
        }
    }
}
