package ru.pavlig43.documentform.api

import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase

interface IDocumentFormDependencies {
    val transaction:DataBaseTransaction
    val db:NocombroDatabase

}