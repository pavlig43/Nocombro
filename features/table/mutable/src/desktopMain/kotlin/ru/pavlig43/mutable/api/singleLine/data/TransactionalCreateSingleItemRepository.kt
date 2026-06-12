package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.SingleItem

/**
 * Базовый репозиторий для атомарного создания одной сущности.
 *
 * Класс фиксирует единый порядок hooks: подготовка, открытие транзакции, валидация
 * и фактическая вставка. Исключения и неуспешная валидация возвращаются как
 * [Result.failure], а transaction provider отвечает за откат записи.
 *
 * @param inWriteTransaction адаптер транзакции конкретного хранилища.
 */
abstract class TransactionalCreateSingleItemRepository<I : SingleItem>(
    private val inWriteTransaction: suspend (block: suspend () -> Int) -> Int,
) : CreateSingleItemRepository<I> {

    /**
     * Выполняет полный шаблон create-операции:
     * 1. подготавливает сущность;
     * 2. валидирует ее;
     * 3. сохраняет в локальную БД в транзакции.
     *
     * @return локальный id созданной записи либо failure с причиной валидации/записи.
     */
    final override suspend fun createEssential(item: I): Result<Int> {
        return runCatching {
            val itemToCreate = prepareForCreate(item)
            inWriteTransaction {
                validate(itemToCreate).getOrThrow()
                createInDb(itemToCreate)
            }
        }
    }

    /**
     * Подготавливает сущность до открытия транзакции.
     *
     * Здесь обычно назначаются `syncId`, `updatedAt` и другие детерминированные
     * значения, которые затем проверяет [validate].
     */
    protected open fun prepareForCreate(item: I): I = item

    /**
     * Выполняет доменную валидацию внутри транзакции перед вставкой.
     */
    protected open suspend fun validate(item: I): Result<Unit> = Result.success(Unit)

    /**
     * Сохраняет сущность в локальную БД и возвращает локальный id созданной записи.
     *
     * Вызывается только после успешной [validate].
     */
    protected abstract suspend fun createInDb(item: I): Int
}
