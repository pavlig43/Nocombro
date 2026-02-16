package ru.pavlig43.flowImmutable.api.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.model.ChangeSet

/**
 * Репозиторий для работы с многострочными данными в таблицах.
 *
 * Предоставляет методы для загрузки начальных данных, реактивного отслеживания изменений
 * и сохранения обновлений.
 *
 * @param BdOut Тип сущности для отображения (выходные данные)
 * @param BdIn Тип сущности для сохранения (входные данные)
 */
interface FlowMultilineRepository<BdOut, BdIn> {

    /**
     * Загружает начальный список входных сущностей для указанного родителя.
     *
     * @param parentId Идентификатор родительской сущности
     * @return Result со списком входных сущностей или ошибкой
     */
    suspend fun getInit(parentId: Int): Result<List<BdIn>>

    /**
     * Создаёт Flow для реактивного отслеживания изменений сущностей.
     *
     * Flow автоматически отправляет новые данные при изменении сущностей
     * с указанными идентификаторами в базе данных.
     *
     * @param ids Список идентификаторов сущностей для отслеживания
     * @return Flow, который emitting Result со списком выходных сущностей или ошибкой
     */
    fun observeOnItemsByIds(ids: List<Int>): Flow<Result<List<BdOut>>>

    /**
     * Сохраняет изменения в репозитории.
     *
     * @param changeSet Набор изменений, содержащий старые и новые данные
     * @return Result успешности операции
     */
    suspend fun update(changeSet: ChangeSet<List<BdIn>>): Result<Unit>

}