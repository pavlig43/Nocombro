package ru.pavlig43.update.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.componentCoroutineScope

sealed interface UpdateState {
    data object Init: UpdateState
    data object Loading : UpdateState
    data object Success : UpdateState
    data class Error(val message: String) : UpdateState
}



class UpdateComponent(
    componentContext: ComponentContext,
    private val onUpdateComponent: suspend () -> Result<Unit>,
    otherValidValue: Flow<Boolean> = flowOf(true),
    val closeFormScreen: () -> Unit,
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    val isValidValue: StateFlow<Boolean> = otherValidValue
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            false
        )

     fun onUpdate() {
        _updateState.update { UpdateState.Loading }
        coroutineScope.launch {
            val result = onUpdateComponent().fold(
                onSuccess = { UpdateState.Success},
                onFailure = { UpdateState.Error(it.message ?: "Неизвестная ошибка")}
            )
            _updateState.update { result }
        }

    }

    private val _updateState: MutableStateFlow<UpdateState> = MutableStateFlow(UpdateState.Init)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()



}

