package ru.pavlig43.itemlist.api.component.refactoring

import ru.pavlig43.database.NocombroDatabase

interface IItemListDependencies {
    val db: NocombroDatabase
}