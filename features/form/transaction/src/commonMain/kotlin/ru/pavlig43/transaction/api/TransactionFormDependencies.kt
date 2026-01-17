package ru.pavlig43.transaction.api

import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.immutable.api.ImmutableTableDependencies

class TransactionFormDependencies (
    val db: NocombroDatabase,
    val dbTransaction: TransactionExecutor,
    val immutableTableDependencies: ImmutableTableDependencies,
    val filesDependencies: FilesDependencies
)