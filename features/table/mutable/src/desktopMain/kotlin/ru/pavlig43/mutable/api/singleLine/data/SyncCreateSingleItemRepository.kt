package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.SingleItem

/**
 * Базовый репозиторий для create-операций с автоматической постановкой записи в очередь синхронизации.
 *
 * Наследник отвечает только за подготовку, валидацию и локальное сохранение сущности.
 * Вся sync-обвязка выполняется в [createEssential] и не должна дублироваться в конкретных формах.
 */
abstract class SyncCreateSingleItemRepository<I : SingleItem>(
    private val tableName: String,
    private val enqueueSyncUpsert: suspend (tableName: String, entityLocalId: String) -> Unit,
    private val inWriteTransaction: suspend (block: suspend () -> Int) -> Int,
) : CreateSingleItemRepository<I> {

    /**
     * Выполняет полный шаблон create-операции:
     * 1. подготавливает сущность;
     * 2. валидирует ее;
     * 3. сохраняет в локальную БД в транзакции;
     * 4. добавляет запись в очередь синхронизации.
     */
    final override suspend fun createEssential(item: I): Result<Int> {
        return runCatching {
            val itemToCreate = prepareForCreate(item)
            inWriteTransaction {
                validate(itemToCreate).getOrThrow()
                val entityId = createInDb(itemToCreate)
                enqueueSyncUpsert(tableName, entityId.toString())
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
     * Сохраняет сущность в локальную БД и возвращает локальный id созданной записи.
     */
    protected abstract suspend fun createInDb(item: I): Int
}
