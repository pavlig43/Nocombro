package ru.pavlig43.transaction.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.api.dependencies

class TransactionFormDependencies (
    val db: NocombroDatabase,
    val dbTransaction: DataBaseTransaction,
    val dependencies: dependencies
)