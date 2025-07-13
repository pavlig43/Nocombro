package ru.pavlig43.itemlist.api

import ru.pavlig43.database.data.document.dao.DocumentDao

interface IItemListDependencies {
    val documentDao: DocumentDao

}