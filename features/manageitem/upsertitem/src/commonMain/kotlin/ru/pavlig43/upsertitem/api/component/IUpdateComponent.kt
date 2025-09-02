package ru.pavlig43.upsertitem.api.component

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
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope

sealed interface UpdateState<O : Any> {
    class Init<O : Any> : UpdateState<O>
    class Loading<O : Any> : UpdateState<O>
    class Success<O : Any>(val data: O) : UpdateState<O>
    class Error<O : Any>(val message: String) : UpdateState<O>
}

interface IUpdateComponent {
    val isValidValue: StateFlow<Boolean>
    val closeFormScreen:()->Unit
    fun onUpdate()
    val updateState:StateFlow<UpdateState<Unit>>
}


class UpdateComponent(
    componentContext: ComponentContext,
    private val onUpdateComponent: suspend () -> RequestResult<Unit>,
    otherValidValue: Flow<Boolean> = flowOf(true),
    override val closeFormScreen: () -> Unit,
) : ComponentContext by componentContext, IUpdateComponent {
    private val coroutineScope = componentCoroutineScope()

    override val isValidValue: StateFlow<Boolean> = otherValidValue
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            false
        )

    override  fun onUpdate() {
        _updateState.update { UpdateState.Loading() }
        coroutineScope.launch {
            val result = onUpdateComponent()
            _updateState.update { result.toUpdateState() }
        }

    }

    private val _updateState: MutableStateFlow<UpdateState<Unit>> = MutableStateFlow(UpdateState.Init<Unit>())
    override val updateState: StateFlow<UpdateState<Unit>> = _updateState.asStateFlow()



}

private fun <O : Any> RequestResult<O>.toUpdateState(): UpdateState<O> {
    return when (this) {
        is RequestResult.Error<*> -> UpdateState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> UpdateState.Loading()
        is RequestResult.Initial<*> -> UpdateState.Init()
        is RequestResult.Success<O> -> UpdateState.Success(data)
    }
}