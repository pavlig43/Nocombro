package ru.pavlig43.addfile.api

import ru.pavlig43.database.data.document.dao.DocumentDao

interface IAddFileDependencies {
    val documentDao:DocumentDao
}