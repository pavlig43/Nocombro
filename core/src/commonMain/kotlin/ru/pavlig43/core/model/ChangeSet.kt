package ru.pavlig43.core.model

/**
 * Класс используется при записи в бд, чтобы перезаписывать только измененные данные
 */
data class ChangeSet<I : Any>(
    val old: I?,
    val new: I
)
object UpsertListChangeSet{
    suspend  fun<I: CollectionObject> update(
        changeSet: ChangeSet<List<I>>,
        delete:suspend (List<Int>)-> Unit,
        upsert:suspend (List<I>)-> Unit
        ): Result<Unit> {
        return runCatching {
            val (old, new) = changeSet
            val newById = new.associateBy { it.id }
            if (old != null) {
                val oldById = old.associateBy { it.id }
                val idsForDelete = oldById.keys - newById.keys
                delete(idsForDelete.toList())
                val collectionForUpsert = new.filter { newItem ->
                    val oldItem = oldById[newItem.id]
                    oldItem == null || oldItem != newItem
                }
                upsert(collectionForUpsert)
            } else {
                upsert(new)
            }
        }

    }
}
