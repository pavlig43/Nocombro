package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.SingleItem

/**
 * Контракт для редактирования одной сущности из single-line формы.
 */
interface UpdateSingleLineRepository<I : SingleItem> {
    /**
     * Загружает исходное состояние сущности для формы редактирования.
     */
    suspend fun getInit(id: Int): Result<I>

    /**
     * Применяет изменения одной сущности.
     */
    suspend fun update(changeSet: ChangeSet<I>): Result<Unit>
}
