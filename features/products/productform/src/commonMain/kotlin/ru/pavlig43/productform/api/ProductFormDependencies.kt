package ru.pavlig43.productform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.manageitem.api.UpsertEssentialsDependencies

class ProductFormDependencies(
    val db: NocombroDatabase,
    val transaction: DataBaseTransaction,
    val itemListDependencies: ItemListDependencies,
    val upsertEssentialsDependencies: UpsertEssentialsDependencies
    )