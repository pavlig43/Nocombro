package ru.pavlig43.update.component

/** Состояние двухфазного сохранения формы. */
sealed interface UpdateState {
    /** Сохранение ещё не запускалось или состояние было сброшено. */
    data object Init: UpdateState
    /** Идёт сохранение либо повтор постобработки. */
    data object Loading : UpdateState
    /** Данные сохранены и постобработка завершена. */
    data object Success : UpdateState
    /** Данные вкладок не были сохранены. */
    data class Error(val message: String) : UpdateState
    /** Данные сохранены, но постобработку нужно повторить отдельно. */
    data class PostProcessError(val message: String) : UpdateState
}
