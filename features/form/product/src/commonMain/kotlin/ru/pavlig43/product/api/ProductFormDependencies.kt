package ru.pavlig43.product.api

import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.immutable.api.ImmutableTableDependencies

class ProductFormDependencies(
    val db: NocombroDatabase,
    val transaction: DataBaseTransaction,
    val immutableTableDependencies: ImmutableTableDependencies,
    val filesDependencies: FilesDependencies
)