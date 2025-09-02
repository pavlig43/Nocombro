package ru.pavlig43.upsertitem.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope

sealed interface CreateState<O:Any> {
    class Init<O:Any> : CreateState<O>
    class Loading<O:Any> : CreateState<O>
    class Success<O : Any>(val data: O) : CreateState<O>
    class Error<O : Any>(val message: String) : CreateState<O>
}

interface ICreateComponent<O : Any> {
    val isValidValue: StateFlow<Boolean>
    fun createItem()
    val saveState: StateFlow<CreateState<O>>
    val onSuccessAction: (O) -> Unit
}


class CreateComponent<O : Any>(
    componentContext: ComponentContext,
    private val onSaveResult: suspend () -> RequestResult<O>,
    otherValidValue: Flow<Boolean> = flowOf(true),
    override val onSuccessAction: (O) -> Unit = {},

    ) : ComponentContext by componentContext, ICreateComponent<O> {
    private val coroutineScope = componentCoroutineScope()

    private val _createState = MutableStateFlow<CreateState<O>>(CreateState.Init())
    override val saveState: StateFlow<CreateState<O>> = _createState.asStateFlow()


    override val isValidValue: StateFlow<Boolean> =
        otherValidValue.combine(_createState) { other, state ->
            other && (  state is CreateState.Init || state is CreateState.Error)
        }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            false
        )

    override fun createItem() {
        coroutineScope.launch {
            _createState.update { CreateState.Loading() }
            _createState.update { onSaveResult().toSaveStateItem() }
            if (_createState.value is CreateState.Success<O>){
                onSuccessAction((_createState.value as CreateState.Success<O>).data )
            }
        }

    }

}

private fun <O:Any> RequestResult<O>.toSaveStateItem(): CreateState<O> {
    return when (this) {
        is RequestResult.Error<*> -> CreateState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> CreateState.Loading()
        is RequestResult.Initial<*> -> CreateState.Init()
        is RequestResult.Success<O> -> CreateState.Success(data)
    }
}