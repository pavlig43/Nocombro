package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.ProductDeclarationIn

/**
 * Преобразует связь product-declaration, заменяя оба локальных FK на sync id.
 */
internal suspend fun ProductDeclarationIn.toMirrorRow(
    db: NocombroDatabase,
): ProductDeclarationMirrorRow {
    val product = db.productDao.getProduct(productId)
    val declaration = db.declarationDao.getDeclaration(declarationId)
    return ProductDeclarationMirrorRow(
        syncId = syncId,
        productSyncId = product.syncId,
        declarationSyncId = declaration.syncId,
        isProductInDeclaration = isProductInDeclaration,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/**
 * Преобразует строку состава и переносит обе product-ссылки независимо.
 *
 * [CompositionIn.syncId] остается identity самой relation row.
 */
internal suspend fun CompositionIn.toMirrorRow(
    db: NocombroDatabase,
): CompositionMirrorRow {
    val parent = db.productDao.getProduct(parentId)
    val product = db.productDao.getProduct(productId)
    return CompositionMirrorRow(
        syncId = syncId,
        parentSyncId = parent.syncId,
        productSyncId = product.syncId,
        count = count,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/**
 * Преобразует партию и заменяет ссылки на продукт и декларацию на sync identity.
 */
internal suspend fun BatchBD.toMirrorRow(
    db: NocombroDatabase,
): BatchMirrorRow {
    val product = db.productDao.getProduct(productId)
    val declaration = db.declarationDao.getDeclaration(declarationId)
    return BatchMirrorRow(
        syncId = syncId,
        productSyncId = product.syncId,
        dateBorn = dateBorn,
        declarationSyncId = declaration.syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
