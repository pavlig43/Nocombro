package ru.pavlig43.documentlist.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.documentlist.internal.data.DocumentListRepository
import ru.pavlig43.itemlist.api.data.IItemListRepository

internal val documentListModule = module {
    singleOf(::DocumentListRepository) bind IItemListRepository::class

}