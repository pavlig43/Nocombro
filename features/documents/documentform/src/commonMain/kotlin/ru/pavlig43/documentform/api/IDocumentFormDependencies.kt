package ru.pavlig43.documentform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.dao.DocumentFilesDao

interface IDocumentFormDependencies {
    val transaction:DataBaseTransaction
    val documentDao: DocumentDao
    val documentFilesDao:DocumentFilesDao

}