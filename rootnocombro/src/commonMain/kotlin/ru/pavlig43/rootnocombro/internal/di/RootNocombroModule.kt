package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.declarationform.api.IDeclarationDependencies
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.itemlist.api.component.refactoring.ItemListDependencies
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType
import ru.pavlig43.itemlist.api.data.VendorListRepository
import ru.pavlig43.notification.api.INotificationDependencies
import ru.pavlig43.productform.api.IProductFormDependencies
import ru.pavlig43.signroot.api.IRootSignDependencies
import ru.pavlig43.vendor.api.IVendorFormDependencies


private val featureDependenciesModule = module {
    singleOf(::ItemListDependencies)
    factoryOf(::DocumentFormDependencies) bind IDocumentFormDependencies::class
    factory<IProductFormDependencies> { ProductFormDependencies(
        transaction = get(),
        db = get(),
        documentListRepository = get(named(ItemListType.Document.name)),
        productListRepository = get(named(ItemListType.Product.name))
    ) }
    factoryOf(::RootSignDependencies) bind IRootSignDependencies::class
    factoryOf(::VendorFormDependencies) bind IVendorFormDependencies::class

    includes(IItemListRepositoryModule)
    factoryOf(::NotificationDependencies) bind INotificationDependencies::class
    factory<IDeclarationDependencies> { DeclarationFormDependencies(
        transaction = get(),
        db = get(),
        itemListDependencies = get()
    ) }

}
internal val rootNocombroModule = listOf(
    featureDependenciesModule,
)


private class NotificationDependencies(
    override val db: NocombroDatabase
):INotificationDependencies

private class DocumentFormDependencies(
    override val transaction: DataBaseTransaction,
    override val db: NocombroDatabase
) : IDocumentFormDependencies

private class ProductFormDependencies(
    override val db: NocombroDatabase,
    override val transaction: DataBaseTransaction,
    override val documentListRepository: IItemListRepository<Document, DocumentType>,
    override val productListRepository: IItemListRepository<Product, ProductType>,
) : IProductFormDependencies
private class VendorFormDependencies(
    override val transaction: DataBaseTransaction,
    override val db: NocombroDatabase
): IVendorFormDependencies

private class DeclarationFormDependencies(
    override val transaction: DataBaseTransaction,
    override val db: NocombroDatabase,
    override val itemListDependencies: ItemListDependencies
):IDeclarationDependencies




private class RootSignDependencies : IRootSignDependencies



