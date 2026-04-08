package ru.pavlig43.mutable.api.multiLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject

/**
 * Базовый репозиторий для обновления коллекции с автоматической записью diff в очередь синхронизации.
 *
 * Подходит для сценариев, где одна коллекция напрямую сохраняется в одну таблицу.
 */
abstract class SyncUpdateCollectionRepository<DBOut : CollectionObject, DBIn : CollectionObject>(
    private val tableName: String,
    private val entitySyncKeyOf: (DBIn) -> String,
    private val enqueueSyncUpsert: suspend (tableName: String, entityKey: String) -> Unit,
    private val enqueueSyncDelete: suspend (tableName: String, entityKey: String) -> Unit,
    private val inWriteTransaction: suspend (block: suspend () -> Unit) -> Unit,
) : UpdateCollectionRepository<DBOut, DBIn> {

    final override suspend fun update(changeSet: ChangeSet<List<DBIn>>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)

        val diff = buildDiff(changeSet)
        val preparedItemsForUpsert = diff.itemsForUpsert.map(::prepareForUpsert)

        return runCatching {
            inWriteTransaction {
                validate(
                    oldItems = changeSet.old.orEmpty(),
                    newItems = changeSet.new,
                ).getOrThrow()

                if (diff.idsForDelete.isNotEmpty()) {
                    deleteByIds(diff.idsForDelete)
                }
                if (preparedItemsForUpsert.isNotEmpty()) {
                    upsertItems(preparedItemsForUpsert)
                }

                diff.itemsForDelete.forEach { item ->
                    enqueueSyncDelete(tableName, entitySyncKeyOf(item))
                }
                preparedItemsForUpsert.forEach { item ->
                    enqueueSyncUpsert(tableName, entitySyncKeyOf(item))
                }
            }
        }
    }

    /**
     * Позволяет подготовить элемент перед upsert, например обновить `updatedAt`.
     */
    protected open fun prepareForUpsert(item: DBIn): DBIn = item

    /**
     * Выполняет доменную валидацию коллекции перед сохранением.
     */
    protected open suspend fun validate(
        oldItems: List<DBIn>,
        newItems: List<DBIn>,
    ): Result<Unit> = Result.success(Unit)

    /**
     * Удаляет элементы по их локальным id.
     */
    protected abstract suspend fun deleteByIds(ids: List<Int>)

    /**
     * Выполняет upsert измененных элементов коллекции.
     */
    protected abstract suspend fun upsertItems(items: List<DBIn>)

    private fun buildDiff(changeSet: ChangeSet<List<DBIn>>): CollectionSyncDiff<DBIn> {
        val oldItems = changeSet.old.orEmpty()
        val newItems = changeSet.new

        val oldItemsByKey = oldItems.associateBy(::diffKeyOf)
        val newItemsByKey = newItems.associateBy(::diffKeyOf)

        val itemsForDelete = oldItemsByKey
            .filterKeys { key -> key !in newItemsByKey }
            .values
            .toList()

        val itemsForUpsert = newItems.filter { newItem ->
            val oldItem = oldItemsByKey[diffKeyOf(newItem)]
            oldItem == null || oldItem != newItem
        }

        return CollectionSyncDiff(
            idsForDelete = itemsForDelete.map { it.id }.filter { it > 0 },
            itemsForDelete = itemsForDelete,
            itemsForUpsert = itemsForUpsert,
        )
    }

    private fun diffKeyOf(item: DBIn): String {
        return if (item.id > 0) {
            "id:${item.id}"
        } else {
            "sync:${entitySyncKeyOf(item)}"
        }
    }
}

private data class CollectionSyncDiff<I>(
    val idsForDelete: List<Int>,
    val itemsForDelete: List<I>,
    val itemsForUpsert: List<I>,
)
