package ru.pavlig43.rootnocombro.intetnal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.document.api.IDocumentDependencies



private val featureDependenciesModule = module {
    factoryOf(::DocumentDependencies) bind IDocumentDependencies::class
}
internal val rootNocombroModule = listOf(
    featureDependenciesModule,
)


private class DocumentDependencies(
    override val documentDao: DocumentDao
) : IDocumentDependencies



