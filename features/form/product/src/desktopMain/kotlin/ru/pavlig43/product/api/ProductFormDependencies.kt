package ru.pavlig43.product.api

import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies

class ProductFormDependencies(
    val db: NocombroDatabase,
    val transaction: TransactionExecutor,
    val immutableTableDependencies: ImmutableTableDependencies,
    val filesDependencies: FilesDependencies
)