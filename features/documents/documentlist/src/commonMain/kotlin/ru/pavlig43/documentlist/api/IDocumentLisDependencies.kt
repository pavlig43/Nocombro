package ru.pavlig43.documentlist.api

import ru.pavlig43.database.data.document.dao.DocumentDao

interface IDocumentLisDependencies {
    val documentDao:DocumentDao
}