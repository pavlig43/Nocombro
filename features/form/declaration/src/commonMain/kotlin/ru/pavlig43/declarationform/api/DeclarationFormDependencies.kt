package ru.pavlig43.declarationform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.api.dependencies

class DeclarationFormDependencies(
    val transaction: DataBaseTransaction,
    val db: NocombroDatabase,
    val dependencies: dependencies,
)