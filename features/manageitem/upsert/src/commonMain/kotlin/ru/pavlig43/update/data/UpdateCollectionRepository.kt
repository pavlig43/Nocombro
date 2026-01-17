package ru.pavlig43.update.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject

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
        return runCatching {
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

}
class UpdateCollectionRepository1<Key,DBOut: CollectionObject,DBIn : CollectionObject>(
    private val loadCollection: suspend (key:Key) -> List<DBOut>,
    private val deleteCollection: suspend (ids: List<Int>) -> Unit,
    private val upsertCollection: suspend (collection: List<DBIn>) -> Unit
)  {
    suspend fun getInit(key: Key): Result<List<DBOut>> {
        return runCatching {
            loadCollection(key)
        }
    }

    suspend fun update(changeSet: ChangeSet<List<DBIn>>): Result<Unit> {
        return runCatching {
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

}