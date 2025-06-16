package ru.pavlig43.document.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.document.internal.data.DocumentRepository
import ru.pavlig43.document.internal.data.CreateDocumentRepository
import ru.pavlig43.itemlist.api.data.IItemRepository
import ru.pavlig43.createitem.api.data.ICreateItemRepository

internal val documentModule = module {
    singleOf(::DocumentRepository) bind IItemRepository::class
    singleOf(::CreateDocumentRepository) bind ICreateItemRepository::class
}