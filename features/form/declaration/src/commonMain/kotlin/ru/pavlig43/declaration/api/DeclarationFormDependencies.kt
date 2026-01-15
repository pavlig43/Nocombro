package ru.pavlig43.declaration.api

import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.immutable.api.ImmutableTableDependencies


class DeclarationFormDependencies(
    val transaction: DataBaseTransaction,
    val db: NocombroDatabase,
    val immutableTableDependencies: ImmutableTableDependencies,
    val filesDependencies: FilesDependencies
)