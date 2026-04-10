package ru.pavlig43.mutable.api.multiLine.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject

/**
 * Контракт для редактирования коллекции строк в multi-line форме.
 */
interface UpdateCollectionRepository<DBOut : CollectionObject, DBIn : CollectionObject> {
    /**
     * Загружает исходное состояние коллекции для указанного родительского объекта.
     */
    suspend fun getInit(id: Int): Result<List<DBOut>>

    /**
     * Применяет изменения коллекции.
     */
    suspend fun update(changeSet: ChangeSet<List<DBIn>>): Result<Unit>
}
