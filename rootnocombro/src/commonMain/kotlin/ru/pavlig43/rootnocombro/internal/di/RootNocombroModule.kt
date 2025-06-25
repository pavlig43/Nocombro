package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.document.api.IDocumentDependencies
import ru.pavlig43.signroot.api.IRootSignDependencies


private val featureDependenciesModule = module {
    factoryOf(::DocumentDependencies) bind IDocumentDependencies::class
    factoryOf(::RootSignDependencies) bind IRootSignDependencies::class
}
internal val rootNocombroModule = listOf(
    featureDependenciesModule,
)


private class DocumentDependencies(
    override val documentDao: DocumentDao
) : IDocumentDependencies
private class RootSignDependencies : IRootSignDependencies



