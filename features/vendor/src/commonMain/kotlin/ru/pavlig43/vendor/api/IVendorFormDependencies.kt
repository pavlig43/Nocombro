package ru.pavlig43.vendor.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.data.ItemFilter
import ru.pavlig43.itemlist.api.data.IItemListRepository

interface IVendorFormDependencies {
    val transaction: DataBaseTransaction
    val db: NocombroDatabase
}