package ru.pavlig43.core

/**
 * Выполняет транзакцию переданных в него функций в(все или ничего)
 */
public interface TransactionExecutor {
    /**
     * Последовательно выполняет все блоки, возвращает первую ошибку или успех
     *
     * @param blocks Список suspend блоков для выполнения
     */
    suspend fun transaction(blocks: List<suspend () -> Result<Unit>>): Result<Unit>
}