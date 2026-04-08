package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.SingleItem

/**
 * Базовый репозиторий для create-операций с автоматической постановкой записи в очередь синхронизации.
 */
abstract class SyncCreateSingleItemRepository<I : SingleItem>(
    private val tableName: String,
    private val enqueueUpsert: suspend (tableName: String, entityLocalId: String) -> Unit,
    private val runInTransaction: suspend (block: suspend () -> Int) -> Int,
) : CreateSingleItemRepository<I> {

    final override suspend fun createEssential(item: I): Result<Int> {
        return runCatching {
            val itemToCreate = prepareForCreate(item)
            runInTransaction {
                validate(itemToCreate).getOrThrow()
                val entityId = createInDb(itemToCreate)
                enqueueUpsert(tableName, entityId.toString())
                entityId
            }
        }
    }

    /**
     * Позволяет подготовить сущность перед сохранением.
     */
    protected open fun prepareForCreate(item: I): I = item

    /**
     * Выполняет доменную валидацию перед сохранением.
     */
    protected open suspend fun validate(item: I): Result<Unit> = Result.success(Unit)

    /**
     * Сохраняет сущность в локальную БД и возвращает ее локальный id.
     */
    protected abstract suspend fun createInDb(item: I): Int
}
