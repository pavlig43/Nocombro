package ru.pavlig43.transaction.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.api.ItemListDependencies

class TransactionFormDependencies (
    val db: NocombroDatabase,
    val itemListDependencies: ItemListDependencies
)