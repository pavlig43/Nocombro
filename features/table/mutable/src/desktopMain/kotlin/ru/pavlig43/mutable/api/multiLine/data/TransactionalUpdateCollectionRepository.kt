package ru.pavlig43.mutable.api.multiLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject

/**
 * Общий шаблон атомарного обновления изменяемой коллекции.
 *
 * Репозиторий вычисляет diff между старым и новым состоянием, валидирует итоговую
 * коллекцию и выполняет delete/upsert в одной внешней транзакции. Он не зависит от
 * конкретного remote transport: при необходимости [captureHardDeletes] оборачивает
 * запись и сохраняет tombstone на уровне database-модуля.
 *
 * Существующие элементы сопоставляются по локальному положительному `id`. Новые,
 * еще не сохраненные элементы сопоставляются по стабильному ключу
 * [entitySyncKeyOf], что предотвращает случайный delete + insert при редактировании
 * коллекции до первого сохранения.
 *
 * @param entitySyncKeyOf возвращает стабильный sync-ключ несохраненного элемента.
 * @param inWriteTransaction выполняет блок в транзакции хранилища.
 * @param captureHardDeletes опционально перехватывает физические удаления внутри
 * уже открытой транзакции.
 */
abstract class TransactionalUpdateCollectionRepository<DBOut : CollectionObject, DBIn : CollectionObject>(
    private val entitySyncKeyOf: (DBIn) -> String,
    private val inWriteTransaction: suspend (block: suspend () -> Unit) -> Unit,
    private val captureHardDeletes: suspend (block: suspend () -> Unit) -> Unit = { block -> block() },
) : UpdateCollectionRepository<DBOut, DBIn> {

    /**
     * Применяет только фактическую разницу между [ChangeSet.old] и [ChangeSet.new].
     *
     * Неизменившийся набор завершается успехом без транзакции. Подготовка upsert
     * выполняется до транзакции, а валидация и запись - внутри нее. Любое исключение
     * преобразуется в [Result.failure] и должно откатить транзакцию.
     */
    final override suspend fun update(changeSet: ChangeSet<List<DBIn>>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)

        val diff = buildDiff(changeSet)
        val preparedItemsForUpsert = diff.itemsForUpsert.map(::prepareForUpsert)

        return runCatching {
            inWriteTransaction {
                captureHardDeletes {
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
                }
            }
        }
    }

    /**
     * Подготавливает измененный или новый элемент перед upsert.
     *
     * Типичный override обновляет `updatedAt`, сбрасывает `deletedAt` или нормализует
     * поля. Метод вызывается только для элементов, попавших в diff.
     */
    protected open fun prepareForUpsert(item: DBIn): DBIn = item

    /**
     * Выполняет доменную валидацию полного конечного состояния коллекции.
     *
     * Валидация получает исходный и новый наборы и выполняется внутри транзакции
     * до первого delete/upsert.
     */
    protected open suspend fun validate(
        oldItems: List<DBIn>,
        newItems: List<DBIn>,
    ): Result<Unit> = Result.success(Unit)

    /**
     * Физически удаляет исчезнувшие существующие элементы по локальным id.
     *
     * Несохраненные элементы с `id <= 0` сюда не передаются.
     */
    protected abstract suspend fun deleteByIds(ids: List<Int>)

    /**
     * Выполняет upsert только новых или измененных элементов коллекции.
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
