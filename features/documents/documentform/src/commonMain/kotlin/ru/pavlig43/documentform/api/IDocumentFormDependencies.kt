package ru.pavlig43.documentform.api

import ru.pavlig43.database.data.document.dao.DocumentDao

interface IDocumentFormDependencies {
    val documentDao: DocumentDao
}