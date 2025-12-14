package ru.pavlig43.declarationform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies

class DeclarationFormDependencies(
    val transaction: DataBaseTransaction,
    val db: NocombroDatabase,
    val itemStaticListDependencies: ItemStaticListDependencies,
)