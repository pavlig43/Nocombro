package ru.pavlig43.productform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase

class ProductFormDependencies(
    val db: NocombroDatabase,
    val transaction: DataBaseTransaction
    )