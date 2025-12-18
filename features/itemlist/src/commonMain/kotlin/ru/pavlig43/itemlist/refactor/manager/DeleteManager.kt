package ru.pavlig43.itemlist.refactor.manager

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope

internal class DeleteManager(
    componentContext: ComponentContext,
    private val clearSelection: () -> Unit,
    private val deleteFn: suspend (Set<Int>) -> RequestResult<Unit>
): ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Initial())
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun deleteSelected(selectedIds: Set<Int>) {
        coroutineScope.launch {
            _deleteState.update { DeleteState.Loading() }
            val result = deleteFn(selectedIds)
            _deleteState.update { result.toDeleteState() }
            if (result is RequestResult.Success<*>) {
                clearSelection()
            }
        }

    }
}
internal sealed interface DeleteState {
    class Initial : DeleteState
    class Loading : DeleteState
    class Success() : DeleteState
    class Error(val message: String) : DeleteState
}

private fun RequestResult<Unit>.toDeleteState(): DeleteState {
    return when (this) {
        is RequestResult.Error<*> -> DeleteState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> DeleteState.Loading()
        is RequestResult.Initial<*> -> DeleteState.Initial()
        is RequestResult.Success<*> -> DeleteState.Success()
    }
}