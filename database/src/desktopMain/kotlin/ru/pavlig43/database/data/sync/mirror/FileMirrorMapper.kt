package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType

/**
 * Преобразует file metadata в mirror row.
 *
 * Абсолютный локальный [FileBD.path] сохраняется как metadata, но межустановочная
 * связь с владельцем строится исключительно через [FileMirrorRow.ownerSyncId].
 * Бинарное содержимое не входит в row и остается в S3.
 */
internal suspend fun FileBD.toMirrorRow(
    db: NocombroDatabase,
): FileMirrorRow {
    return FileMirrorRow(
        syncId = syncId,
        ownerType = ownerFileType,
        ownerSyncId = db.loadOwnerSyncId(ownerId, ownerFileType),
        displayName = displayName,
        path = path,
        remoteObjectKey = remoteObjectKey,
        remoteStorageProvider = remoteStorageProvider,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

private suspend fun NocombroDatabase.loadOwnerSyncId(
    ownerId: Int,
    ownerType: OwnerType,
): String {
    return when (ownerType) {
        OwnerType.DECLARATION -> declarationDao.getDeclaration(ownerId).syncId
        OwnerType.PRODUCT -> productDao.getProduct(ownerId).syncId
        OwnerType.VENDOR -> vendorDao.getVendor(ownerId).syncId
        OwnerType.DOCUMENT -> documentDao.getDocument(ownerId).syncId
        OwnerType.TRANSACTION -> transactionDao.getTransaction(ownerId).syncId
        OwnerType.EXPENSE -> expenseDao.getExpense(ownerId)?.syncId
            ?: error("Missing expense owner for id=$ownerId")
        OwnerType.EXPERIMENT_ENTRY -> experimentEntryDao.getEntry(ownerId)?.syncId
            ?: error("Missing experiment entry owner for id=$ownerId")
    }
}
