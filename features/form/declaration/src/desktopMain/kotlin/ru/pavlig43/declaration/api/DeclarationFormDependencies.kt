package ru.pavlig43.declaration.api

import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies


class DeclarationFormDependencies(
    val transaction: TransactionExecutor,
    val db: NocombroDatabase,
    val immutableTableDependencies: ImmutableTableDependencies,
    val filesDependencies: FilesDependencies
)