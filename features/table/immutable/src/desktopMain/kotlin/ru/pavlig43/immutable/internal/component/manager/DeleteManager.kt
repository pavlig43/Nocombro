package ru.pavlig43.immutable.internal.component.manager

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope

/**
 * Координирует удаление выбранных строк таблицы.
 *
 * Пока идёт запрос, повторный вызов игнорируется. Выделение очищается лишь после
 * успеха; при ошибке оно сохраняется, чтобы пользователь мог повторить действие.
 */
internal class DeleteManager(
    componentContext: ComponentContext,
    private val clearSelection: () -> Unit,
    private val deleteFn: suspend (Set<Int>) -> Result<Unit>
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Initial)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    /**
     * Удаляет переданные строки и публикует итог в [deleteState].
     *
     * Синхронное исключение [deleteFn] преобразуется в [DeleteState.Error] так же,
     * как `Result.failure`, поэтому состояние не зависает в Loading.
     */
    fun deleteSelected(selectedIds: Set<Int>) {
        if (_deleteState.value == DeleteState.Loading) return
        _deleteState.value = DeleteState.Loading
        coroutineScope.launch {
            val result = runCatching {
                deleteFn(selectedIds)
            }.getOrElse { error -> Result.failure(error) }
            val state = result.fold(
                onSuccess = {
                    clearSelection()
                    DeleteState.Success
                },
                onFailure = { DeleteState.Error(it.message ?: "Не удалось удалить строки") }
            )
            _deleteState.update { state }

        }
    }
}

/** Состояние одного действия удаления в неизменяемой таблице. */
sealed interface DeleteState {
    /** Удаление ещё не запускалось. */
    data object Initial : DeleteState
    /** Запрос выполняется; повторный клик запрещён. */
    data object Loading : DeleteState
    /** Строки удалены, выделение очищено. */
    data object Success : DeleteState
    /** Удаление не удалось, выделение сохранено для повтора. */
    data class Error(val message: String) : DeleteState
}

