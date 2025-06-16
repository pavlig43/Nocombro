package ru.pavlig43.document.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.dao.DocumentDao

interface IDocumentDependencies {
    val documentDao: DocumentDao
}