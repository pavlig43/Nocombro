package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.itemlist.api.data.ItemListRepository
import ru.pavlig43.notification.api.INotificationDependencies
import ru.pavlig43.productform.api.IProductFormDependencies
import ru.pavlig43.signroot.api.IRootSignDependencies


private val featureDependenciesModule = module {
    factoryOf(::DocumentFormDependencies) bind IDocumentFormDependencies::class
    factory<IProductFormDependencies> { ProductFormDependencies(
        transaction = get(),
        db = get(),
        documentListRepository = get(named(ItemListType.Document.name))
    ) }
    factoryOf(::RootSignDependencies) bind IRootSignDependencies::class

    includes(itemListRepositoryModule)
    factoryOf(::NotificationDependencies) bind INotificationDependencies::class

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
    override val documentListRepository: ItemListRepository<Document, DocumentType>,
) : IProductFormDependencies

private class RootSignDependencies : IRootSignDependencies



