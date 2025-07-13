package ru.pavlig43.documentform.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.createitem.api.data.IItemFormRepository
import ru.pavlig43.documentform.internal.data.CreateDocumentRepository


internal val documentFormModule = module {
    singleOf(::CreateDocumentRepository) bind IItemFormRepository::class
}