package ru.pavlig43.product.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies

class ProductFormDependencies(
    val db: NocombroDatabase,
    val transaction: DataBaseTransaction,
    val itemStaticListDependencies: ItemStaticListDependencies
)