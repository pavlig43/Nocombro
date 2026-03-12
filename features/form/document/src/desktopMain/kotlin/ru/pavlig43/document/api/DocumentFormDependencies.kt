package ru.pavlig43.document.api

import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.files.api.FilesDependencies

class DocumentFormDependencies(
    val transaction: TransactionExecutor,
    val db: NocombroDatabase,
    val filesDependencies: FilesDependencies
)