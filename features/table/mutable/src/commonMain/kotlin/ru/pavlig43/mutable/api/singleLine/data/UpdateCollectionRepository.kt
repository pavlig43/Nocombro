package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.core.model.UpsertListChangeSet

class UpdateCollectionRepository<DBOut: CollectionObject,DBIn : CollectionObject>(
    private val loadCollection: suspend (parentId: Int) -> List<DBOut>,
    private val deleteCollection: suspend (ids: List<Int>) -> Unit,
    private val upsertCollection: suspend (collection: List<DBIn>) -> Unit
)  {
    suspend fun getInit(id: Int): Result<List<DBOut>> {
        return runCatching {
            loadCollection(id)
        }
    }

    suspend fun update(changeSet: ChangeSet<List<DBIn>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = deleteCollection,
            upsert = upsertCollection
        )
    }

}
