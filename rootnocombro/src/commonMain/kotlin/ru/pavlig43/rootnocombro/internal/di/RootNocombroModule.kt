package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.dao.DocumentFilesDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.product.dao.ProductFilesDao
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.itemlist.api.data.ItemListRepository
import ru.pavlig43.productform.api.IProductFormDependencies
import ru.pavlig43.signroot.api.IRootSignDependencies


private val featureDependenciesModule = module {
    factoryOf(::DocumentFormDependencies) bind IDocumentFormDependencies::class
    factory<IProductFormDependencies> { ProductFormDependencies(
        productDao = get(),
        transaction = get(),
        productFilesDao = get(),
        productDeclarationDao = get(),
        documentListRepository = get(named(ItemListType.Document.name))
    ) }
    factoryOf(::RootSignDependencies) bind IRootSignDependencies::class
    includes(itemListRepositoryModule)
}
internal val rootNocombroModule = listOf(
    featureDependenciesModule,
)


private class DocumentFormDependencies(
    override val documentDao: DocumentDao,
    override val documentFilesDao: DocumentFilesDao,
    override val transaction: DataBaseTransaction
) : IDocumentFormDependencies

private class ProductFormDependencies(
    override val productDao: ProductDao,
    override val transaction: DataBaseTransaction,
    override val productFilesDao: ProductFilesDao,
    override val productDeclarationDao: ProductDeclarationDao,
    override val documentListRepository: ItemListRepository<Document, DocumentType>,
) : IProductFormDependencies

private class RootSignDependencies : IRootSignDependencies



