package ru.pavlig43.update.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.*
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

