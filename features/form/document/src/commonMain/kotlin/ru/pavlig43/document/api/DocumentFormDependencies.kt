package ru.pavlig43.document.api

import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase

class DocumentFormDependencies(
    val transaction: DataBaseTransaction,
    val db: NocombroDatabase,
    val filesDependencies: FilesDependencies
)