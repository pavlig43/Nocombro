package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem

/**
 * Базовый репозиторий для update-операций с автоматическим обновлением очереди синхронизации.
 */
abstract class SyncUpdateSingleLineRepository<I : SingleItem>(
    private val tableName: String,
    private val enqueueUpsert: suspend (tableName: String, entityLocalId: String) -> Unit,
    private val runInTransaction: suspend (block: suspend () -> Unit) -> Unit,
) : UpdateSingleLineRepository<I> {

    final override suspend fun update(changeSet: ChangeSet<I>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)

        return runCatching {
            val itemToUpdate = prepareForUpdate(changeSet.new)
            runInTransaction {
                validate(itemToUpdate).getOrThrow()
                updateInDb(itemToUpdate)
                enqueueUpsert(tableName, itemToUpdate.id.toString())
            }
        }
    }

    /**
     * Позволяет подготовить сущность перед сохранением.
     */
    protected open fun prepareForUpdate(item: I): I = item

    /**
     * Выполняет доменную валидацию перед сохранением.
     */
    protected open suspend fun validate(item: I): Result<Unit> = Result.success(Unit)

    /**
     * Сохраняет изменения сущности в локальную БД.
     */
    protected abstract suspend fun updateInDb(item: I)
}
