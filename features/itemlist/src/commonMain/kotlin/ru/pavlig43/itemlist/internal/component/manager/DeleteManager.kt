package ru.pavlig43.itemlist.internal.component.manager

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope

internal class DeleteManager(
    componentContext: ComponentContext,
    private val clearSelection: () -> Unit,
    private val deleteFn: suspend (Set<Int>) -> Result<Unit>
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Initial)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun deleteSelected(selectedIds: Set<Int>) {
        coroutineScope.launch {
            _deleteState.update { DeleteState.Loading }
            val state = deleteFn(selectedIds).fold(
                onSuccess = {
                    clearSelection()
                    DeleteState.Success
                },
                onFailure = { DeleteState.Error(it.message ?: "unknown error") }
            )
            _deleteState.update { state }

        }

    }
}

internal sealed interface DeleteState {
    data object Initial : DeleteState
    data object Loading : DeleteState
    data object Success : DeleteState
    data class Error(val message: String) : DeleteState
}

