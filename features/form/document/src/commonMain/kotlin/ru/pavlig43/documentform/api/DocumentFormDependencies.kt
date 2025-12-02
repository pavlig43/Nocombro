package ru.pavlig43.documentform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase

class DocumentFormDependencies(
    val transaction: DataBaseTransaction,
    val db: NocombroDatabase,
)