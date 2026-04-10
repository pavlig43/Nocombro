package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem

/**
 * Базовый репозиторий для update-операций с автоматическим обновлением очереди синхронизации.
 *
 * Наследник отвечает только за подготовку, валидацию и локальное сохранение изменений.
 * Вся sync-обвязка выполняется в [update] и не должна дублироваться в конкретных формах.
 */
abstract class SyncUpdateSingleLineRepository<I : SingleItem>(
    private val tableName: String,
    private val entitySyncKeyOf: (I) -> String,
    private val enqueueSyncUpsert: suspend (tableName: String, entityLocalId: String) -> Unit,
    private val inWriteTransaction: suspend (block: suspend () -> Unit) -> Unit,
) : UpdateSingleLineRepository<I> {

    /**
     * Выполняет полный шаблон update-операции:
     * 1. проверяет, что данные реально изменились;
     * 2. подготавливает новую версию сущности;
     * 3. валидирует ее;
     * 4. сохраняет изменения в локальную БД в транзакции;
     * 5. обновляет очередь синхронизации.
     */
    final override suspend fun update(changeSet: ChangeSet<I>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)

        return runCatching {
            val itemToUpdate = prepareForUpdate(changeSet.new)
            inWriteTransaction {
                validate(itemToUpdate).getOrThrow()
                updateInDb(itemToUpdate)
                enqueueSyncUpsert(tableName, entitySyncKeyOf(itemToUpdate))
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
