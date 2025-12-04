package ru.pavlig43.update.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope

sealed interface UpdateState<O : Any> {
    class Init<O : Any> : UpdateState<O>
    class Loading<O : Any> : UpdateState<O>
    class Success<O : Any>(val data: O) : UpdateState<O>
    class Error<O : Any>(val message: String) : UpdateState<O>
}



class UpdateComponent(
    componentContext: ComponentContext,
    private val onUpdateComponent: suspend () -> RequestResult<Unit>,
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
        _updateState.update { UpdateState.Loading() }
        coroutineScope.launch {
            val result = onUpdateComponent()
            _updateState.update { result.toUpdateState() }
        }

    }

    private val _updateState: MutableStateFlow<UpdateState<Unit>> = MutableStateFlow(UpdateState.Init<Unit>())
    val updateState: StateFlow<UpdateState<Unit>> = _updateState.asStateFlow()



}

private fun <O : Any> RequestResult<O>.toUpdateState(): UpdateState<O> {
    return when (this) {
        is RequestResult.Error<*> -> UpdateState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> UpdateState.Loading()
        is RequestResult.Initial<*> -> UpdateState.Init()
        is RequestResult.Success<O> -> UpdateState.Success(data)
    }
}