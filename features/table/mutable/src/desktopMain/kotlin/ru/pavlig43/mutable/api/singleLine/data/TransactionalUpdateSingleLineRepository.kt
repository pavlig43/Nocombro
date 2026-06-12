package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem

/**
 * Базовый репозиторий атомарного обновления одной сущности.
 *
 * Шаблон отделяет transport-neutral CRUD от mirror sync: изменение `updatedAt`
 * выполняется в [prepareForUpdate], а синхронизация позже обнаруживает новую версию
 * сравнением snapshots. Неизменившийся [ChangeSet] не открывает транзакцию.
 *
 * @param inWriteTransaction адаптер транзакции конкретного хранилища.
 */
abstract class TransactionalUpdateSingleLineRepository<I : SingleItem>(
    private val inWriteTransaction: suspend (block: suspend () -> Unit) -> Unit,
) : UpdateSingleLineRepository<I> {

    /**
     * Выполняет полный шаблон update-операции:
     * 1. проверяет, что данные реально изменились;
     * 2. подготавливает новую версию сущности;
     * 3. валидирует ее;
     * 4. сохраняет изменения в локальную БД в транзакции.
     *
     * Любое исключение преобразуется в [Result.failure]; transaction provider
     * должен обеспечить откат частичной записи.
     */
    final override suspend fun update(changeSet: ChangeSet<I>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)

        return runCatching {
            val itemToUpdate = prepareForUpdate(changeSet.new)
            inWriteTransaction {
                validate(itemToUpdate).getOrThrow()
                updateInDb(itemToUpdate)
            }
        }
    }

    /**
     * Подготавливает новую версию до открытия транзакции.
     *
     * Типичный override обновляет sync metadata и нормализует пользовательский ввод.
     */
    protected open fun prepareForUpdate(item: I): I = item

    /**
     * Выполняет доменную валидацию внутри транзакции перед update.
     */
    protected open suspend fun validate(item: I): Result<Unit> = Result.success(Unit)

    /**
     * Сохраняет подготовленную и проверенную сущность в локальную БД.
     */
    protected abstract suspend fun updateInDb(item: I)
}
