package ru.pavlig43.itemlist.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.common.data.Item
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.itemlist.api.data.IItemRepository
import ru.pavlig43.itemlist.api.data.ItemUi

interface IItemListDependencies {
    val documentDao: DocumentDao

}