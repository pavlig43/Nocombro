package ru.pavlig43.documentform.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.documentform.internal.data.InitBaseValuesDocumentRepository
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.documentform.internal.data.SaveDocumentRepository
import ru.pavlig43.documentform.internal.data.ISaveDocumentRepository


internal val createDocumentFormModule = module {
    singleOf(::InitBaseValuesDocumentRepository) bind IInitDataRepository::class
    singleOf(::SaveDocumentRepository) bind ISaveDocumentRepository::class
}
//internal val changeDocumentModule = module {
//    singleOf(::InitBaseValuesDocumentRepository) bind IInitDataRepository::class
//    singleOf(::ChangeDocumentRepository) bind IItemFormRepository::class
//}