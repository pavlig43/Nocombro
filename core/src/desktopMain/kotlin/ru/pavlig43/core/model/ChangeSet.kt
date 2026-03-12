package ru.pavlig43.core.model

/**
 * Класс используется при записи в бд, чтобы перезаписывать только измененные данные
 */
data class ChangeSet<I : Any>(
    val old: I?,
    val new: I
)


object UpsertListChangeSet{
    /**
     * Утилита для обработки изменений в коллекциях с операциями удаления и upsert (вставка/обновление).
     *
     * Вычисляет разницу между старой и новой версией списка и выполняет соответствующие операции в репозитории.
     * Оптимизирует работу с БД/API, отправляя только изменённые данные.
     *
     * ## Алгоритм работы:
     * 1. **Вычисление разницы** между старой и новой коллекцией
     * 2. **Определение элементов для удаления** (есть в старой, нет в новой)
     * 3. **Определение элементов для upsert** (новые или изменённые)
     * 4. **Пакетное выполнение операций** в переданных suspend-функциях
     *
     * @param I Тип элементов коллекции, должен реализовывать [CollectionObject]
     * @param changeSet Пара (старый список, новый список) для сравнения.
     *                  Старый список может быть `null` при первоначальной вставке
     * @param delete Suspend-функция для удаления элементов по их ID
     * @param upsert Suspend-функция для вставки или обновления элементов
     * @return [Result]<Unit> - успех или ошибка выполнения операций
     *
     * ## Пример использования:
     * ```kotlin
     * // Синхронизация списка товаров
     * UpsertListChangeSet.update(
     *     changeSet = ChangeSet(oldProducts, newProducts),
     *     delete = { ids -> repository.deleteProducts(ids) },
     *     upsert = { products -> repository.upsertProducts(products) }
     * )
     * ```
     *
     * ## Особенности:
     * - Использует сравнение по `id` и `equals()` для определения изменений
     * - Выполняет операции в порядке: сначала удаление, затем upsert
     * - Работает с `null` старым списком (первоначальное заполнение)
     * - Гарантирует что каждый элемент будет обработан только один раз
     *
     * @see CollectionObject Базовый интерфейс для объектов коллекции с идентификатором
     * @see ChangeSet Контейнер для хранения пар значений (старое, новое)
     */
    suspend  fun<I: CollectionObject> update(
        changeSet: ChangeSet<List<I>>,
        delete:suspend (List<Int>)-> Unit,
        upsert:suspend (List<I>)-> Unit
        ): Result<Unit> {
        return runCatching {
            val (old, new) = changeSet
            val newById = new.associateBy { it.id }
            if (old != null) {
                val oldById = old.associateBy { it.id }
                val idsForDelete = oldById.keys - newById.keys
                delete(idsForDelete.toList())
                val collectionForUpsert = new.filter { newItem ->
                    val oldItem = oldById[newItem.id]
                    oldItem == null || oldItem != newItem
                }
                upsert(collectionForUpsert)
            } else {
                upsert(new)
            }
        }

    }
}
