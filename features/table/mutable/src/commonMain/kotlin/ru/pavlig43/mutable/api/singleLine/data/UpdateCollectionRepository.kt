package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject

interface UpdateCollectionRepository<DBOut : CollectionObject, DBIn : CollectionObject> {
    suspend fun getInit(id: Int): Result<List<DBOut>>
    suspend fun update(changeSet: ChangeSet<List<DBIn>>): Result<Unit>
}
