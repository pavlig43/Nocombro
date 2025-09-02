package ru.pavlig43.upsertitem.api.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.CollectionObject
import ru.pavlig43.core.data.dbSafeCall

class UpdateCollectionRepository<Out: CollectionObject,In : CollectionObject>(
    private val tag: String,
    private val loadCollection: suspend (parentId: Int) -> List<Out>,
    private val deleteCollection: suspend (ids: List<Int>) -> Unit,
    private val upsertCollection: suspend (collection: List<In>) -> Unit
)  {
    suspend fun getInit(id: Int): RequestResult<List<Out>> {
        return dbSafeCall(tag) {
            loadCollection(id)
        }
    }

    suspend fun update(changeSet: ChangeSet<List<In>>) {
        val (old, new) = changeSet
        val newById = new.associateBy { it.id }
        if (old != null) {
            val oldById = old.associateBy { it.id }
            val idsForDelete = oldById.keys - newById.keys
            deleteCollection(idsForDelete.toList())
            val collectionForUpsert = new.filter { newItem ->
                val oldItem = oldById[newItem.id]
                oldItem == null || oldItem != newItem
            }
            upsertCollection(collectionForUpsert)
        } else {
            upsertCollection(new)
        }


    }

}

