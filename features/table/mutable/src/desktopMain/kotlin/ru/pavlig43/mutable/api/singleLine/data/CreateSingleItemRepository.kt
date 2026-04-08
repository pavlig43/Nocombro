package ru.pavlig43.mutable.api.singleLine.data

import ru.pavlig43.core.model.SingleItem

/**
 * Контракт для создания одной сущности из single-line формы.
 *
 * Интерфейс остается минимальным и не знает, есть ли вокруг синхронизация,
 * транзакции или другая инфраструктурная обвязка.
 */
interface CreateSingleItemRepository<I : SingleItem> {
    /**
     * Создает сущность и возвращает локальный id созданной записи.
     */
    suspend fun createEssential(item: I): Result<Int>
}
