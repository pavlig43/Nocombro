package ru.pavlig43.immutable.internal.data

import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с неизменяемым списком элементов.
 *
 * Предоставляет методы для удаления элементов и реактивного отслеживания изменений.
 *
 * @param I Тип элемента списка
 */
internal interface ImmutableListRepository<I> {
    /**
     * Удаляет элементы с указанными идентификаторами.
     *
     * @param ids Множество идентификаторов для удаления
     * @return Result успешности операции
     */
    suspend fun deleteByIds(ids: Set<Int>): Result<Unit>

    /**
     * Создаёт Flow для отслеживания изменений элементов.
     *
     * Flow автоматически отправляет новые данные при изменении списка.
     *
     * @return Flow, который emitting Result со списком элементов или ошибкой
     */
    fun observeOnItems(): Flow<Result<List<I>>>
}