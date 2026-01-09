package ru.pavlig43.transaction.api

import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.immutable.api.ImmutableTableDependencies

class TransactionFormDependencies (
    val db: NocombroDatabase,
    val dbTransaction: DataBaseTransaction,
    val immutableTableDependencies: ImmutableTableDependencies,
    val filesDependencies: FilesDependencies
)