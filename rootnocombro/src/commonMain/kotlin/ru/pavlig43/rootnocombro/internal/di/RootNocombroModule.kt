package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pavlig43.declarationform.api.DeclarationDependencies
import ru.pavlig43.documentform.api.DocumentFormDependencies
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.manageitem.api.UpsertEssentialsDependencies
import ru.pavlig43.notification.api.NotificationDependencies
import ru.pavlig43.productform.api.ProductFormDependencies
import ru.pavlig43.signroot.api.IRootSignDependencies
import ru.pavlig43.vendor.api.VendorFormDependencies


internal val featureDependenciesModule = listOf(module {
    singleOf(::ItemListDependencies)
    singleOf(::UpsertEssentialsDependencies)
    factoryOf(::ProductFormDependencies)
    factoryOf(::DocumentFormDependencies)
    factoryOf(::VendorFormDependencies)
    factoryOf(::DeclarationDependencies)
    factoryOf(::IRootSignDependencies)
    factoryOf(::NotificationDependencies)

})
















