package ru.pavlig43.manageitem.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.manageitem.api.CreateItemType
import ru.pavlig43.manageitem.api.UpsertEssentialsDependencies
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository

internal fun moduleFactory(dependencies: UpsertEssentialsDependencies) = listOf(module {
    single<NocombroDatabase> { dependencies.db }
    single<CreateEssentialsRepository<Document>>  (named(CreateItemType.Document)){ getDocumentRepository(get())}
    single<CreateEssentialsRepository<Product>>  (named(CreateItemType.Product)){ getProductRepository(get())}
    single<CreateEssentialsRepository<Vendor>>  (named(CreateItemType.Vendor)){ getVendorRepository(get())}
    single<CreateEssentialsRepository<DeclarationIn>>  (named(CreateItemType.Declaration)){ getDeclarationRepository(get())}
}
)

private fun getDocumentRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Document> {
    val dao = db.documentDao
    return CreateEssentialsRepository(
        tag = "Create Document Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create,
    )
}
private fun getProductRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Product> {
    val dao = db.productDao
    return CreateEssentialsRepository(
        tag = "Create Product Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create,
    )
}
private fun getVendorRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Vendor> {
    val dao = db.vendorDao
    return CreateEssentialsRepository(
        tag = "Create Vendor Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create,
    )
}
private fun getDeclarationRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<DeclarationIn> {
    val dao = db.declarationDao
    return CreateEssentialsRepository(
        tag = "Create Declaration Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create,
    )
}