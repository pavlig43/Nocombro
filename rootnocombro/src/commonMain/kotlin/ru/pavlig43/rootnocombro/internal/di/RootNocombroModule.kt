package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.documentlist.api.IDocumentLisDependencies
import ru.pavlig43.productform.api.IProductFormDependencies
import ru.pavlig43.productlist.api.IProductLisDependencies
import ru.pavlig43.signroot.api.IRootSignDependencies


private val featureDependenciesModule = module {
    factoryOf(::DocumentListDependencies) bind IDocumentLisDependencies::class
    factoryOf(::DocumentFormDependencies) bind IDocumentFormDependencies::class
    factoryOf(::ProductFormDependencies) bind IProductFormDependencies::class
    factoryOf(::ProductListDependencies) bind IProductLisDependencies::class
    factoryOf(::RootSignDependencies) bind IRootSignDependencies::class

}
internal val rootNocombroModule = listOf(
    featureDependenciesModule,
)


private class DocumentListDependencies(
    override val documentDao: DocumentDao
) : IDocumentLisDependencies

private class DocumentFormDependencies(
    override val documentDao: DocumentDao
) : IDocumentFormDependencies

private class ProductFormDependencies(
    override val productDao: ProductDao
):IProductFormDependencies

private class ProductListDependencies(
    override val productDao: ProductDao
):IProductLisDependencies

private class RootSignDependencies : IRootSignDependencies



