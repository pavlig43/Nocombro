package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pavlig43.declarationform.api.DeclarationFormDependencies
import ru.pavlig43.document.api.DocumentFormDependencies
import ru.pavlig43.itemlist.api.dependencies
import ru.pavlig43.notification.api.NotificationDependencies
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.signroot.api.IRootSignDependencies
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.vendor.api.VendorFormDependencies


internal val featureDependenciesModule = listOf(module {
    singleOf(::dependencies)
    factoryOf(::IRootSignDependencies)
    factoryOf(::NotificationDependencies)

    factoryOf(::ProductFormDependencies)
    factoryOf(::DocumentFormDependencies)
    factoryOf(::VendorFormDependencies)
    factoryOf(::DeclarationFormDependencies)
    factoryOf(::TransactionFormDependencies)

})
















